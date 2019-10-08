
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT
import org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.api.*
import vulkan.app.*
import vulkan.common.*
import vulkan.d2.Camera2D
import vulkan.d2.FPS
import vulkan.d3.Camera3D
import vulkan.d3.Model3D
import vulkan.maths.plusAssign
import vulkan.misc.RGBA
import vulkan.misc.VkFormat
import vulkan.misc.megabytes

/**
 * Vulkan 3D graphics example.
 */

fun main(args:Array<String>) {
    log.info("Testing Vulkan 3D Graphics...")

    val client = Graphics3DApplication()
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
private class Graphics3DApplication : VulkanClient(Parameters(
    windowed                = true,
    width                   = 800,
    height                  = 800,
    windowTitle             = "Vulkan 3D Graphics Test",
    enableVsync             = false,
    prefNumSwapChainBuffers = 3,
    depthStencilFormat      = VK_FORMAT_D32_SFLOAT)) // VK_FORMAT_D32_SFLOAT_S8_UINT / VK_FORMAT_D32_SFLOAT
{
    private lateinit var vk : VulkanApplication
    private lateinit var device : VkDevice

    private val memory      = VulkanMemory()
    private val buffers     = VulkanBuffers()
    private val camera2d    = Camera2D()
    private val camera3d    = Camera3D()
    private val fps         = FPS()
    private val clearColour = ClearColour(RGBA(0f, 0f, 0f, 1f), true)
    private val obj         = Model3D()
    private val rotation    = Vector3f()

    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {
        f.geometryShader(true)
    }

    override fun createRenderPass(device : VkDevice, surfaceFormat : VkFormat) : VkRenderPass {
        return device.createColorAndDepthRenderPass(surfaceFormat, VK_FORMAT_D32_SFLOAT)
    }
    override fun destroy() {
        log.info("Destroying Client")
        device.let {
            device.waitForIdle()

            clearColour.destroy()

            fps.destroy()

            obj.destroy()
            buffers.destroy()
            memory.destroy()
        }
    }
    override fun vulkanReady(vk : VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        initialise()

        vk.graphics.showWindow()
    }
    override fun render(frame: FrameInfo, res: PerFrameResource) {
        update(frame)

        res.cmd.let { b->
            b.beginOneTimeSubmit()

            obj.beforeRenderPass(frame, res)
            fps.beforeRenderPass(frame, res)
            // Renderpass initialLayout = UNDEFINED
            //                   loadOp = CLEAR
            b.beginRenderPass(
                vk.graphics.renderPass,
                res.frameBuffer,
                clearColour.value,
                vk.graphics.swapChain.area,
                true
            )

            obj.insideRenderPass(frame, res)
            fps.insideRenderPass(frame, res)

            // Renderpass finalLayout = PRESENT_SRC_KHR
            b.endRenderPass()
            b.end()

            /** Submit render buffer */
            vk.queues.get(Queues.GRAPHICS).submit(
                cmdBuffers       = arrayOf(b),
                waitSemaphores   = arrayOf(res.imageAvailable),
                waitStages       = intArrayOf(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT),
                signalSemaphores = arrayOf(res.renderFinished),
                fence            = res.fence
            )
        }
    }
    private fun update(frame:FrameInfo) {
        var cameraMoved = false


        val add = (1.0 * frame.perSecond).toFloat()
        rotation += add
        obj.rotation(rotation)

        vk.graphics.drainWindowEvents().forEach {
            when(it) {
                is KeyEvent        -> {
                    if(it.key == GLFW.GLFW_KEY_ESCAPE) vk.graphics.postCloseMessage()
                }
                is MouseDragStart  -> {
//                    println("Start drag: ${it.pos.string()} button:${it.button}")
//                    System.out.flush()
                }
                is MouseDrag       -> {
//                    println("drag: ${it.delta.string()} button:${it.button}")
//                    System.out.flush()
                }
                is MouseDragEnd    -> {
//                    println("End drag: ${it.delta.string()} button:${it.button}")
//                    System.out.flush()
                }
                is MouseWheelEvent -> {
                    val zoom = it.yDelta

                    camera3d.moveForward((zoom * frame.perSecond * 10000).toFloat())
//                    when {
//                        zoom > 0f -> camera.zoomIn(  zoom * frame.perSecond.toFloat() * 200, 0.1f)
//                        zoom < 0f -> camera.zoomOut(-zoom * frame.perSecond.toFloat() * 200, 1.5f)
//                    }
                    cameraMoved = true
                }
            }
        }
        if(cameraMoved) {
            obj.camera(camera3d)
            log.debug("camera3d = $camera3d")
        }
    }
    private fun initialise() {
        log.info("Initialising client")

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

        camera2d.resizeWindow(vk.graphics.windowSize)

        camera3d.run {
            init(position = Vector3f(0f, 0f, 120f), focalPoint = Vector3f(0f,0f,0f))
            fovNearFar(Math.toRadians(70.0).toFloat(), 0.01f, 1000.0f)
            resizeWindow(vk.graphics.windowSize)
            log.debug("camera3d = $this")
        }

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

        val context = RenderContext(
            vk,
            device,
            vk.graphics.renderPass,
            buffers
        )

        fps.init(context)
            .camera(camera2d)

        obj.run {
            init(context, "models/suzanne.obj.txt")
            camera(camera3d)
            lightPos(Vector3f(200f,1000f,800f))
            scale(50f)
        }
    }
}
