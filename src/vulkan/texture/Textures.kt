package vulkan.texture

import org.apache.log4j.Logger
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkBufferImageCopy
import vulkan.api.*
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.VkBuffer
import vulkan.api.image.VkImage
import vulkan.api.image.setImageLayout
import vulkan.api.memory.VkDeviceMemory
import vulkan.api.memory.allocImage
import vulkan.api.memory.allocStagingSrcBuffer
import vulkan.api.memory.allocateMemory
import vulkan.app.VulkanApplication
import vulkan.common.KILOBYTE
import vulkan.common.MEGABYTE
import vulkan.common.Queues
import vulkan.misc.VkFormat
import vulkan.misc.megabytes
import vulkan.misc.orThrow
import vulkan.misc.translateVkFormat
import kotlin.collections.set

class Textures(val name:String = "") {
    private val logger: Logger = Logger.getLogger("Textures")

    private lateinit var vk: VulkanApplication

    private lateinit var commandPool   : VkCommandPool
    private lateinit var deviceMemory  : VkDeviceMemory
    private lateinit var stagingMemory : VkDeviceMemory
    private lateinit var stagingBuffer : VkBuffer

    private val loader = ImageLoader
    private val map    = HashMap<String, Texture>()

    fun init(vk: VulkanApplication, sizeBytes:Int) : Textures {
        this.vk          = vk
        this.commandPool = vk.device.createCommandPool(vk.queues.getFamily(Queues.TRANSFER).index,
                                                       VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
        allocateMemory(sizeBytes)
        return this
    }
    fun destroy() {
        map.entries.forEach { e->
            log("Destroying image ${e.key}")
            e.value.image.destroy()
        }
        stagingBuffer.destroy()
        stagingMemory.free()
        deviceMemory.free()
        commandPool.destroy()
    }

    fun get(name:String, format:VkFormat = 0) : Texture {
        log("get \"$name\" ${if(format==0) "" else format.translateVkFormat()}")
        return map.getOrPut(name) {
            generateTexture(name, loader.load(name, format))
        }
    }
    fun create(name:String, data:RawImageData) : Texture {
        val t = generateTexture(name, data)
        map[name] = t
        return t
    }
    fun exists(name:String) : Boolean {
        return map.containsKey(name) || loader.exists(name)
    }
    //=========================================================================================
    private fun generateTexture(name:String, data:RawImageData) : Texture {
        log("Creating texture \"$name\"")

        val image = uploadImageToDevice(data)
        logUsage()
        data.free()

        return Texture(
            name     = name,
            width    = data.width,
            height   = data.height,
            channels = data.channels,
            levels   = data.levels,
            image    = image)
    }
    private fun uploadImageToDevice(data: RawImageData): VkImage {

        log("Uploading image $data")

        /** Allocate space in staging buffer */
        stagingBuffer.allocate(data.size).orThrow().let { staging ->

            log("Allocated ${data.size/KILOBYTE} KB of staging buffer")

            /** Write the data to the staging buffer */
            staging.mapForWriting { bb->
                bb.put(data.data)
            }

            val image = deviceMemory.allocImage { info->
                info.imageType(VK_IMAGE_TYPE_2D)
                info.format(data.format)
                info.mipLevels(1)
                info.usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
                info.extent().set(data.width, data.height, 1)
            }.orThrow("Unable to allocate image: $data")

            copy(staging, image)

            staging.free()

            return image
        }
    }
    private fun copy(srcImage: BufferAlloc, destImage: VkImage) {

        // Aspect can be one of:
        // VK_IMAGE_ASPECT_COLOR_BIT
        // VK_IMAGE_ASPECT_DEPTH_BIT
        // VK_IMAGE_ASPECT_STENCIL_BIT
        val aspect = VK_IMAGE_ASPECT_COLOR_BIT

        val region = VkBufferImageCopy.calloc(1)
            .bufferOffset(srcImage.memoryOffset.toLong())
            .bufferRowLength(0)
            .bufferImageHeight(0)

        region.imageSubresource()
            .aspectMask(aspect)
            .mipLevel(0)
            .baseArrayLayer(0)
            .layerCount(1)

        region.imageOffset().set(0,0,0)

        region.imageExtent().set(destImage.dimensions[0], destImage.dimensions[1], destImage.dimensions[2])

        commandPool.beginOneTimeSubmit { cmd->

            /**
             * Change dest image layout:
             * from VK_IMAGE_LAYOUT_UNDEFINED
             * to   VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
             */
            cmd.setImageLayout(
                destImage,
                aspect,
                VK_IMAGE_LAYOUT_UNDEFINED,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL
            )

            /** Copy the staging buffer to the GPU destImage */
            cmd.copyBufferToImage(
                srcImage.buffer,
                destImage,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                region
            )

            /**
             * Change the GPU image layout from TRANSFER_DST_OPTIMAL
             *                               to SHADER_READ_ONLY_OPTIMAL
             */
            cmd.setImageLayout(
                destImage,
                aspect,
                VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL
            )
            cmd.end()

            log("Copying to GPU: $destImage")
            val start = System.nanoTime()

            vk.queues.get(Queues.TRANSFER).submitAndWait(cmd)

            val end = System.nanoTime()
            log("Copy took ${(end -start)/1_000_000.0} ms")
        }
        region.free()
    }
    private fun allocateMemory(size:Int) {
        this.deviceMemory =
            vk.allocateMemory(
                size              = size,
                desiredMemFlags   = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                undesiredMemFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                bufferUsage       = VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                imageUsage        = VK_IMAGE_USAGE_TRANSFER_DST_BIT
            ).orThrow()
        log("Allocated ${size/MEGABYTE} MB of device texture memory")

        val stagingSize = Math.min(64.megabytes(), size)

        this.stagingMemory =
            vk.allocateMemory(
                size              = stagingSize,
                desiredMemFlags   = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT,
                undesiredMemFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                bufferUsage       = VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                imageUsage        = 0
            ).orThrow()
        log("Allocated ${stagingSize/MEGABYTE} MB of texture staging texture memory")

        this.stagingBuffer = stagingMemory.allocStagingSrcBuffer(stagingSize).orThrow()
    }
    private fun log(msg:String) {
        if(name.isBlank()) logger.info(msg) else logger.info("[$name] $msg")
    }
    private fun logUsage() {
        val alloced = deviceMemory.bytesAllocated
        val total   = deviceMemory.size
        val percent = alloced*100.0 / total
        log("Allocated ${alloced/MEGABYTE.toFloat()} / ${total/MEGABYTE} MB ($percent %)")
    }
}

