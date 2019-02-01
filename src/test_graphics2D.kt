
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4i
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.api.*
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.d2.*
import vulkan.misc.*
import vulkan.texture.Textures


/**
 * Vulkan graphics example.
 */

fun main(args:Array<String>) {
    log.info("Testing Vulkan Graphics...")

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

    log.info("Finished")
}
private class GraphicsApplication : VulkanClient(
    windowed                = true,
    width                   = 1400,
    height                  = 800,
    windowTitle             = "Vulkan 2D Graphics Test",
    enableVsync             = false,
    prefNumSwapChainBuffers = 2)
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

        vk.graphics.showWindow()
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {

        val b = res.adhocCB
        b.beginOneTimeSubmit()

        beforeRenderPass(frame, res)

        // Renderpass initialLayout = UNDEFINED
        // The renderpass loadOp    = CLEAR
        b.beginRenderPass(
            vk.graphics.renderPass,
            res.frameBuffer,
            clearColour,
            Vector4i(0,0, vk.graphics.windowSize.x, vk.graphics.windowSize.y),
            true
        )

        insideRenderPass(frame, res)

        // Renderpass finalLayout = PRESENT_SRC_KHR
        b.endRenderPass()
        b.end()

        /** Submit render buffer */
        vk.queues.get(Queues.GRAPHICS).submit(
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
    private lateinit var vk: VulkanApplication
    private lateinit var device: VkDevice
    private val memory          = VulkanMemory()
    private val buffers         = VulkanBuffers()
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

        vk.graphics.setCallback(object : GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetWindowShouldClose(window, true)
                }
            }
        })

        camera.resizeWindow(vk.graphics.windowSize)

        memory.init(vk)
            .createOnDevice(VulkanMemory.DEVICE, 256.megabytes())
            .createShared(VulkanMemory.SHARED, 1.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 64.megabytes())
        log.info("$memory")

        buffers.init(vk)
            .createVertexBuffer(VulkanBuffers.VERTEX, memory.get(VulkanMemory.DEVICE), 64.megabytes())
            .createIndexBuffer(VulkanBuffers.INDEX, memory.get(VulkanMemory.DEVICE), 64.megabytes())
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 64.megabytes())
            .createUniformBuffer(VulkanBuffers.UNIFORM, memory.get(VulkanMemory.SHARED), 1.megabytes())
        log.info("$buffers")

        textures.init(vk, 16.megabytes())

        createSamplers()

        val context = RenderContext(
            vk,
            device,
            vk.graphics.renderPass
        )

        quads[0]
            .init(context, buffers, textures.get("cat.dds").image.getView(), sampler!!)
            .camera(camera)
            .model(Matrix4f()
                    .translate(515f,10f,0f)
                    .scale(100f, 100f, 0f))
        quads[1]
            .init(context, buffers, textures.get("dog.dds").image.getView(), sampler!!)
            .camera(camera)
            .model(Matrix4f()
                .translate(10f,10f,0f)
                .scale(100f, 100f, 0f))

        rectangles
            .init(context, buffers, 1000)
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
            .init(context, buffers, 100)
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

        text.init(context, buffers, vk.graphics.fonts.get("segoeprint"), 10000, true)
            .camera(camera)
            .setSize(16f)
            .setColour(WHITE*1.1f)
            .setDropShadowColour(RGBA(0f, 0f, 0f, 0.8f))
            .setDropShadowOffset(Vector2f(-0.0025f, 0.0025f))

        (0..18).forEach { i->
            text.setColour(RGBA(i/19.0f,0.5f+i/40.0f,1f,1f)*1.1f)
            text.appendText("Hello there I am some text...", 10, 110+i*20)
        }

        fps.init(context, buffers)
           .camera(camera)
    }
    private fun createSamplers() {
        sampler = device.createSampler { info-> }
    }
}