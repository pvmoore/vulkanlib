package vulkan.api

import org.lwjgl.glfw.GLFWVulkan
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.EXTDebugReport.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.log
import vulkan.misc.check
import vulkan.misc.dumpInstanceExtensions
import vulkan.misc.dumpInstanceLayers

fun createInstance(layers:List<String>): VkInstance {
    log.info("Creating Vulkan instance")

    dumpInstanceLayers()
    dumpInstanceExtensions()

    MemoryStack.stackPush().use { stack ->
        /** Extensions */
        val requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions()!!
        val enabledExtensions  = stack.callocPointer(requiredExtensions.remaining()+1)

        enabledExtensions.put(requiredExtensions)
        enabledExtensions.put(stack.UTF8(VK_EXT_DEBUG_REPORT_EXTENSION_NAME))
        enabledExtensions.flip()

        /** Layers */
        val enabledLayers = stack.callocPointer(layers.size)
        layers.forEach {
            log.info("Enabling layer $it")
            enabledLayers.put(stack.UTF8(it))
        }
        enabledLayers.flip()

        val appInfo = VkApplicationInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
            .pNext(MemoryUtil.NULL)
            .pApplicationName(stack.UTF8("Vulkan"))
            .applicationVersion(0)
            .pEngineName(stack.UTF8(""))
            .engineVersion(0)
            .apiVersion(VK_MAKE_VERSION(1, 0, 0))

        val createInfo = VkInstanceCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
            .pNext(MemoryUtil.NULL)
            .pApplicationInfo(appInfo)
            .ppEnabledExtensionNames(enabledExtensions)
            .ppEnabledLayerNames(enabledLayers)

        val instancePtr = stack.mallocPointer(1)
        vkCreateInstance(createInfo, null, instancePtr).check()
        instancePtr.get(0).let {
            return VkInstance(it, createInfo)
        }
    }
}
fun VkInstance.destroy() {
    vkDestroyInstance(this, null)
}
fun VkInstance.createDebugCallback(): Pair<VkDebugReportCallbackEXT, Long> {
    log.info("Creating debug callback")
    val debugCallback = object : VkDebugReportCallbackEXT() {
        override fun invoke(
            flags: Int,
            objectType: Int,
            `object`: Long,
            location: Long,
            messageCode: Int,
            pLayerPrefix: Long,
            pMessage: Long,
            pUserData: Long
        ): Int {
            val level:String = when {
                (flags and VK_DEBUG_REPORT_ERROR_BIT_EXT)!=0 -> "ERROR"
                (flags and VK_DEBUG_REPORT_WARNING_BIT_EXT)!=0 -> "WARN"
                (flags and VK_DEBUG_REPORT_INFORMATION_BIT_EXT)!=0 -> "INFO"
                (flags and VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT)!=0 -> "PERF"
                else -> "DEBUG"
            }
            if(level in setOf("ERROR", "WARN")) {
                log.error("$level: " + VkDebugReportCallbackEXT.getString(pMessage))
                System.err.println("$level: " + VkDebugReportCallbackEXT.getString(pMessage))
            } else {
                log.info("$level: " + VkDebugReportCallbackEXT.getString(pMessage))
            }
            return 0
        }
    }
    MemoryStack.stackPush().use { stack ->
        val dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.callocStack()
            .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
            .pNext(MemoryUtil.NULL)
            .pfnCallback(debugCallback)
            .pUserData(MemoryUtil.NULL)
            .flags(//VK_DEBUG_REPORT_DEBUG_BIT_EXT or
                   //EXTDebugReport.VK_DEBUG_REPORT_INFORMATION_BIT_EXT or
                   VK_DEBUG_REPORT_ERROR_BIT_EXT or
                   VK_DEBUG_REPORT_WARNING_BIT_EXT or
                   VK_DEBUG_REPORT_PERFORMANCE_WARNING_BIT_EXT
            )

        val pCallback = stack.mallocLong(1)
        vkCreateDebugReportCallbackEXT(this, dbgCreateInfo, null, pCallback).check()
        val debugCallbackHandle = pCallback.get(0)

        return Pair(debugCallback, debugCallbackHandle)
    }
}
