package vulkan

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWKeyCallback
import org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.Callback
import org.lwjgl.system.Configuration
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT
import org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.*
import vulkan.api.*
import vulkan.common.*
import vulkan.font.Fonts
import vulkan.misc.VkSurfaceKHR
import vulkan.misc.check
import vulkan.misc.dump
import vulkan.misc.string

class VulkanApplication(val client: VulkanClient) {
    private val deviceExtensions                        = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
    private val glfwCallbacks                           = mutableListOf<Callback>()
    private var debugCallbackHandle:Long                = VK_NULL_HANDLE
    private var debugCallback:VkDebugReportCallbackEXT? = null
    //===================================================================================

    /** Getter properties */
    lateinit var instance:VkInstance
    lateinit var physicalDevice:VkPhysicalDevice
    lateinit var physicalDeviceProperties:VkPhysicalDeviceProperties
    lateinit var physicalDeviceMemoryProperties:VkPhysicalDeviceMemoryProperties
    lateinit var device:VkDevice
    lateinit var swapChain: SwapChain
    lateinit var graphicsQueues:Array<VkQueue>
    lateinit var transferQueues:Array<VkQueue>
    lateinit var computeQueues:Array<VkQueue>
    lateinit var renderPass:VkRenderPass
    lateinit var frameResources:List<PerFrameResource>
    lateinit var graphicsCP: VkCommandPool
    lateinit var transferCP: VkCommandPool

    var surface:VkSurfaceKHR    = VK_NULL_HANDLE
    var window:Long             = VK_NULL_HANDLE
    var frameResourceIndex:Long = 0

    val features            = VkPhysicalDeviceFeatures.calloc()
    val requestedWindowSize = Vector2i(client.width, client.height)
    val queueFamilies       = QueueFamilies()
    val fonts               = Fonts
    val animations          = Animations
    val shaders             = Shaders
    val surfaceFormat       = SurfaceFormat()

    val windowSize:Vector2i get() {
        val w = BufferUtils.createIntBuffer(1)
        val h = BufferUtils.createIntBuffer(1)
        glfwGetWindowSize(window, w, h)
        return Vector2i(w.get(0), h.get(0))
    }
    var fps:Double = 0.0
            private set(value) { field = value }

    // Setter properties
    var windowTitle:String = client.windowTitle
        set(value) { field = value; glfwSetWindowTitle(window, field) }

    fun showWindow(flag:Boolean=true) {
        when(flag) {
            true -> glfwShowWindow(window)
            else -> glfwHideWindow(window)
        }
    }
    fun setCallback(callback: GLFWKeyCallback) {
        glfwCallbacks.add(callback)
        glfwSetKeyCallback(window, callback)
    }

