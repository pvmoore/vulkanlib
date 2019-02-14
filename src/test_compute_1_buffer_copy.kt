
import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.*
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.descriptor.*
import vulkan.api.memory.StagingTransfer
import vulkan.api.memory.createStagingTransfer
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.app.KeyEvent
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.misc.megabytes
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vulkan compute example of copying one buffer to another.
 * Shows use of push constants and spec constants.
 */

fun main() {
    println("Testing Vulkan Compute...")

    val client = ComputeSimpleBufferCopy()
    var app: VulkanApplication? = null

    fun initialise() {
        app = VulkanApplication(client)
        app?.initialise()

        val totalMem = Runtime.getRuntime().totalMemory()
        val freeMem  = Runtime.getRuntime().freeMemory()
        println("total: ${totalMem/ MEGABYTE} MB")
        println("free : ${freeMem/ MEGABYTE} MB")
        println("used : ${(totalMem-freeMem)/ MEGABYTE} MB")
    }
    fun destroy() {
        client.destroy()
        app?.destroy()
    }
    fun enterMainLoop() {
        app!!.graphics.enterLoop()
    }

    try{
        initialise()
        enterMainLoop()
    }catch(e:Throwable) {
        e.printStackTrace()
    }finally {
        destroy()
    }

    println("Finished")
}
//=========================================================================================

