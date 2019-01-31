package vulkan.app

import org.apache.log4j.Level
import org.apache.log4j.LogManager
import org.apache.log4j.Logger
import org.lwjgl.Version
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported
import org.lwjgl.system.Configuration
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.vkDestroyDebugReportCallbackEXT
import org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME
import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import vulkan.api.*
import vulkan.common.*
import vulkan.misc.dump

class VulkanApplication(val client: VulkanClient) {
    private val deviceExtensions                        = listOf(VK_KHR_SWAPCHAIN_EXTENSION_NAME)
    private var debugCallbackHandle:Long                = VK_NULL_HANDLE
    private var debugCallback:VkDebugReportCallbackEXT? = null
    private val features                                = VkPhysicalDeviceFeatures.calloc()

    lateinit var instance:VkInstance
    lateinit var physicalDevice:VkPhysicalDevice
    lateinit var device:VkDevice
    lateinit var physicalDeviceProperties:VkPhysicalDeviceProperties
    lateinit var physicalDeviceMemoryProperties:VkPhysicalDeviceMemoryProperties

    val queues   = Queues(client)
    val shaders  = Shaders
    val graphics = GraphicsComponent(client)

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

        val layers = ArrayList<String>()
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

        queues.init(instance, physicalDevice)

        client.enableFeatures(features)
        device = createLogicalDevice(physicalDevice, queues, deviceExtensions, features)

        queues.deviceCreated(device)

        if(!client.headless) {
            graphics.init(this)
        }

        shaders.init(device)

        log.info("Vulkan ready")
        client.vulkanReady(this)
    }
    fun destroy() {
        log.info("Destroying VulkanApplication")
        device?.let {
            it.waitForIdle()

            physicalDeviceMemoryProperties?.free()
            physicalDeviceProperties?.free()

            shaders.destroy()
            features.free()

            if(graphics.isInitialised) graphics.destroy()

            it.destroy()
        }
        debugCallback?.free()
        if(debugCallbackHandle!=VK_NULL_HANDLE) vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null)

        instance.destroy()
        glfwTerminate()
    }
}