    fun initialise() {
        if(DEBUG) {
            println("Mode .......... DEBUG")
            //Configuration.DEBUG.set(true)
            Configuration.DEBUG_LOADER.set(true)
            Configuration.DEBUG_STREAM.set(true)
            Configuration.DEBUG_STACK.set(true)
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(true)
            Configuration.DEBUG_MEMORY_ALLOCATOR_INTERNAL.set(true)
            Configuration.DISABLE_CHECKS.set(false)
        } else {
            println("Mode .......... RELEASE")
            Configuration.DEBUG.set(false)
            Configuration.DEBUG_LOADER.set(false)
            Configuration.DEBUG_STREAM.set(false)
            Configuration.DEBUG_STACK.set(false)
            Configuration.DEBUG_MEMORY_ALLOCATOR.set(false)
            Configuration.DEBUG_MEMORY_ALLOCATOR_INTERNAL.set(false)
            Configuration.DISABLE_CHECKS.set(true)

            Logger.getLogger("Global").level  = Level.WARN
            Logger.getLogger("Shaders").level = Level.WARN
            Logger.getRootLogger().level      = Level.WARN
        }

        /** Ensure all logs are flushed when program exits */
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() { LogManager.shutdown() }}
        )

        log.info("LWJGL ......... " + Version.getVersion())
        log.info("GLFW .......... " + glfwGetVersionString())

        if(!glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }
        if(!glfwVulkanSupported()) {
            throw AssertionError("GLFW failed to find the Vulkan loader")
        }

        val layers = mutableListOf<String>()
        if(DEBUG) {
            layers.add("VK_LAYER_LUNARG_standard_validation")
            layers.add("VK_LAYER_LUNARG_assistant_layer")
        }

        instance = createInstance(layers)
        if(DEBUG) {
            instance.createDebugCallback().let {
                debugCallback       = it.first
                debugCallbackHandle = it.second
            }
        }
        physicalDevice = selectBestPhysicalDevice(instance = instance, requiredExtensions = listOf())
        physicalDeviceMemoryProperties = getMemoryProperties(physicalDevice)
        physicalDeviceMemoryProperties.dump()
        physicalDeviceProperties = physicalDevice.getProperties()
        physicalDeviceProperties.dump()

        if(!client.headless) {

            window = createWindow()
            log.info("Window size ... ${windowSize.string()}")

            surface = createSurface()

            selectSurfaceFormat(physicalDevice, surface, surfaceFormat)
        }

        selectQueueFamilies(queueFamilies, physicalDevice, client, surface)

        client.enableFeatures(features)
        device = createLogicalDevice(physicalDevice, queueFamilies, deviceExtensions, features)

        graphicsQueues = device.getQueues(queueFamilies.graphics, queueFamilies.numGraphicsQueues)
        transferQueues = device.getQueues(queueFamilies.transfer, queueFamilies.numTransferQueues)
        computeQueues  = device.getQueues(queueFamilies.compute, queueFamilies.numComputeQueues)

        if(!client.headless) {
            if(!canPresent(physicalDevice, surface, queueFamilies.graphics)) {
                throw Error("Can't present on this surface")
            }

            renderPass = client.createRenderPass(device, surfaceFormat.colorFormat)

            swapChain = SwapChain(this, surfaceFormat)

            graphicsCP = device.createCommandPools(
                queueFamilies.graphics,
                VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT or VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
            )
            log.info("Created graphics command pool using queue family ${queueFamilies.graphics}")

            frameResources = createPerFrameResources()
        }

        if(queueFamilies.numTransferQueues>0) {
            transferCP = device.createCommandPools(queueFamilies.transfer, VK_COMMAND_POOL_CREATE_TRANSIENT_BIT)
            log.info("Created transfer command pool using queue family ${queueFamilies.transfer}")
        }

        shaders.init(device)
        fonts.init(this)

        log.info("Vulkan ready")
        client.vulkanReady(this)
    }
    fun destroy() {
        log.info("Destroying VulkanApplication")
        device?.let {
            it.waitForIdle()

            physicalDeviceMemoryProperties?.free()
            physicalDeviceProperties?.free()

            if(!client.headless) {
                frameResources?.forEach { pfr ->
                    pfr.imageAvailable?.destroy()
                    pfr.renderFinished?.destroy()
                    pfr.fence?.destroy()
                }
                swapChain?.destroy()
                graphicsCP?.destroy()
            }

            transferCP?.destroy()

            shaders.destroy()
            fonts.destroy()

            features.free()

            it.destroy()
        }
        glfwCallbacks.forEach { it.free() }
        debugCallback?.free()
        if(debugCallbackHandle!=VK_NULL_HANDLE) vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null)
        if(surface!=VK_NULL_HANDLE) vkDestroySurfaceKHR(instance, surface, null)

        instance.destroy()
        glfwDestroyWindow(window)
        glfwTerminate()
    }
    fun mainLoop() {
        log.info("Entering mainLoop")
        var frameNumber           = 0L
        var seconds               = 0L
        var tenSeconds            = 0L
        var previousTimestamp     = System.nanoTime()
        val targetFrameTimeNsecs  = (1_000_000_000 / client.targetFPS).toLong()
        val frameInfo             = FrameInfo(0, 0.0, 1.0)

        while(!glfwWindowShouldClose(window)) {

            renderFrame(frameInfo)

            glfwPollEvents()

            val timestamp  = System.nanoTime()
            val frameNsecs = timestamp - previousTimestamp
            previousTimestamp = timestamp

            frameInfo.delta = frameNsecs.toDouble()/targetFrameTimeNsecs.toDouble()
            frameInfo.number++
            frameInfo.relNumber += frameInfo.delta

            frameNumber = frameInfo.number

            //frameTiming.endFrame(frameNsecs);

            if(timestamp/BILLION > seconds) {
                seconds = timestamp/BILLION
                fps = BILLION.toDouble() / frameNsecs.toDouble()

                //val avg = frameTiming.average(2);
                //currentFPS = 1000.0 / avg;

                log.info(String.format("Frame (abs:%s, rel:%.2f) delta=%.4f ms:%.3f fps:%.2f",
                    frameInfo.number,
                    frameInfo.relNumber,
                    frameInfo.delta,
                    frameNsecs/MILLION.toDouble(),
                    fps))
            }
            if(timestamp/(BILLION*10) > tenSeconds) {
                tenSeconds = timestamp/(BILLION*10)

                val totalMem = Runtime.getRuntime().totalMemory()
                val freeMem  = Runtime.getRuntime().freeMemory()
                log.info("VM memory usage: Used ${(totalMem-freeMem)/ MEGABYTE} / ${totalMem/ MEGABYTE} MB")
            }
        }
        log.info("Exiting mainLoop on frame $frameNumber")
    }
    //=================================================================================================
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
        animations.update(frame.delta)

        /** Let the app do its thing */
        client.render(frame, resource)

        /** Present */
        swapChain.queuePresent(
            queue          = graphicsQueues[0],
            imageIndex     = index,
            waitSemaphores = arrayOf(resource.renderFinished))
    }
    private fun createWindow():Long {
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)

        val resizable   = false
        val decorated   = true
        val autoIconify = false
        var monitor = glfwGetPrimaryMonitor()
        val vidmode = glfwGetVideoMode(monitor)!!

        if(client.windowed) {
            monitor = 0
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
            glfwWindowHint(GLFW_RESIZABLE, if(resizable) GLFW_TRUE else GLFW_FALSE)
            glfwWindowHint(GLFW_DECORATED, if(decorated) GLFW_TRUE else GLFW_FALSE)

        } else {
            // for the moment always use the desktop resolution
            requestedWindowSize.x = vidmode.width()
            requestedWindowSize.y = vidmode.height()
        }

        glfwWindowHint(GLFW_AUTO_ICONIFY, if(autoIconify) GLFW_TRUE else GLFW_FALSE)

        val window = glfwCreateWindow(requestedWindowSize.x, requestedWindowSize.y, windowTitle, monitor, NULL).let {
            if(it==0L) throw RuntimeException("Failed to create the GLFW window")
            it
        }

        glfwSetWindowPos(
            window,
            (vidmode.width() - requestedWindowSize.x) / 2,
            (vidmode.height() - requestedWindowSize.y) / 2
        )

        log.info("Video mode .... (${vidmode.width()} x ${vidmode.height()} ${vidmode.refreshRate()} Hz")
        return window
    }
    private fun createSurface():VkSurfaceKHR {
        MemoryStack.stackPush().use { stack ->
            val pSurface = stack.mallocLong(1)
            glfwCreateWindowSurface(instance, window, null, pSurface).check()
            return pSurface.get(0)
        }
    }
    private fun createPerFrameResources():List<PerFrameResource> {
        assert(swapChain.frameBuffers.isNotEmpty()) { "Swapchain framebuffers not yet initialised" }

        val list = ArrayList<PerFrameResource>()

        (0 until swapChain.numImages).forEach { i ->
            val r = PerFrameResource(
                index = i,
                image = swapChain.images[i],
                imageView = swapChain.views[i],
                frameBuffer = swapChain.frameBuffers[i],
                adhocCB = graphicsCP.alloc(),
                fence = device.createFence(signalled = true),
                imageAvailable = device.createSemaphore(),
                renderFinished = device.createSemaphore()
            )
            list.add(r)
        }
        log.info("Created ${list.size} per frame resources")
        return list
    }
}

