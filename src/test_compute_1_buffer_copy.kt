
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.*
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.memory.StagingTransfer
import vulkan.api.memory.createStagingTransfer
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.app.KeyEvent
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.d2.Camera2D
import vulkan.d2.FPS
import vulkan.misc.RGBA
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
private class ComputeSimpleBufferCopy : VulkanClient(Parameters(
    windowed                = true,
    width                   = 800,
    height                  = 800,
    windowTitle             = "Vulkan Compute Buffer Copy Test",
    enableVsync             = false,
    prefNumSwapChainBuffers = 2))
{
    private data class UBO(val value1:Float,
                           val value2:Float,
                           val _pad: Vector2i = Vector2i()) : AbsUBO()

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
    private lateinit var device:VkDevice
    private lateinit var context:RenderContext

    private var stagingUploadBuffer      = null as BufferAlloc?
    private var stagingDownloadBuffer    = null as BufferAlloc?
    private var shaderReadBuffer         = null as BufferAlloc?
    private var shaderWriteBuffer        = null as BufferAlloc?
    private var computeCP                = null as VkCommandPool?
    private var computeCommandBuffer     = null as VkCommandBuffer?
    private var computeCompleteSemaphore = null as VkSemaphore?

    private val uploader:StagingTransfer by lazy { vk.createStagingTransfer(buffers.get(VulkanBuffers.STAGING_UPLOAD)) }

    private val uploadByteBuffer = ByteBuffer.allocateDirect(4.megabytes())
                                             .order(ByteOrder.nativeOrder())

    private val pipeline      = ComputePipeline()
    private val ubo           = UBO(1f, 2f)
    private val specConstants = SpecConstants(2).set(0, 5f).set(1, 7f)
    private val pushConstants = PushConstants(2)
    private val clearColour   = ClearColour(RGBA(0.2f, 0.1f, 0.2f, 1f))

    private val bufferBarrier = VkBufferMemoryBarrier.calloc(1)
    private val memoryBarrier = VkMemoryBarrier.calloc(1)


    private val memory      = VulkanMemory()
    private val buffers     = VulkanBuffers()
    private val descriptors = Descriptors()
    private val fps         = FPS()
    private val camera      = Camera2D()

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

            fps.destroy()
            ubo.destroy()

            clearColour.destroy()
            bufferBarrier.free()
            memoryBarrier.free()

            computeCP?.let { cp->
                cp.free(computeCommandBuffer!!)
                cp.destroy()
            }

            computeCompleteSemaphore?.destroy()

            pipeline?.destroy()

            descriptors.destroy()

            buffers.destroy()
            memory.destroy()
        }
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {

        update(frame, res)

        pushConstants.set(0, 10f)
        pushConstants.set(1, 20f)

        computeCommandBuffer!!.run {
            beginOneTimeSubmit()

            transfer(ubo)

            copyBuffer(stagingUploadBuffer!!, shaderReadBuffer!!)

            bindPipeline(pipeline)
            bindDescriptorSets(
                VK_PIPELINE_BIND_POINT_COMPUTE,
                pipeline!!.layout,
                0,
                arrayOf(descriptors.layout(0).set(0)),
                intArrayOf()
            )
            pushConstants(
                pipeline!!.layout,
                VK_SHADER_STAGE_COMPUTE_BIT,
                0,
                pushConstants.floatBuffer
            )

            dispatch(1.megabytes() / 64, 1, 1)

            copyBuffer(shaderWriteBuffer!!, stagingDownloadBuffer!!)

//            pipelineBarrier(
//                srcStageMask = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
//                destStageMask = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT,
//                dependencyFlags = 0,
//                bufferMemoryBarriers = null,
//                imageMemoryBarriers = null,
//                memoryBarriers = memoryBarrier
//            )

            end()
        }

        vk.queues.get(Queues.COMPUTE).submit(
            arrayOf(computeCommandBuffer!!),
            arrayOf(),      // wait semaphores
            intArrayOf(),   // wait stages
            arrayOf(computeCompleteSemaphore!!)       // signal semaphores
        )

        val b = res.cmd
        b.beginOneTimeSubmit()

        // Note: Do buffer updates/transfers outside the render pass

        fps.beforeRenderPass(frame, res)

        // Renderpass initialLayout = UNDEFINED
        // The renderpass loadOp    = CLEAR
        b.beginRenderPass(
            vk.graphics.renderPass,
            res.frameBuffer,
            clearColour.value,
            vk.graphics.swapChain.area,
            true
        );

        fps.insideRenderPass(frame, res)

        // Renderpass finalLayout = PRESENT_SRC_KHR
        b.endRenderPass()
        b.end()

        /// Submit render buffer
        vk.queues.get(Queues.GRAPHICS).submit(
            arrayOf(b),
            arrayOf(computeCompleteSemaphore!!, res.imageAvailable),                 // wait semaphores
            intArrayOf(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),     // wait stages
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

        //updateStagingBuffer()

    }
    private fun afterRender(frame: FrameInfo, res: PerFrameResource) {
        fun checkResult() {
            /** Read data from staging download buffer and check them */

            stagingDownloadBuffer!!.mapForReading { bb->
                val floats = bb.asFloatBuffer()

                val expected = floatArrayOf(
                    45f, 46f, 47f, 48f, 49f, 50f, 51f, 52f, 53f, 54f, 55f, 56f, 57f, 58f, 59f, 60f, 61f
                )
                val actual = (0..16).map { floats[it] }.toFloatArray()

                print("Output:")
                (0..16).forEach {
                    print("${floats[it]}, ")
                }
                println()

                assert(actual.contentEquals(expected))
            }
        }

        if(frame.number==1L) {
            checkResult()
        }
    }
    private fun initialise() {
        println("--------------------------------------------------------------------- Initialising client")

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

        this.camera.resizeWindow(vk.graphics.windowSize)

        memory.init(vk)
            .createOnDevice(VulkanMemory.DEVICE, 256.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 32.megabytes())
            .createStagingDownload(VulkanMemory.STAGING_DOWNLOAD, 32.megabytes())
        log.info("$memory")

        buffers.init(vk)
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 8.megabytes())
            .createStagingDownloadBuffer(VulkanBuffers.STAGING_DOWNLOAD, memory.get(VulkanMemory.STAGING_DOWNLOAD), 8.megabytes())
            .createUniformBuffer(VulkanBuffers.UNIFORM, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createVertexBuffer(VulkanBuffers.VERTEX, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createIndexBuffer(VulkanBuffers.INDEX, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createStorageBuffer("input", memory.get(VulkanMemory.DEVICE), 4.megabytes())
            .createStorageBuffer("output", memory.get(VulkanMemory.DEVICE), 4.megabytes(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
        log.info("$buffers")

        //uniformBufferAlloc = buffers.get(VulkanBuffers.UNIFORM).allocate(ubo.size())
        //log.info("uniformBufferAlloc = $uniformBufferAlloc")

        stagingUploadBuffer   = buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(4.megabytes())
        stagingDownloadBuffer = buffers.get(VulkanBuffers.STAGING_DOWNLOAD).allocate(4.megabytes())

        shaderReadBuffer  = buffers.get("input").allocate(4.megabytes())
        shaderWriteBuffer = buffers.get("output").allocate(4.megabytes())

        this.context = RenderContext(vk, vk.device, vk.graphics.renderPass, buffers)

        this.ubo.init(context)

        this.fps
            .init(context)
            .camera(camera)


        /** Initialise the input data */
        val dataIn = FloatArray(size = 1.megabytes(), init = {
            when {
                it > 1.megabytes() - 10 -> 1.megabytes().toFloat() - it
                else -> it * 1f
            }
        })
        updateStagingUploadBuffer(dataIn)

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

        memoryBarrier
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER)
            .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            .dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)


        computeCompleteSemaphore = device.createSemaphore()
    }
    private fun updateStagingUploadBuffer(array:FloatArray) {

        uploadByteBuffer.asFloatBuffer().put(array).flip()

        println("inputs = ${uploadByteBuffer.asFloatBuffer()[1]}")

        stagingUploadBuffer!!.mapForWriting { bb->
            bb.put(uploadByteBuffer)
            uploadByteBuffer.flip()
        }
    }
    private fun createCommandPools() {
        computeCP  = device.createCommandPool(
            vk.queues.getFamily(Queues.COMPUTE),
             VK_COMMAND_POOL_CREATE_TRANSIENT_BIT or VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
        )

        computeCommandBuffer = computeCP?.alloc()
    }
    private fun createDescriptors() {
        /**
         * Layout 0 bindings:
         *    0     uniform buffer
         *    1     storage buffer
         *    2     storage buffer
         */
        descriptors
            .init(context)
            .createLayout()
                .uniformBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
                .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
                .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
                .numSets(1)
            .build()

        /** Create 1 set from layout 0 */
        descriptors
            .layout(0).createSet()
                .add(ubo.deviceBuffer)
                .add(shaderReadBuffer!!)
                .add(shaderWriteBuffer!!)
            .write()
    }
    private fun createPipeline() {
        val context = RenderContext(vk, device, vk.graphics.renderPass, buffers)

        pipeline.init(context)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader("ComputeSimpleBufferCopy.comp", specConstants)
            .withPushConstants(firstIndex = 0, count = pushConstants.count)
            .build()
    }
}