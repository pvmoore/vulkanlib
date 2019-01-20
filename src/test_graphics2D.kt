
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4i
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.VulkanApplication
import vulkan.api.*
import vulkan.api.buffer.VkBuffer
import vulkan.api.memory.*
import vulkan.common.*
import vulkan.d2.*
import vulkan.misc.*
import vulkan.texture.Textures


/**
 * Vulkan graphics example.
 */

fun main(args: Array<String>) {

    log.info("Testing Vulkan Graphics...")

    test()

    log.info("Finished")
}
private fun test() {
    val client = GraphicsApplication()
    var app: VulkanApplication? = null

    fun initialise() {
        app = VulkanApplication(client)
        app?.initialise()
    }
    fun destroy() {
        client.destroy()
        app?.destroy()
    }
    fun enterMainLoop() {
        app!!.mainLoop()
    }

    try{
        initialise()
        enterMainLoop()
    }catch(e:Throwable) {
        e.printStackTrace()
    }finally {
        destroy()
    }
}
private class GraphicsApplication : VulkanClient(
    windowed                = true,
    width                   = 1400,
    height                  = 800,
    windowTitle             = "Vulkan 2D Graphics Test",
    enableVsync             = false,
    prefNumSwapChainBuffers = 2,
    prefNumComputeQueues    = 0)
{
    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {
        f.geometryShader(true)
    }
    override fun destroy() {
        log.info("Destroying Client")
        device?.let {
            device.waitForIdle()

            clearColour.free()

            sampler?.destroy()

            quads.forEach { q->q.destroy()}
            rectangles.destroy()
            roundRectangles.destroy()
            text.destroy()
            fps.destroy()

            buffers.destroy()
            memory.destroy()
            textures.destroy()
        }
    }
    override fun vulkanReady(vk: VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        initialise()

        vk.showWindow()
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {

        val b = res.adhocCB
        b.beginOneTimeSubmit()

        beforeRenderPass(frame, res)

        // Renderpass initialLayout = UNDEFINED
        // The renderpass loadOp    = CLEAR
        b.beginRenderPass(
            vk.renderPass,
            res.frameBuffer,
            clearColour,
            Vector4i(0,0, vk.windowSize.x, vk.windowSize.y),
            true
        )

        insideRenderPass(frame, res)

        // Renderpass finalLayout = PRESENT_SRC_KHR
        b.endRenderPass()
        b.end()

        /** Submit render buffer */
        vk.graphicsQueues[0].submit(
            arrayOf(b),
            arrayOf(res.imageAvailable),                                // wait semaphores
            intArrayOf(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),  // wait stages
            arrayOf(res.renderFinished),    // signal semaphores
            res.fence                       // fence
        )
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
    private lateinit var vk:VulkanApplication
    private lateinit var device: VkDevice
    private val memory          = Memory()
    private val buffers         = Buffers()
    private val camera          = Camera2D()
    private val quads           = arrayOf(Quad(), Quad())
    private val rectangles      = Rectangles()
    private val roundRectangles = RoundRectangles()
    private val text            = Text()
    private val fps             = FPS()
    private val textures        = Textures()
    private val clearColour     = VkClearValue.calloc(1).color {
        it.float32(0, 0.2f)
          .float32(1, 0.0f)
          .float32(2, 0.0f)
          .float32(3, 1.0f)
    }

    private var sampler:VkSampler? = null

    private val uploader by lazy { StagingTransfer(vk.transferCP, vk.transferQueues.first(), buffers.stagingBuffer) }


    private fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        quads.forEach { q-> q.beforeRenderPass(frame, res) }
        rectangles.beforeRenderPass(frame, res)
        roundRectangles.beforeRenderPass(frame, res)
        text.beforeRenderPass(frame, res)
        fps.beforeRenderPass(frame, res)
    }
    private fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        quads.forEach { q-> q.insideRenderPass(frame, res) }
        rectangles.insideRenderPass(frame, res)
        roundRectangles.insideRenderPass(frame, res)
        text.insideRenderPass(frame, res)
        fps.insideRenderPass(frame, res)
    }
    private fun initialise() {
        log.info("Initialising client")

        vk.setCallback(object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetWindowShouldClose(window, true)
                }
            }
        })

        camera.resizeWindow(vk.windowSize)

        memory.init()
        buffers.init()
        textures.init(vk, 16.megabytes())

        createSamplers()

        val context = RenderContext(
            vk,
            device,
            vk.renderPass
        )

        val renderBuffers = RenderBuffers(
            staging  = buffers.stagingBuffer,
            readback = buffers.stagingBuffer,
            vertex   = buffers.vertexBuffer,
            index    = buffers.indexBuffer,
            uniform  = buffers.uniformBuffer
        )

        quads[0]
            .init(context, renderBuffers, textures.get("cat.dds").image.getView(), sampler!!)
            .camera(camera)
            .model(Matrix4f()
                    .translate(515f,10f,0f)
                    .scale(100f, 100f, 0f))
        quads[1]
            .init(context, renderBuffers, textures.get("dog.dds").image.getView(), sampler!!)
            .camera(camera)
            .model(Matrix4f()
                .translate(10f,10f,0f)
                .scale(100f, 100f, 0f))

        rectangles
            .init(context, renderBuffers, 1000)
            .camera(camera)
            .setColour(WHITE)
            .addRectangle(
                Vector2f(300f, 200f),
                Vector2f(400f, 200f),
                Vector2f(400f, 300f),
                Vector2f(300f, 300f))
            .setColour(YELLOW)
            .addRectangle(
                Vector2f(450f, 200f),
                Vector2f(550f, 250f),
                Vector2f(480f, 300f),
                Vector2f(420f, 230f),
                WHITE, BLUE, RED, GREEN
            )

        val orange = RGBA(0.7f, 0.4f, 0.1f, 1f) * 0.75f
        val black  = RGBA(0f, 0f, 0f, 0f)

        roundRectangles
            .init(context, renderBuffers, 100)
            .camera(camera)
            .setColour(RGBA(0.3f, 0.5f, 0.7f, 1f))
            .addRectangle(Vector2f(650f,350f), Vector2f(150f,100f), 7f)
            .addRectangle(Vector2f(650f,200f), Vector2f(150f,100f),
                orange,orange*3f,
                orange,orange*3f,
                30f)
            .addRectangle(Vector2f(820f,200f), Vector2f(150f,100f),
                orange*3f,orange*3f,
                orange,orange,
                30f)
            // capsule
            .addRectangle(Vector2f(1000f,220f), Vector2f(150f,60f),
                WHITE,WHITE,
                black,black,
                30f)
            .addRectangle(Vector2f(1000f,220f), Vector2f(150f,60f),
                black,black,
                WHITE,WHITE,
                30f)
            // white border
            .addRectangle(Vector2f(1170f,200f), Vector2f(150f,100f),
                WHITE*0.8f, WHITE,
                WHITE*0.8f, black+0.5f,
                32f)
            .addRectangle(Vector2f(1175f,204f), Vector2f(140f,92f),
                orange, orange,
                orange,orange,
                30f)

        text.init(context, renderBuffers, vk.fonts.get("segoeprint"), 10000, true)
            .camera(camera)
            .setSize(16f)
            .setColour(WHITE*1.1f)
            .setDropShadowColour(RGBA(0f, 0f, 0f, 0.8f))
            .setDropShadowOffset(Vector2f(-0.0025f, 0.0025f))

        (0..18).forEach { i->
            text.setColour(RGBA(i/19.0f,0.5f+i/40.0f,1f,1f)*1.1f)
            text.appendText("Hello there I am some text...", 10, 110+i*20)
        }

        fps.init(context, renderBuffers)
           .camera(camera)




    }
    private fun createSamplers() {
        sampler = device.createSampler { info-> }
    }
    //============================================================================================
    private inner class Memory {
        private var isInitialised = false

        lateinit var deviceBufferMem:VkDeviceMemory
        lateinit var deviceImageMem:VkDeviceMemory
        lateinit var deviceUBOMem:VkDeviceMemory
        lateinit var stagingMem:VkDeviceMemory

        fun init() {
            log.info("initialise")
            deviceBufferMem = vk.allocateMemory(
                size = 256.megabytes(),
                type = vk.selectDeviceMemoryType(256.megabytes())!!)
            deviceImageMem = vk.allocateMemory(
                size = 256.megabytes(),
                type = vk.selectDeviceMemoryType(256.megabytes())!!)
            stagingMem = vk.allocateMemory(
                size = 64.megabytes(),
                type = vk.selectStagingUploadMemoryType(64.megabytes())!!
            )

            /** Try for shared device and host (AMD only) otherwise just use standard device only */
            val sharedType = vk.selectSharedMemoryType(1.megabytes()) ?:
            vk.selectDeviceMemoryType(1.megabytes())

            deviceUBOMem = vk.allocateMemory(
                size = 1.megabytes(),
                type = sharedType!!
            )

            isInitialised = true
            log.info("Memory {\n$this}")
        }
        fun destroy() {
            isInitialised.ifTrue {
                deviceBufferMem.free()
                deviceImageMem.free()
                deviceUBOMem.free()
                stagingMem.free()
            }
        }

        override fun toString(): String {
            val buf = StringBuilder()
            buf.append("\tDevice  : ").append(deviceBufferMem).append("\n")
            buf.append("\tTexture : ").append(deviceImageMem).append("\n")
            buf.append("\tUBO     : ").append(deviceUBOMem).append("\n")
            buf.append("\tStaging : ").append(stagingMem).append("\n")
            return buf.toString()
        }
    }
    private inner class Buffers {
        private var isInitialised = false

        lateinit var stagingBuffer: VkBuffer
        lateinit var vertexBuffer: VkBuffer
        lateinit var indexBuffer: VkBuffer
        lateinit var uniformBuffer: VkBuffer

        fun init() {
            memory.stagingMem.allocBuffer(
                size      = 64.megabytes(),
                overrides = { info -> info.usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT) },
                onError   = { reason -> throw Error("Unable to allocate staging buffer: $reason") },
                onSuccess = { b ->  stagingBuffer = b }
            )
            memory.deviceBufferMem.allocBuffer(
                size      = 64.megabytes(),
                overrides = { info -> info.usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT) },
                onError   = { reason -> throw Error("Unable to allocate vertex buffer: $reason") },
                onSuccess = { b ->
                    vertexBuffer      = b
                }
            )
            memory.deviceUBOMem.allocBuffer(
                size      = 1.megabytes(),
                overrides = { info -> info.usage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT) },
                onError   = { reason -> throw Error("Unable to allocate uniform buffer: $reason") },
                onSuccess = { b ->
                    uniformBuffer      = b
                }
            )
            memory.deviceBufferMem.allocBuffer(
                size      = 64.megabytes(),
                overrides = { info -> info.usage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT) },
                onError   = { reason -> throw Error("Unable to allocate index buffer: $reason") },
                onSuccess = { b ->
                    indexBuffer      = b
                }
            )
            isInitialised = true
            log.info("Buffers {\n$this}")
        }
        fun destroy() {
            isInitialised.ifTrue {
                stagingBuffer.destroy()
                vertexBuffer.destroy()
                indexBuffer.destroy()
                uniformBuffer.destroy()
            }
        }
        override fun toString(): String {
            val buf = StringBuilder()
            buf.append("\tVertex        : ").append(vertexBuffer).append("\n")
            buf.append("\tIndex         : ").append(indexBuffer).append("\n")
            buf.append("\tUBO           : ").append(uniformBuffer).append("\n")
            buf.append("\tStaging       : ").append(stagingBuffer).append("\n")
            return buf.toString()
        }
    }
}