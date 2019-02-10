package vulkan.app

import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.*
import org.lwjgl.glfw.GLFW.GLFW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_HIDDEN
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL
import org.lwjgl.glfw.GLFW.glfwSetInputMode
import org.lwjgl.system.Callback
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import vulkan.api.*
import vulkan.common.*
import vulkan.font.Fonts
import vulkan.misc.*

class GraphicsComponent(val client: VulkanClient) {
    private val glfwCallbacks           = ArrayList<Callback>()
    private var surface:VkSurfaceKHR    = VK_NULL_HANDLE
    private var window:Long             = VK_NULL_HANDLE
    private var frameResourceIndex:Long = 0
    private val frameResources          = ArrayList<PerFrameResource>()
    private val windowEvents            = ArrayList<WindowEvent>()

    private lateinit var vk:VulkanApplication
    private lateinit var device: VkDevice
    private lateinit var commandPool: VkCommandPool

    val surfaceFormat = SurfaceFormat()
    val animations    = Animations
    val fonts         = Fonts
    var isInitialised = false

    lateinit var swapChain: SwapChain
    lateinit var renderPass: VkRenderPass

    fun init(vk:VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        createWindow()
        createSurface()
        selectSurfaceFormat()
        createRenderPass()
        createSwapChain()
        createCommandPool()
        createPerFrameResources()
        addEventCallbacks()

        fonts.init(vk)

        this.isInitialised  = true
    }
    fun destroy() {
        if(!isInitialised) return

        glfwCallbacks.forEach { it.free() }
        frameResources.forEach { pfr ->
            pfr.imageAvailable.destroy()
            pfr.renderFinished.destroy()
            pfr.fence.destroy()
        }
        swapChain.destroy()
        commandPool.destroy()
        renderPass.destroy()
        fonts.destroy()
        if(surface!=VK_NULL_HANDLE) vkDestroySurfaceKHR(vk.instance, surface, null)
        GLFW.glfwDestroyWindow(window)
    }
    fun postCloseMessage() {
        GLFW.glfwSetWindowShouldClose(window, true)
    }

    val windowSize: Vector2i
        get() {
            val w = BufferUtils.createIntBuffer(1)
            val h = BufferUtils.createIntBuffer(1)
            GLFW.glfwGetWindowSize(window, w, h)
            return Vector2i(w.get(0), h.get(0))
        }

    var fps:Double = 0.0
        private set(value) { field = value }

    // Setter properties
    var windowTitle:String = client.windowTitle
        set(value) { field = value; GLFW.glfwSetWindowTitle(window, field) }