//=========================================================================================
private class ComputeSimpleBufferCopy : VulkanClient(
    windowed                = true,
    width                   = 800,
    height                  = 800,
    windowTitle             = "Vulkan Compute Buffer Copy Test",
    enableVsync             = false,
    prefNumSwapChainBuffers = 2)
{
    private data class UBO(val value1:Float, val value2:Float, val _pad: Vector2i = Vector2i()) : Transferable {
        override fun writeTo(dest: ByteBuffer) {
            dest.putFloat(value1)
            dest.putFloat(value2)
        }
        override fun size() = 16
    }
//    inner class FrameResource {
//        val computeBuffer    = computeCP!!.alloc()
//        val transferBuffer   = transferCP!!.alloc()
//        val transferFinished = device.createSemaphore()
//        val computeFinished  = device.createSemaphore()
//        val descriptorSet    =
//
//        fun destroy() {
//            computeCP?.free(computeBuffer)
//            transferCP?.free(transferBuffer)
//            transferFinished.destroy()
//            computeFinished.destroy()
//        }
//    }

    //==============================================================================================
    private lateinit var vk: VulkanApplication
    private lateinit var device: VkDevice

    private var uniformBufferAlloc : BufferAlloc? = null

    private var descriptorPool: VkDescriptorPool? = null
    private var dsLayout: VkDescriptorSetLayout? = null
    private var descriptorSet: VkDescriptorSet? = null

    private var computeCP: VkCommandPool? = null
    private var computeCommandBuffer:VkCommandBuffer? = null
    private var computeCompleteSemaphore:VkSemaphore? = null

    private val uploader:StagingTransfer by lazy { vk.createStagingTransfer(buffers.get(VulkanBuffers.STAGING_UPLOAD)) }

    private val uploadByteBuffer = ByteBuffer.allocateDirect(4.megabytes())
                                             .order(ByteOrder.nativeOrder())

    private val pipeline      = ComputePipeline()
    private val ubo           = UBO(1f, 2f)
    private val specConstants = SpecConstants(2).set(0, 5f).set(1, 7f)
    private val pushConstants = PushConstants(2)

    private val clearColour   = VkClearValue.calloc(1)
    private val bufferBarrier = VkBufferMemoryBarrier.calloc(1)

    private val memory = VulkanMemory()
    private val buffers = VulkanBuffers()

    //private val frameResources = ArrayList<FrameResource>()

    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {

    }

    override fun selectQueues(props : VkQueueFamilyProperties.Buffer, queues : Queues) {
        super.selectQueues(props, queues)

        /** Select the first compute queue that we see */
        props.forEachIndexed { i, family ->
            if(family.queueCount() > 0) {
                if(queues.isCompute(family.queueFlags())) {
                    queues.select(Queues.COMPUTE, i, 1)
                }
            }
        }
    }
    override fun vulkanReady(vk: VulkanApplication) {
        println("Vulkan ready")
        this.vk     = vk
        this.device = vk.device

        initialise()

        vk.graphics.showWindow()
    }
    override fun destroy() {
        println("Destroying Client")
        device?.let {
            device.waitForIdle()

            uploader.destroy()

            clearColour.free()
            bufferBarrier.free()

            dsLayout?.destroy()
            descriptorPool?.destroy()

            computeCP?.let { cp->
                cp.free(computeCommandBuffer!!)
                cp.destroy()
            }

            computeCompleteSemaphore?.destroy()

            pipeline?.destroy()

            buffers.destroy()
            memory.destroy()
        }
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {

        update(frame, res)

        pushConstants.set(0, 10f)
        pushConstants.set(1, 20f)

        val cmd = computeCommandBuffer!!

        cmd.beginOneTimeSubmit()
            .copyBuffer(
                buffers.get(VulkanBuffers.STAGING_UPLOAD),
                buffers.get("input"))
            .bindPipeline(pipeline)
            .bindDescriptorSets(
                VK_PIPELINE_BIND_POINT_COMPUTE,
                pipeline!!.layout,
                0,
                arrayOf(descriptorSet!!),
                intArrayOf()
            )
            .pushConstants(
                pipeline!!.layout,
                VK_SHADER_STAGE_COMPUTE_BIT,
                0,
                pushConstants.floatBuffer
            )
            .dispatch(1.megabytes(), 1, 1)
            .copyBuffer(
                buffers.get("output"),
                buffers.get(VulkanBuffers.STAGING_DOWNLOAD))

            .end()

        vk.queues.get(Queues.COMPUTE).submit(
            arrayOf(cmd),
            arrayOf(),      // wait semaphores
            intArrayOf(),   // wait stages
            arrayOf(computeCompleteSemaphore!!)       // signal semaphores
        )

        val b = res.cmd
        b.beginOneTimeSubmit()

        // Note: Do buffer updates/transfers outside the render pass

        // Renderpass initialLayout = UNDEFINED
        // The renderpass loadOp    = CLEAR
        b.beginRenderPass(
            vk.graphics.renderPass,
            res.frameBuffer,
            clearColour,
            Vector4i(0,0, vk.graphics.windowSize.x, vk.graphics.windowSize.y),
            true
        );

        // Renderpass finalLayout = PRESENT_SRC_KHR
        b.endRenderPass()
        b.end()

        /// Submit render buffer
        vk.queues.get(Queues.GRAPHICS).submit(
            arrayOf(b),
            arrayOf(computeCompleteSemaphore!!),                 // wait semaphores
            intArrayOf(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT),     // wait stages
            arrayOf(res.renderFinished),    // signal semaphores
            res.fence                       // fence
        )

        afterRender(frame, res)
    }
    //=====================================================================================================
    //   _____      _            _
    //  |  __ \    (_)          | |
    //  | |__) | __ ___   ____ _| |_ ___
    //  |  ___/ '__| \ \ / / _` | __/ _ \
    //  | |   | |  | |\ V / (_| | | |  _/
    //  |_|   |_|  |_| \_/ \__,_| \__\__|
    //
    //=====================================================================================================

    private fun update(frame: FrameInfo, res: PerFrameResource) {

        vk.graphics.drainWindowEvents().forEach {
            if(it is KeyEvent && it.key == GLFW.GLFW_KEY_ESCAPE) {
                vk.graphics.postCloseMessage()
            }
        }

        fun updateStagingBuffer() {
            /** Write data to staging upload buffer */

            val b = buffers.get(VulkanBuffers.STAGING_UPLOAD)

            b.mapForWriting(0, b.size) { bb->
                bb.put(uploadByteBuffer)
                uploadByteBuffer.flip()
            }
        }

        //updateStagingBuffer()

    }
    private fun afterRender(frame: FrameInfo, res: PerFrameResource) {
        fun checkResult() {
            /** Read data from staging download buffer and check them */

            val b = buffers.get(VulkanBuffers.STAGING_DOWNLOAD)

            b.mapForReading(0, b.size) { bb->
                val floats = bb.asFloatBuffer()

                val expected = floatArrayOf(
                    42f, 43f, 44f, 45f, 46f, 47f, 48f, 49f, 50f, 51f, 52f, 53f, 54f, 55f, 56f, 57f, 58f
                )
                val actual = (0..16).map { floats[it] }.toFloatArray()

//            print("Output:")
//            (0..16).forEach {
//                print("${floats[it]}, ")
//            }
//            println()

                assert(actual.contentEquals(expected))
            }
        }
        checkResult()
    }
    private fun initialise() {
        println("--------------------------------------------------------------------- Initialising client")

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

        memory.init(vk)
            .createOnDevice(VulkanMemory.DEVICE, 256.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 32.megabytes())
            .createStagingDownload(VulkanMemory.STAGING_DOWNLOAD, 32.megabytes())
        log.info("$memory")

        buffers.init(vk)
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 4.megabytes())
            .createStagingUploadBuffer(VulkanBuffers.STAGING_DOWNLOAD, memory.get(VulkanMemory.STAGING_DOWNLOAD), 4.megabytes())
            .createUniformBuffer(VulkanBuffers.UNIFORM, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createStorageBuffer("input", memory.get(VulkanMemory.DEVICE), 4.megabytes())
            .createStorageBuffer("output", memory.get(VulkanMemory.DEVICE), 4.megabytes())
        log.info("$buffers")

        uniformBufferAlloc = buffers.get(VulkanBuffers.UNIFORM).allocate(ubo.size())
        log.info("uniformBufferAlloc = $uniformBufferAlloc")

        /** Set the clear colour */
        clearColour.color()
            .float32(0, 0.5f)
            .float32(1, 0.1f)
            .float32(2, 0.1f)
            .float32(3, 0.1f)

        /** Initialise the input data */
        val dataIn = FloatArray(size = 1.megabytes(), init = {
            when {
                it > 1.megabytes() - 10 -> 1.megabytes().toFloat() - it
                else -> it * 1f
            }
        })
        uploadByteBuffer.asFloatBuffer().put(dataIn).flip()

        println("inputs = ${uploadByteBuffer.asFloatBuffer()[1]}")
//
//        println("upload buffer = ${dataIn[1]},${dataIn[2]}")
//        println("upload buffer = ${fb.get(1)},${fb.get(2)}")


        createCommandPools()
        createDescriptors()
        createPipeline()
        //createFrameResources()

        bufferBarrier
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .buffer(buffers.get("output").handle)
            .offset(0)
            .size(VK_WHOLE_SIZE)

        computeCompleteSemaphore = device.createSemaphore()

        /** Write the UBO */
        /** Must be a multiple of 16 bytes */
        assert((ubo.size() and 15)==0)

        uploader.upload(uniformBufferAlloc!!, ubo)

        /** Write the input buffer */
        uploader.upload(buffers.get("input"), 0, uploadByteBuffer)

        println("------------------------------------------------------------------------------------------")
    }
    private fun createCommandPools() {
        println("Creating command pools")

        computeCP  = device.createCommandPool(vk.queues.getFamily(Queues.COMPUTE),
                                              VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or
            VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
        )

        computeCommandBuffer = computeCP?.alloc()
    }
    private fun createDescriptors() {
        println("Creating bindings")
        /** Descriptor pool with:
         *      1 uniform buffer and
         *      2 storage buffers,
         *      1 set only
         */
        descriptorPool = device.createDescriptorPool(numSizes = 2) { info, sizes ->
            info.maxSets(1)

            /** 1 uniform buffer */
            sizes[0].descriptorCount(1)
                    .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)

            /** 2 storage buffers */
            sizes[1].descriptorCount(2)
                    .type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
        }
        /**
         * Layout:
         *  0 - uniform buffer
         *  1 - storage buffer
         *  2 - storage buffer
         */
        dsLayout = device.createDescriptorSetLayout(numBindings=3) { bindings->
            bindings[0]
                .binding(0)
                .descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
            bindings[1]
                .binding(1)
                .descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
            bindings[2]
                .binding(2)
                .descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
                .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
        }

        /** Get our 1 set */
        descriptorSet = descriptorPool!!.allocSets(arrayOf(dsLayout!!)).first()

        /** Update the set with our buffers */
        val bufferInfos = VkDescriptorBufferInfo.calloc(3).also {
            it[0].buffer(buffers.get(VulkanBuffers.UNIFORM).handle)
            it[0].offset(uniformBufferAlloc!!.bufferOffset.toLong())
            it[0].range(uniformBufferAlloc!!.size.toLong())

            it[1].buffer(buffers.get("input").handle)
            it[1].offset(0)
            it[1].range(buffers.get("input").size.toLong())

            it[2].buffer(buffers.get("output").handle)
            it[2].offset(0)
            it[2].range(buffers.get("output").size.toLong())
        }

        val imageInfos = VkDescriptorImageInfo.calloc(1)
        // not used

        val writes = VkWriteDescriptorSet.calloc(1)
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(descriptorSet!!.handle)
            .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
            .pBufferInfo(bufferInfos)
            .pImageInfo(null)
            .dstBinding(0)
            .dstArrayElement(0)

        vkUpdateDescriptorSets(device, writes, null)

        bufferInfos.free()
        imageInfos.free()
        writes.free()
    }
    private fun createPipeline() {
        println("Creating pipeline")
        val context = RenderContext(vk, device, vk.graphics.renderPass, buffers)

        pipeline.init(context)
            .withDSLayouts(arrayOf(dsLayout!!))
            .withShader("ComputeSimpleBufferCopy.comp", specConstants)
            .withPushConstants(firstIndex = 0, count = pushConstants.count)
            .build()
        println("pipeline = $pipeline")
    }
}