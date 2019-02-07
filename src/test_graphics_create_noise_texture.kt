
import org.joml.Matrix4f
import org.joml.Vector4i
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.vulkan.VK10
import org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM
import org.lwjgl.vulkan.VkClearValue
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.api.*
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.d2.Camera2D
import vulkan.d2.FPS
import vulkan.d2.Quad
import vulkan.maths.ImprovedNoise
import vulkan.misc.megabytes
import vulkan.texture.RawImageData
import vulkan.texture.Textures
import java.nio.ByteBuffer

fun main(args:Array<String>) {

    val client = TestCreateNoiseTexture()
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
}
private class TestCreateNoiseTexture : VulkanClient(
    windowed                = true,
    width                   = 1000,
    height                  = 800,
    windowTitle             = "Create and display noise texture",
    enableVsync             = false,
    prefNumSwapChainBuffers = 2)
{
    private lateinit var vk     : VulkanApplication
    private lateinit var device : VkDevice

    private val memory          = VulkanMemory()
    private val buffers         = VulkanBuffers()
    private val textures        = Textures("Noise")
    private val camera          = Camera2D()
    private val quad            = Quad()
    private val fps             = FPS()
    private val clearColour     = VkClearValue.calloc(1).color {
        it.float32(0, 0.0f)
          .float32(1, 0.2f)
          .float32(2, 0.0f)
          .float32(3, 1.0f)
    }
    private var sampler: VkSampler? = null

    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {

    }
    override fun destroy() {
        device?.let {
            device.waitForIdle()

            clearColour.free()

            sampler?.destroy()

            quad.destroy()
            fps.destroy()

            textures.destroy()
            buffers.destroy()
            memory.destroy()
        }
    }
    override fun vulkanReady(vk: VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        vk.graphics.setCallback(object :GLFWKeyCallback() {
            override fun invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
                if(key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                    GLFW.glfwSetWindowShouldClose(window, true)
                }
            }
        })

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

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

        textures.init(vk, 32.megabytes())

        val context = RenderContext(
            vk,
            device,
            vk.graphics.renderPass,
            buffers
        )

        fps.init(context)
            .camera(camera)

        sampler = device.createSampler { info-> }

        createNoiseTexture()

        quad.init(context, textures.get("GeneratedNoise").image.getView(), sampler!!)
            .camera(camera)
            .model(
                Matrix4f()
                    .translate(10f,10f,0f)
                    .scale(512f, 512f, 0f))

        vk.graphics.showWindow()
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {
        res.adhocCB.let { b->
            b.beginOneTimeSubmit()

            quad.beforeRenderPass(frame, res)
            fps.beforeRenderPass(frame, res)

            // Renderpass initialLayout = UNDEFINED
            // The renderpass loadOp    = CLEAR
            b.beginRenderPass(
                vk.graphics.renderPass,
                res.frameBuffer,
                clearColour,
                Vector4i(0, 0, vk.graphics.windowSize.x, vk.graphics.windowSize.y),
                true
            )

            quad.insideRenderPass(frame, res)
            fps.insideRenderPass(frame, res)

            // Renderpass finalLayout = PRESENT_SRC_KHR
            b.endRenderPass()
            b.end()
            /** Submit render buffer */
            vk.queues.get(Queues.GRAPHICS).submit(
                arrayOf(b),
                arrayOf(res.imageAvailable),                                // wait semaphores
                intArrayOf(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),  // wait stages
                arrayOf(res.renderFinished),    // signal semaphores
                res.fence                       // fence
            )
        }
    }
    private fun createNoiseTexture() {

        val noise = ImprovedNoise()
        val array = noise.noiseArray2D(512, 200.0, 5, 0.5)

        val data = RawImageData(
            name     = "GeneratedNoise",
            channels = 1,
            size     = array.size,
            data     = ByteBuffer.wrap(array),
            format   = VK_FORMAT_R8_UNORM,
            levels   = 1,
            width    = 512,
            height   = 512
        )

        textures.create(data.name, data)
    }
}