    fun showWindow(flag:Boolean=true) {
        when(flag) {
            true -> GLFW.glfwShowWindow(window)
            else -> GLFW.glfwHideWindow(window)
        }
    }
    /** Return all window events and clear the queue. */
    fun drainWindowEvents() : List<WindowEvent> {
        val list = windowEvents.toList()
        windowEvents.clear()
        return list
    }
    fun peekAtWindowEvents(type:WindowEvent) : List<WindowEvent> {
        return windowEvents.filter { it.javaClass == type.javaClass }
    }
    fun setMouseCursorVisible(visible : Boolean) {
        glfwSetInputMode(window, GLFW_CURSOR, if(visible) GLFW_CURSOR_NORMAL else GLFW_CURSOR_HIDDEN)
    }
    /**
     * This will capture the mouse and make it hidden.
     * The application will need to display its own cursor.
     */
    fun captureMouse() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
    }

    fun enterLoop() {
        log.info("Entering mainLoop")
        var frameNumber           = 0L
        var seconds               = 0L
        var tenSeconds            = 10L
        var previousTimestamp     = System.nanoTime()
        val frameInfo             = FrameInfo(0, 0.0, 1.0)

        while(!GLFW.glfwWindowShouldClose(window)) {

            renderFrame(frameInfo)

            GLFW.glfwPollEvents()

            val timestamp  = System.nanoTime()
            val frameNsecs = timestamp - previousTimestamp
            val fps        = BILLION.toDouble() / frameNsecs.toDouble()
            previousTimestamp = timestamp

            frameInfo.number++
            frameInfo.seconds   += (frameNsecs.toDouble()/BILLION)
            frameInfo.perSecond = 1.0 / fps

            frameNumber = frameInfo.number

            //frameTiming.endFrame(frameNsecs);

            if(frameInfo.seconds.toLong() > seconds) {
                seconds = frameInfo.seconds.toLong()

                this.fps = fps
                //val avg = frameTiming.average(2);
                //currentFPS = 1000.0 / avg;

                log.info(String.format("Frame: %d sec: %.2f persec=%.4f ms:%.3f fps:%.2f",
                    frameInfo.number,
                    frameInfo.seconds,
                    frameInfo.perSecond,
                    frameNsecs/MILLION.toDouble(),
                    fps))
            }
            if(frameInfo.seconds.toLong() >= tenSeconds) {
                tenSeconds = frameInfo.seconds.toLong() + 10

                val totalMem = Runtime.getRuntime().totalMemory()
                val freeMem  = Runtime.getRuntime().freeMemory()
                log.info("VM memory usage: Used ${(totalMem-freeMem)/ MEGABYTE} / ${totalMem/ MEGABYTE} MB")
            }
        }
        log.info("Exiting mainLoop on frame $frameNumber")
    }
    //=======================================================================================
    private fun renderFrame(frame: FrameInfo) {

        /** Select the current frame resource */
        val mod      = (frameResourceIndex%frameResources.size).toInt()
        val resource = frameResources[mod]
        frameResourceIndex++

        /** Wait for the fence to be signalled */
        resource.fence.waitFor()
        resource.fence.reset()

        /** Get the next available image view */
        val index = swapChain.acquireNext(resource.imageAvailable)

        /** Update any animations */
        animations.update(frame.perSecond)

        /** Let the app do its thing */
        client.render(frame, resource)

        /** Present */
        swapChain.queuePresent(
            queue          = vk.queues.get(Queues.GRAPHICS),
            imageIndex     = index,
            waitSemaphores = arrayOf(resource.renderFinished))
    }
    private fun createWindow() {
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API)
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)

        var width       = client.width
        var height      = client.height
        val resizable   = false
        val decorated   = true
        val autoIconify = false
        var monitor = GLFW.glfwGetPrimaryMonitor()
        val vidmode = GLFW.glfwGetVideoMode(monitor)!!

        if(client.windowed) {
            monitor = 0
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, if(resizable) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)
            GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, if(decorated) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)

        } else {
            // for the moment always use the desktop resolution
            width  = vidmode.width()
            height = vidmode.height()
        }

        GLFW.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, if(autoIconify) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)

        window = GLFW.glfwCreateWindow(
            width,
            height,
            windowTitle,
            monitor,
            MemoryUtil.NULL
        ).let {
            if(it==0L) throw RuntimeException("Failed to create the GLFW window")
            it
        }

        GLFW.glfwSetWindowPos(
            window,
            (vidmode.width() - width) / 2,
            (vidmode.height() - height) / 2
        )

        log.info("Video mode .... (${vidmode.width()} x ${vidmode.height()} ${vidmode.refreshRate()} Hz")
        log.info("Window size ... ${windowSize.string()}")
    }
    private fun createSurface() {
        MemoryStack.stackPush().use { stack ->
            val pSurface = stack.mallocLong(1)
            GLFWVulkan.glfwCreateWindowSurface(vk.instance, window, null, pSurface).check()
            surface = pSurface.get(0)
        }
        if(!canPresent(vk.physicalDevice, surface, vk.queues.getFamily(Queues.GRAPHICS))) {
            throw Error("Can't present on this surface")
        }
    }
    private fun createRenderPass() {
        renderPass = client.createRenderPass(device, surfaceFormat.colorFormat)
    }
    private fun createSwapChain() {
        val w = windowSize
        swapChain = SwapChain(vk, w.x, w.y, surface, surfaceFormat, renderPass)
    }
    private fun createCommandPool() {
        commandPool = device.createCommandPool(
            vk.queues.getFamily(Queues.GRAPHICS),
            VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT or VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
        )
        log.info("Created graphics command pool using queue family ${vk.queues.getFamily(Queues.GRAPHICS)}")
    }
    private fun createPerFrameResources() {
        assert(swapChain.frameBuffers.isNotEmpty()) { "Swapchain framebuffers not yet initialised" }

        (0 until swapChain.numImages).forEach { i ->
            val r = PerFrameResource(
                index          = i,
                image          = swapChain.images[i],
                imageView      = swapChain.views[i],
                frameBuffer    = swapChain.frameBuffers[i],
                adhocCB        = commandPool.alloc(),
                fence          = device.createFence(signalled = true),
                imageAvailable = device.createSemaphore(),
                renderFinished = device.createSemaphore()
            )
            frameResources.add(r)
        }
        log.info("Created ${frameResources.size} per frame resources")
    }
    private fun selectSurfaceFormat() {
        MemoryStack.stackPush().use { stack ->

            val formats = getFormats(stack, vk.physicalDevice, surface)
            assert(formats.count()>0)

            var colorSpace: VkColorSpaceKHR
            var colorFormat: VkFormat
            val desiredFormat     = VK_FORMAT_B8G8R8A8_UNORM
            val desiredColorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR

            colorFormat = desiredFormat
            colorSpace  = desiredColorSpace

            // If the format list includes just one entry of VK_FORMAT_UNDEFINED,
            // the surface has no preferred format. Otherwise, at least one
            // supported format will be returned
            if(formats.count() == 1 && formats[0].format() == VK_FORMAT_UNDEFINED) {
                colorFormat = VK_FORMAT_B8G8R8A8_UNORM
                colorSpace  = formats[0].colorSpace()
            } else {
                colorFormat = formats[0].format()
                colorSpace  = formats[0].colorSpace()

                formats.forEach {
                    if(it.format()==desiredFormat && it.colorSpace()==desiredColorSpace) {
                        colorFormat = it.format()
                        colorSpace  = it.colorSpace()
                    }
                }
            }
            surfaceFormat.colorFormat = colorFormat
            surfaceFormat.colorSpace  = colorSpace
        }
    }
    private fun addEventCallbacks() {

        val keyCallback = object : GLFWKeyCallback() {
            override fun invoke(window : Long, key : Int, scancode : Int, action : Int, mods : Int) {
                windowEvents.add(KeyEvent(key, scancode, KeyAction(action), KeyMods(mods)))
            }
        }
        val mouseButtonCallback = object : GLFWMouseButtonCallback() {
            override fun invoke(window : Long, button : Int, action : Int, mods : Int) {
                windowEvents.add(MouseButtonEvent(button, KeyAction(action), KeyMods(mods)))
            }
        }
        val mousePosCallback = object : GLFWCursorPosCallback() {
            override fun invoke(window : Long, xpos : Double, ypos : Double) {
                windowEvents.add(MouseMoveEvent(xpos.toFloat(), ypos.toFloat()))
            }
        }
        val mouseScrollCallback = object : GLFWScrollCallback() {
            override fun invoke(window : Long, xoffset : Double, yoffset : Double) {
                windowEvents.add(MouseWheelEvent(xoffset.toFloat(), yoffset.toFloat()))
            }
        }

        glfwCallbacks.add(keyCallback)
        glfwCallbacks.add(mouseButtonCallback)
        glfwCallbacks.add(mousePosCallback)
        glfwCallbacks.add(mouseScrollCallback)

        GLFW.glfwSetKeyCallback(window, keyCallback)
        GLFW.glfwSetMouseButtonCallback(window, mouseButtonCallback)
        GLFW.glfwSetCursorPosCallback(window, mousePosCallback)
        GLFW.glfwSetScrollCallback(window, mouseScrollCallback)
    }
}
