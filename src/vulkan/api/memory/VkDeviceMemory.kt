package vulkan.api.memory

import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.buffer.VkBuffer
import vulkan.api.image.VkImage
import vulkan.common.MEGABYTE
import vulkan.common.MemoryType
import vulkan.common.log
import vulkan.misc.Allocator
import vulkan.misc.VkBufferUsageFlags
import vulkan.misc.check
import java.nio.ByteBuffer

class VkDeviceMemory(private val physicalDeviceProperties:VkPhysicalDeviceProperties,
                     internal val device: VkDevice,
                     val handle:Long,
                     val size:Int,
                     val type: MemoryType
)
{
    private val mappedPtr:Long
    private val allocator = Allocator(size)

    val bytesAllocated get() = allocator.bytesUsed
    val bytesFree get()      = allocator.bytesFree

    enum class Reason { OK, INCORRECT_TYPE, OUT_OF_MEMORY }

    init{
        if(type.isHostVisible) {
            val ptr = memCallocPointer(1)
            vkMapMemory(device, handle, 0, VK_WHOLE_SIZE, 0, ptr).check()
            mappedPtr = ptr.get(0)
            memFree(ptr)
        } else {
            mappedPtr = VK_NULL_HANDLE
        }
    }
    fun free() {
        if(type.isHostVisible) {
            vkUnmapMemory(device, handle)
        }
        vkFreeMemory(device, handle, null)
    }
    fun allocBuffer(size:Int,
                    overrides:(VkBufferCreateInfo)->Unit,
                    onError:(reason:Reason)->Unit,
                    onSuccess:(b: VkBuffer)->Unit)
    {
        // VkBufferCreateFlags
        //    VK_BUFFER_CREATE_SPARSE_BINDING_BIT
        //    VK_BUFFER_CREATE_SPARSE_RESIDENCY_BIT
        //    VK_BUFFER_CREATE_SPARSE_ALIASED_BIT
        //    VK_BUFFER_CREATE_PROTECTED_BIT

        /** Set default values */
        val info = VkBufferCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .size(size.toLong())
            .flags(0)
            .usage(0)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .pQueueFamilyIndices(null)

        /** Allow the user to change some values */
        overrides(info)

        /** Make sure the user has set usage */
        assert(info.usage() != 0)

        val pBuffer = memAllocLong(1)
        vkCreateBuffer(device, info, null, pBuffer).check()

        val memReqs = VkMemoryRequirements.calloc()
        vkGetBufferMemoryRequirements(device, pBuffer.get(0), memReqs)

        /** Check type bits */
        if((memReqs.memoryTypeBits() and (1 shl type.index))==0) {
            return onError(Reason.INCORRECT_TYPE)
        }

        val offset = allocator.alloc(memReqs.size().toInt(), memReqs.alignment().toInt())
        if(offset==-1) {
            return onError(Reason.OUT_OF_MEMORY)
        }

        val buffer = VkBuffer(
            physicalDeviceProperties,
            device,
            this,
            pBuffer.get(0),
            offset,
            memReqs.size().toInt(),
            info.usage()
        )

        vkBindBufferMemory(device, buffer.handle, handle, offset.toLong()).check()

        info.free()
        memReqs.free()
        memFree(pBuffer)

        onSuccess(buffer)
    }
    fun allocateImage(overrides:(VkImageCreateInfo)->Unit,
                      onError:(reason:Reason)->Unit,
                      onSuccess:(b: VkImage)->Unit)
    {
        // VkImageCreateFlags
        //    VK_IMAGE_CREATE_SPARSE_BINDING_BIT
        //    VK_IMAGE_CREATE_SPARSE_RESIDENCY_BIT
        //    VK_IMAGE_CREATE_SPARSE_ALIASED_BIT
        //    VK_IMAGE_CREATE_MUTABLE_FORMAT_BIT
        //    VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT
        //    VK_IMAGE_CREATE_ALIAS_BIT
        //    VK_IMAGE_CREATE_SPLIT_INSTANCE_BIND_REGIONS_BIT
        //    VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT
        //    VK_IMAGE_CREATE_BLOCK_TEXEL_VIEW_COMPATIBLE_BIT
        //    VK_IMAGE_CREATE_EXTENDED_USAGE_BIT
        //    VK_IMAGE_CREATE_PROTECTED_BIT
        //    VK_IMAGE_CREATE_DISJOINT_BIT
        //    VK_IMAGE_CREATE_CORNER_SAMPLED_BIT_NV
        //    VK_IMAGE_CREATE_SAMPLE_LOCATIONS_COMPATIBLE_DEPTH_BIT_EXT
        //    VK_IMAGE_CREATE_SUBSAMPLED_BIT_EXT
        //    VK_IMAGE_CREATE_SPLIT_INSTANCE_BIND_REGIONS_BIT
        //    VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT
        //    VK_IMAGE_CREATE_BLOCK_TEXEL_VIEW_COMPATIBLE_BIT
        //    VK_IMAGE_CREATE_EXTENDED_USAGE_BIT
        //    VK_IMAGE_CREATE_DISJOINT_BIT
        //    VK_IMAGE_CREATE_ALIAS_BIT

        /** Set default values */
        val info = VkImageCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
            .flags(0)
            .imageType(VK_IMAGE_TYPE_2D)
            .format(VK_FORMAT_R8G8B8A8_UNORM)
            .mipLevels(1)
            .arrayLayers(1)
            .samples(1)
            .tiling(VK_IMAGE_TILING_OPTIMAL)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .usage(0)

        /** Allow the user to change some values */
        overrides(info)

        /** Make sure the user has set extent and usage */
        assert(info.extent().width() != 0)
        assert(info.usage() != 0)

        val pImage = memAllocLong(1)
        vkCreateImage(device, info, null, pImage).check()

        val memReqs = VkMemoryRequirements.calloc()
        vkGetImageMemoryRequirements(device, pImage.get(0), memReqs)

        /** Check type bits */
        if((memReqs.memoryTypeBits() and (1 shl type.index))==0) {
            return onError(Reason.INCORRECT_TYPE)
        }

        val offset = allocator.alloc(memReqs.size().toInt(), memReqs.alignment().toInt())
        if(offset==-1) {
            log.error("OUT OF MEMORY requesting ${memReqs.size()} bytes")
            return onError(Reason.OUT_OF_MEMORY)
        }

        val dimensions = intArrayOf(info.extent().width(), info.extent().height(), info.extent().depth())

        val image = VkImage(device, pImage.get(0), offset, info.format(), dimensions, memReqs.size().toInt())

        vkBindImageMemory(device, image.handle, handle, offset.toLong()).check()

        memReqs.free()
        memFree(pImage)
        info.free()

        onSuccess(image)
    }

    fun mapForWriting(buffer:VkBuffer, offset:Int, size:Int, functor:(ByteBuffer)->Unit) {
        assert(offset>=0 && size>0)
        assert(size <= this.size-offset)

        val bb = map(buffer.memoryOffset + offset, size)
        functor(bb)
        flushRange(buffer.memoryOffset + offset, size)
    }
    fun mapForReading(buffer:VkBuffer, offset:Int, size:Int, functor:(ByteBuffer)->Unit) {
        assert(offset>=0 && size>0)
        assert(size <= this.size-offset)

        invalidateRange(buffer.memoryOffset + offset, size)
        val bb = map(buffer.memoryOffset + offset, size)
        functor(bb)
    }

    /** @return estimate of num bytes committed */
    fun getCommitment():Long {
        val pLong = memAllocLong(1)
        vkGetDeviceMemoryCommitment(device, handle, pLong)
        val commitment = pLong.get(0)
        memFree(pLong)
        return commitment
    }
    override fun toString() = "VkDeviceMemory(size=$size (${size/ MEGABYTE} MBs), type=$type, allocated=$bytesAllocated bytes)"

    //=======================================================================================================

    private fun map(offset:Int, size:Int):ByteBuffer {
        assert(type.isHostVisible)

//        val ptr = memCallocPointer(1)
//        vkMapMemory(device, handle, memoryOffset.toLong(), size.toLong(), 0, ptr).check()
//        val p = ptr.get(0)
//        memFree(ptr)

//        println("memoryOffset = $memoryOffset, size = $size")
//        println("p          = $p")

        return memByteBuffer(mappedPtr+offset, size)
        //return memByteBuffer(p, size)

//        val buf = mappedPtr.getByteBuffer(0, size)
//        buf.position(memoryOffset)
//        buf.limit(buf.position() + size)
//        return buf
    }
    private fun flushRange(offset:Int, size:Int) {
        assert(type.isHostVisible)
        if(type.isHostCoherent) return

        val range = VkMappedMemoryRange.calloc()
            .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            .memory(handle)
            .offset(offset.toLong())
            .size(size.toLong())

        vkFlushMappedMemoryRanges(device, range).check()
        range.free()
    }
    private fun invalidateRange(offset:Int, size:Int) {
        assert(type.isHostVisible)
        if(type.isHostCoherent) return

        val range = VkMappedMemoryRange.calloc()
            .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
            .memory(handle)
            .offset(offset.toLong())
            .size(size.toLong())

        vkInvalidateMappedMemoryRanges(device, range).check()
        range.free()
    }
}
//=======================================================================================================
fun VkDeviceMemory.allocIndexBuffer(size:Int, usage:VkBufferUsageFlags =  VK_BUFFER_USAGE_TRANSFER_DST_BIT): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_INDEX_BUFFER_BIT) },
        onError   = { r-> throw Error("Unable to allocate index buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocStagingSrcBuffer(size:Int, usage:VkBufferUsageFlags = 0): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_TRANSFER_SRC_BIT) },
        onError   = { r-> throw Error("Unable to allocate staging buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocStagingDestBuffer(size:Int, usage:VkBufferUsageFlags = VK_BUFFER_USAGE_TRANSFER_DST_BIT): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_TRANSFER_DST_BIT) },
        onError   = { r-> throw Error("Unable to allocate staging buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocStorageBuffer(size:Int, usage:VkBufferUsageFlags = VK_BUFFER_USAGE_TRANSFER_DST_BIT): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT) },
        onError   = { r-> throw Error("Unable to allocate storage buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocUniformBuffer(size:Int, usage:VkBufferUsageFlags = VK_BUFFER_USAGE_TRANSFER_DST_BIT): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT) },
        onError   = { r-> throw Error("Unable to allocate uniform buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocVertexBuffer(size:Int, usage:VkBufferUsageFlags = VK_BUFFER_USAGE_TRANSFER_DST_BIT): VkBuffer? {
    var buffer: VkBuffer? = null
    allocBuffer(
        size = size,
        overrides = { info -> info.usage(usage or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT) },
        onError   = { r-> throw Error("Unable to allocate vertex buffer: $r}") },
        onSuccess = { b-> buffer = b }
    )
    return buffer
}
fun VkDeviceMemory.allocImage(overrides:(VkImageCreateInfo)->Unit) : VkImage? {
    var image:VkImage? = null

    allocateImage(
        overrides = overrides,
        onError   = { r-> throw Error("Unable to allocate image: $r}") },
        onSuccess = { img-> image = img })

    return image
}