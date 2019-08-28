package vulkan.misc

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_MEMORY_PROPERTY_PROTECTED_BIT
import vulkan.common.log

fun VkPhysicalDeviceProperties.dump() {
    log.info("Physical device properties:")
    log.info("\tvendorID ....... ${vendorID()}")
    log.info("\tdeviceID ....... ${deviceID()}")
    log.info("\tdeviceName ..... ${deviceNameString()}")
    log.info("\tdeviceType ..... ${deviceType()}")
    log.info("\tdriverVersion .. ${versionToString(driverVersion())}")
    log.info("\tapiVersion ..... ${versionToString(apiVersion())}")

    limits().dump()
}
fun VkPhysicalDeviceLimits.dump() {
    log.info("Physical device limits:")
    log.info("\t- maxImageDimension1D ............... ${maxImageDimension1D()}")
    log.info("\t- maxImageDimension2D ............... ${maxImageDimension2D()}")
    log.info("\t- maxImageDimension3D ............... ${maxImageDimension3D()}")
    log.info("\t- maxComputeSharedMemorySize ........ ${maxComputeSharedMemorySize()}")
    log.info("\t- maxComputeWorkGroupCount .......... ${maxComputeWorkGroupCount()}")

    log.info("\t- maxComputeWorkGroupCount .......... ${maxComputeWorkGroupSize().string()}")

    log.info("\t- maxUniformBufferRange ............. ${maxUniformBufferRange()}")
    log.info("\t- maxStorageBufferRange ............. ${maxStorageBufferRange()}")

    log.info("\t- timestampComputeAndGraphics ....... ${timestampComputeAndGraphics()}")
    log.info("\t- timestampPeriod ................... ${timestampPeriod()}")
    log.info("\t- discreteQueuePriorities ........... ${discreteQueuePriorities()}")
    log.info("\t- maxPushConstantsSize .............. ${maxPushConstantsSize()}")
    log.info("\t- maxSamplerAllocationCount ......... ${maxSamplerAllocationCount()}")
    log.info("\t- bufferImageGranularity ............ ${bufferImageGranularity()}")

    log.info("\t- maxBoundDescriptorSets ............ ${maxBoundDescriptorSets()}")

    log.info("\t- minUniformBufferOffsetAlignment ... ${minUniformBufferOffsetAlignment()}")

    log.info("\t- minStorageBufferOffsetAlignment ... ${minStorageBufferOffsetAlignment()}")
    log.info("\t- minMemoryMapAlignment ............. ${minMemoryMapAlignment()}")
    log.info("\t- maxMemoryAllocationCount .......... ${maxMemoryAllocationCount()}")

    log.info("\t- maxDescriptorSetSamplers .......... ${maxDescriptorSetSamplers()}")
    log.info("\t- maxDescriptorSetStorageBuffers .... ${maxDescriptorSetStorageBuffers()}")
    log.info("\t- maxSamplerAnisotropy .............. ${maxSamplerAnisotropy()}")
    log.info("\t- maxViewports ...................... ${maxViewports()}")
    log.info("\t- maxViewportDimensions [x,y] ....... ${maxViewportDimensions().string()}")

    log.info("\t- maxFramebufferWidth ............... ${maxFramebufferWidth()}")
    log.info("\t- maxFramebufferHeight .............. ${maxFramebufferHeight()}")

    log.info("\t- optimalBufferCopyOffsetAlignment .. ${optimalBufferCopyOffsetAlignment()}")
}
fun VkExtensionProperties.Buffer.dump() {
    log.info("Physical Device Extensions:")
    this.forEach {
        log.info("\t${it.extensionNameString()} version:${it.specVersion()}")
    }
}
fun VkPhysicalDeviceFeatures.dump() {
    log.info("\tFeatures:")
    log.info("\t- geometryShader     .. ${geometryShader()}")
    log.info("\t- tessellationShader .. ${tessellationShader()}")
    log.info("\t- shaderFloat64 ....... ${shaderFloat64()}")
    log.info("\t- shaderInt64 ......... ${shaderInt64()}")
    log.info("\t- shaderInt16 ......... ${shaderInt16()}")
    log.info("\t- samplerAnisotropy ... ${samplerAnisotropy()}")
}
fun VkQueueFamilyProperties.Buffer.dump() {
    log.info("\tQueue families: ${this.count()}")

    this.forEachIndexed { i, f ->
        log.info("\t[Family $i] QueueCount: ${f.queueCount()}, " +
                "flags: ${f.queueFlags().translateVkQueueFlags()}, " +
                "timestampValidBits: ${f.timestampValidBits()}, " +
                "minImageTransferGranularity: ${f.minImageTransferGranularity().translateVkEntent3D()}")
    }
}
fun dumpInstanceExtensions() {
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        vkEnumerateInstanceExtensionProperties(null as CharSequence?, count, null)
        log.info("Instance extensions: ${count.get(0)}")
        if(count.get(0)>0) {
            val props = VkExtensionProperties.callocStack(count.get(0))
            vkEnumerateInstanceExtensionProperties(null as CharSequence?, count, props)

            props.forEachIndexed { i, it ->
                log.info("\t[$i] extensionName: ${it.extensionNameString()} specVersion:${it.specVersion()}")
            }
        }
    }
}
fun dumpInstanceLayers() {
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        VK10.vkEnumerateInstanceLayerProperties(count, null);
        log.info("Instance layers: ${count.get(0)}")
        if(count.get(0) > 0) {
            val layerProps = VkLayerProperties.callocStack(count.get(0))
            VK10.vkEnumerateInstanceLayerProperties(count, layerProps)

            layerProps.forEachIndexed { i, it ->
                log.info(
                    "\t[$i] layer name:${it.layerNameString()} " +
                            "desc:${it.descriptionString()} " +
                            "specVersion:${versionToString(it.specVersion())} " +
                            "implVersion:${it.implementationVersion()}"
                )

            }
        }
    }
}
fun VkSurfaceCapabilitiesKHR.dump() {
    log.info("Surface capabilities:")
    log.info("   minImageCount  = ${minImageCount()}")
    log.info("   maxImageCount  = ${maxImageCount()}")
    log.info("   currentExtent  = ${currentExtent().translateVkEntent2D()}")
    log.info("   minImageExtent = ${minImageExtent().translateVkEntent2D()}")
    log.info("   maxImageExtent = ${maxImageExtent().translateVkEntent2D()}")
    log.info("   maxImageArrayLayers ${maxImageArrayLayers()}")
    log.info("   supportedTransforms ${supportedTransforms()}")
    log.info("   currentTransform = ${currentTransform()}")
    log.info("   supportedCompositeAlpha ${supportedCompositeAlpha()}")
    log.info("   supportedUsageFlags = ${supportedUsageFlags().translateVkImageUsageFlags()}")
}
fun VkPhysicalDeviceMemoryProperties.dump() {
    log.info("Physical device memory properties:")

    this.memoryTypes().forEachIndexed { i, type ->
        val isLocal      = (type.propertyFlags() and VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0
        val hostVisible  = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0
        val hostCoherent = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0
        val hostCached   = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0
        val lazyAlloc    = (type.propertyFlags() and VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0
        val protected    = (type.propertyFlags() and VK_MEMORY_PROPERTY_PROTECTED_BIT) != 0

        log.info("   Type[$i]: heap: ${type.heapIndex()}, " +
                "isLocal: $isLocal, " +
                "hostVisible: $hostVisible, " +
                "hostCoherent: $hostCoherent, " +
                "hostCached: $hostCached, " +
                "lazyAlloc: $lazyAlloc, " +
                "protected: $protected")
    }
    this.memoryHeaps().forEachIndexed { i, heap ->
        val isLocal = (heap.flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0
        log.info("   Heap[$i] size: ${heap.size()/(1024*1024)} MB, local: $isLocal")
    }
}
//fun dump(presentModes:VkPresentModeKHR[]) {
//    log("Present modes:");
//    foreach(i, pm; presentModes) {
//        log("   [%s] %s", i, pm.to!translateVkEntent2D);
//    }
//}
fun dump(m: VkMemoryRequirements, name:String="") {
    log.info("Memory requirements ($name) :")
    log.info("   size:${m.size()}, alignment:${m.alignment()} memoryTypeBits:${m.memoryTypeBits()}")
}
