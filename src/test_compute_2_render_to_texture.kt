
import org.joml.Matrix4f
import org.joml.Vector2i
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.*
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.image.VkImage
import vulkan.api.memory.allocImage
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.app.GraphicsComponent
import vulkan.app.KeyEvent
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.d2.Camera2D
import vulkan.d2.FPS
import vulkan.d2.Quad
import vulkan.misc.RGBA
import vulkan.misc.megabytes
import vulkan.misc.orThrow

/**
 * Vulkan compute to texture example.
 *
 * The compute target image is the swap frame image view. This is not supported in all
 * graphics cards but works with both my AMD RX 470 and nVidia 1660.
 */

fun main(args:Array<String>) {
    val client = RenderToTextureExample()
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
private class RenderToTextureExample : VulkanClient(
    windowed                = true,
    width                   = 800,
    height                  = 800,
    windowTitle             = "Vulkan Render To Texture Example",
    enableVsync             = false,
    prefNumSwapChainBuffers = 3)
{
    private class FrameResource(
        val computeBuffer : VkCommandBuffer,
        val transferBuffer : VkCommandBuffer,
        val transferFinished : VkSemaphore,
        val computeFinished : VkSemaphore,
        val targetImage:VkImage,
        val quad:Quad = Quad()
    )

    private val memory              = VulkanMemory()
    private val buffers             = VulkanBuffers()
    private val camera              = Camera2D()
    private val clearColour         = ClearColour(RGBA(0.2f, 0f, 0f, 1f))
    private val fps                 = FPS()
    private val frameResources      = ArrayList<FrameResource>()
    private val computeDescriptors  = Descriptors()
    private val computePipeline     = ComputePipeline()
    private var deviceReadBuffer    = null as BufferAlloc?
    private var stagingWriteBuffer  = null as BufferAlloc?
    private var sampler             = null as VkSampler?
    private var currentSecond       = 0

    private lateinit var computeCP:VkCommandPool
    private lateinit var transferCP:VkCommandPool

    private lateinit var vk: VulkanApplication
    private lateinit var device: VkDevice
    private lateinit var context: RenderContext
    private lateinit var graphics: GraphicsComponent

    override fun destroy() {
        log.info("Destroying Client")
        device?.let {
            device.waitForIdle()

            clearColour.destroy()

            sampler?.destroy()

            fps.destroy()

            frameResources.forEach { fr ->
                fr.transferFinished.destroy()
                fr.computeFinished.destroy()
                fr.targetImage.destroy()
                fr.quad.destroy()
            }

            computeCP.destroy()
            transferCP.destroy()

            computePipeline.destroy()
            computeDescriptors.destroy()

            buffers.destroy()
            memory.destroy()
        }
    }
    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {
        f.geometryShader(true)
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
    override fun vulkanReady(vk : VulkanApplication) {
        this.vk       = vk
        this.graphics = vk.graphics
        this.device   = vk.device

        this.memory.init(vk)
        this.buffers.init(vk)
        this.camera.resizeWindow(vk.graphics.windowSize)

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

        this.context = RenderContext(vk, vk.device, graphics.renderPass, buffers)

        /**
         * Device: 256 Mb
         *      Vertex:  16
         *      Index:   16
         *      Uniform: 1
         *      Storage: 64
         *
         * Staging upload:   64 Mb
         * Staging download: 64 Mb
         */
        this.memory.init(context.vk)
            .createOnDevice(VulkanMemory.DEVICE, 256.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 64.megabytes())
            .createStagingDownload(VulkanMemory.STAGING_DOWNLOAD, 64.megabytes())

        this.buffers.init(context.vk)
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 64.megabytes())
            .createStagingDownloadBuffer(VulkanBuffers.STAGING_DOWNLOAD, memory.get(VulkanMemory.STAGING_DOWNLOAD), 64.megabytes())
            .createVertexBuffer(VulkanBuffers.VERTEX, memory.get(VulkanMemory.DEVICE), 16.megabytes())
            .createIndexBuffer(VulkanBuffers.INDEX, memory.get(VulkanMemory.DEVICE), 16.megabytes())
            .createUniformBuffer(VulkanBuffers.UNIFORM, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createStorageBuffer(VulkanBuffers.STORAGE, memory.get(VulkanMemory.DEVICE), 64.megabytes())

        val bufferSize = graphics.swapChain.extent.x * graphics.swapChain.extent.y * 4 * 3
        log.info("buffer size = $bufferSize")

        this.deviceReadBuffer   = buffers.get(VulkanBuffers.STORAGE).allocate(bufferSize)
        this.stagingWriteBuffer = buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(bufferSize)

        this.fps
            .init(context)
            .camera(camera)

        this.computeCP = device.createCommandPool(
            vk.queues.getFamily(Queues.COMPUTE)
        )
        this.transferCP = device.createCommandPool(
            vk.queues.getFamily(Queues.TRANSFER),
            VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
        )

        this.sampler = device.createSampler { info->
            info.magFilter(VK_FILTER_NEAREST)
            info.minFilter(VK_FILTER_NEAREST)
            info.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
        }

        /**
         * Bindings:
         *    0     storage buffer
         *    1     storage image
         */
        this.computeDescriptors
            .init(context)
            .createLayout()
                .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
                .storageImage(VK_SHADER_STAGE_COMPUTE_BIT)
                .numSets(graphics.swapChain.numImages)
            .build()

        this.computePipeline
            .init(context)
            .withDSLayouts(arrayOf(computeDescriptors.layout(0).dsLayout))
            .withShader("RenderToTexture.comp")
            .build()

        createFrameResources()
        recordComputeFrames()

        vk.graphics.showWindow()
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {
        updateFrame(frame)

        val computeFrame   = frameResources[res.index]
        val waitSemaphores = mutableListOf(res.imageAvailable)
        val waitStages     = mutableListOf(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)

        // Notes on transfers:
        // 1 - Do we need a barrier to transfer ownership from
        //     transfer queue to compute queue? Seems to work
        //     ok without it.
        // 2 - It might be faster to use an alternative method
        //     eg. use compute queue to do transfer.
        // 3 - This copies the whole host buffer to device. Could
        //     also use updateBuffer if the update is <65536 bytes
        //     and the transfer needs to be outside the render pass.
        //     Or just change the size of the VkBufferCopy in the code below.
        // 4 - All frames are looking at the same buffer storage
        //     so any updates will likely need to be done carefully
        //     in an additive way, freeing areas after 3 frames or so.

        if(frame.seconds.toInt() > currentSecond) {
            currentSecond = frame.seconds.toInt()

            createUploadData(frame.number)
            computeFrame.transferBuffer.run {
                begin()
                copyBuffer(stagingWriteBuffer!!, deviceReadBuffer!!)
                end()
            }
            vk.queues.get(Queues.TRANSFER).submit(
                cmdBuffers       = arrayOf(computeFrame.transferBuffer),
                signalSemaphores = arrayOf(computeFrame.transferFinished),
                waitSemaphores   = arrayOf(),
                waitStages       = intArrayOf(),
                fence            = null
            )
            waitSemaphores.add(computeFrame.transferFinished)
            waitStages.add(VK_PIPELINE_STAGE_TRANSFER_BIT)
        }

        val computeQueue = vk.queues.get(Queues.COMPUTE)

        // Submit our compute buffer.
        // Wait for imageAvailable semaphore.
        computeQueue.submit(
            cmdBuffers       = arrayOf(computeFrame.computeBuffer),
            waitSemaphores   = waitSemaphores.toTypedArray(),
            waitStages       = waitStages.toIntArray(),
            signalSemaphores = arrayOf(computeFrame.computeFinished),
            fence            = null
        )

        res.cmd.let { b->
            b.beginOneTimeSubmit()

            fps.beforeRenderPass(frame, res)
            computeFrame.quad.beforeRenderPass(frame, res)

            // Renderpass initialLayout = UNDEFINED
            // The renderpass loadOp    = CLEAR
            b.beginRenderPass(
                context.renderPass!!,
                res.frameBuffer,
                clearColour.value,
                graphics.swapChain.area,
                true
            )

            computeFrame.quad.insideRenderPass(frame, res)
            fps.insideRenderPass(frame, res)

            // Renderpass finalLayout = PRESENT_SRC_KHR
            b.endRenderPass()
            b.end()

            /** Submit render buffer */
            context.vk.queues.get(Queues.GRAPHICS).submit(
                cmdBuffers       = arrayOf(b),
                waitSemaphores   = arrayOf(computeFrame.computeFinished),
                waitStages       = intArrayOf(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT),
                signalSemaphores = arrayOf(res.renderFinished),
                fence            = res.fence
            )
        }
    }
    private fun updateFrame(frame:FrameInfo) {
        val events = ArrayList(graphics.drainWindowEvents())
        events.forEach {
            when(it) {
                is KeyEvent -> {
                    if(it.key == GLFW.GLFW_KEY_ESCAPE) graphics.postCloseMessage()
                }
            }
        }
    }
    private fun createUploadData(num:Long) {
        val screen = graphics.swapChain.extent
        val v  = (num % 256 / 256.0f)
        val tl = Vector2i(10, 10)
        val br = Vector2i(screen.x - 10, screen.y - 10)

        stagingWriteBuffer!!.mapForWriting { buf ->
            val floats = buf.asFloatBuffer()

            for(y in tl.y until br.y) {
                for(x in tl.x until br.x) {
                    val i = (x + y * screen.x) * 3

                    floats.put(i, v)
                    floats.put(i+1, Math.min(1.0f, v+Math.random().toFloat()*0.25f))
                    floats.put(i+2, v)
                }
            }
        }
    }
    private fun createFrameResources() {
        for(r in 0 until graphics.swapChain.numImages) {

            val fr = FrameResource(
                computeBuffer = computeCP.alloc(),
                transferBuffer = transferCP.alloc(),
                computeFinished = device.createSemaphore(),
                transferFinished = device.createSemaphore(),
                targetImage = memory.get(VulkanMemory.DEVICE).allocImage { info->
                    info.imageType(VK_IMAGE_TYPE_2D)
                    info.format(VK_FORMAT_R8G8B8A8_UNORM)
                    info.mipLevels(1)
                    info.usage(VK_IMAGE_USAGE_STORAGE_BIT or VK_IMAGE_USAGE_SAMPLED_BIT)
                    info.extent().set(camera.windowSize.x, camera.windowSize.y, 1)
                }.orThrow()
            )

            fr.quad
                .init(context, fr.targetImage.getView(), sampler!!)
                .camera(camera)
                .model(Matrix4f()
                            .translate(0f,0f,0f)
                            .scale(camera.windowSize.x.toFloat(), camera.windowSize.y.toFloat(), 0f))

            frameResources.add(fr)

            computeDescriptors
                .layout(0).createSet()
                    .add(deviceReadBuffer!!)
                    .add(fr.targetImage.getView(), VK_IMAGE_LAYOUT_GENERAL)
                .write()
        }
    }
    private fun recordComputeFrames() {
        assert(frameResources.size == graphics.frameResources.size)

        val preImageBarriers = VkImageMemoryBarrier.calloc(1)
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .srcAccessMask(0)
            .dstAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(VK_IMAGE_LAYOUT_GENERAL)
            .srcQueueFamilyIndex(vk.queues.getFamily(Queues.GRAPHICS).index)
            .dstQueueFamilyIndex(vk.queues.getFamily(Queues.COMPUTE).index)

        val postImageBarriers = VkImageMemoryBarrier.calloc(1)
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
            .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
            .dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            .oldLayout(VK_IMAGE_LAYOUT_GENERAL)
            .newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            .srcQueueFamilyIndex(vk.queues.getFamily(Queues.COMPUTE).index)
            .dstQueueFamilyIndex(vk.queues.getFamily(Queues.GRAPHICS).index)

        graphics.frameResources.forEach { res ->
            val r = frameResources[res.index]

            val b       = r.computeBuffer
            val ds      = computeDescriptors.layout(0).set(res.index)
            val extent  = graphics.swapChain.extent

            assert(extent.x%8==0 && extent.y%8==0)

            preImageBarriers.image(r.targetImage.handle)
            postImageBarriers.image(r.targetImage.handle)

            b.run {
                begin()
                bindPipeline(computePipeline)

                bindDescriptorSets(
                    pipelineBindPoint = VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipelineLayout    = computePipeline.layout,
                    firstSet          = 0,
                    descriptorSets    = arrayOf(ds),
                    dynamicOffsets    = intArrayOf())

                pipelineBarrier(
                    srcStageMask = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    destStageMask = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    dependencyFlags = 0,
                    memoryBarriers = null,
                    bufferMemoryBarriers = null,
                    imageMemoryBarriers = preImageBarriers
                )

                dispatch(extent.x/8, extent.y/8, 1)

                pipelineBarrier(
                    srcStageMask = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    destStageMask = VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                    dependencyFlags = 0,
                    memoryBarriers = null,
                    bufferMemoryBarriers = null,
                    imageMemoryBarriers = postImageBarriers
                )

                end()
            }
        }
        preImageBarriers.free()
        postImageBarriers.free()
    }
}
