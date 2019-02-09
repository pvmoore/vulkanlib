package vulkan.misc

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VK11.VK_MEMORY_PROPERTY_PROTECTED_BIT

fun VkPhysicalDeviceProperties.dump() {
    println("Physical device properties:")
    println("\tvendorID ....... ${vendorID()}")
    println("\tdeviceID ....... ${deviceID()}")
    println("\tdeviceName ..... ${deviceNameString()}")
    println("\tdeviceType ..... ${deviceType()}")
    println("\tdriverVersion .. ${versionToString(driverVersion())}")
    println("\tapiVersion ..... ${versionToString(apiVersion())}")

    limits().dump()
}
fun VkPhysicalDeviceLimits.dump() {
    println("Physical device limits:")
    println("\t- maxImageDimension1D ............... ${maxImageDimension1D()}")
    println("\t- maxImageDimension2D ............... ${maxImageDimension2D()}")
    println("\t- maxImageDimension3D ............... ${maxImageDimension3D()}")
    println("\t- maxComputeSharedMemorySize ........ ${maxComputeSharedMemorySize()}")
    println("\t- maxComputeWorkGroupCount .......... ${maxComputeWorkGroupCount()}")

    println("\t- maxComputeWorkGroupCount .......... ${maxComputeWorkGroupSize().string()}")

    println("\t- maxUniformBufferRange ............. ${maxUniformBufferRange()}")
    println("\t- maxStorageBufferRange ............. ${maxStorageBufferRange()}")

    println("\t- timestampComputeAndGraphics ....... ${timestampComputeAndGraphics()}")
    println("\t- timestampPeriod ................... ${timestampPeriod()}")
    println("\t- discreteQueuePriorities ........... ${discreteQueuePriorities()}")
    println("\t- maxPushConstantsSize .............. ${maxPushConstantsSize()}")
    println("\t- maxSamplerAllocationCount ......... ${maxSamplerAllocationCount()}")
    println("\t- bufferImageGranularity ............ ${bufferImageGranularity()}")

    println("\t- maxBoundDescriptorSets ............ ${maxBoundDescriptorSets()}")

    println("\t- minUniformBufferOffsetAlignment ... ${minUniformBufferOffsetAlignment()}")

    println("\t- minStorageBufferOffsetAlignment ... ${minStorageBufferOffsetAlignment()}")
    println("\t- minMemoryMapAlignment ............. ${minMemoryMapAlignment()}")
    println("\t- maxMemoryAllocationCount .......... ${maxMemoryAllocationCount()}")

    println("\t- maxDescriptorSetSamplers .......... ${maxDescriptorSetSamplers()}")
    println("\t- maxDescriptorSetStorageBuffers .... ${maxDescriptorSetStorageBuffers()}")
    println("\t- maxSamplerAnisotropy .............. ${maxSamplerAnisotropy()}")
    println("\t- maxViewports ...................... ${maxViewports()}")
    println("\t- maxViewportDimensions [x,y] ....... ${maxViewportDimensions().string()}")

    println("\t- maxFramebufferWidth ............... ${maxFramebufferWidth()}")
    println("\t- maxFramebufferHeight .............. ${maxFramebufferHeight()}")

    println("\t- optimalBufferCopyOffsetAlignment .. ${optimalBufferCopyOffsetAlignment()}")
}
fun VkExtensionProperties.Buffer.dump() {
    println("Physical Device Extensions:")
    this.forEach {
        println("\t${it.extensionNameString()} version:${it.specVersion()}")
    }
}
fun VkPhysicalDeviceFeatures.dump() {
    println("\tFeatures:")
    println("\t- geometryShader     .. ${geometryShader()}")
    println("\t- tessellationShader .. ${tessellationShader()}")
    println("\t- shaderFloat64 ....... ${shaderFloat64()}")
    println("\t- shaderInt64 ......... ${shaderInt64()}")
    println("\t- shaderInt16 ......... ${shaderInt16()}")
    println("\t- samplerAnisotropy ... ${samplerAnisotropy()}")
}
fun VkQueueFamilyProperties.Buffer.dump() {
    println("\tQueue families: ${this.count()}")

    this.forEachIndexed { i, f ->
        println("\t[Family $i] QueueCount: ${f.queueCount()}, " +
                "flags: ${f.queueFlags().translateVkQueueFlags()}, " +
                "timestampValidBits: ${f.timestampValidBits()}, " +
                "minImageTransferGranularity: ${f.minImageTransferGranularity().translateVkEntent3D()}")
    }
}
fun dumpInstanceExtensions() {
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        VK10.vkEnumerateInstanceExtensionProperties(null as CharSequence?, count, null)
        println("Instance extensions: ${count.get(0)}")
        if(count.get(0)>0) {
            val props = VkExtensionProperties.callocStack(count.get(0))
            VK10.vkEnumerateInstanceExtensionProperties(null as CharSequence?, count, props)

            props.forEachIndexed { i, it ->
                println("\t[$i] extensionName: ${it.extensionNameString()} specVersion:${it.specVersion()}")
            }
        }
    }
}
fun dumpInstanceLayers() {
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        VK10.vkEnumerateInstanceLayerProperties(count, null);
        println("Instance layers: ${count.get(0)}")
        if(count.get(0) > 0) {
            val layerProps = VkLayerProperties.callocStack(count.get(0))
            VK10.vkEnumerateInstanceLayerProperties(count, layerProps)

            layerProps.forEachIndexed { i, it ->
                println(
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
    println("Surface capabilities:")
    println("   minImageCount  = ${minImageCount()}")
    println("   maxImageCount  = ${maxImageCount()}")
    println("   currentExtent  = ${currentExtent().translateVkEntent2D()}")
    println("   minImageExtent = ${minImageExtent().translateVkEntent2D()}")
    println("   maxImageExtent = ${maxImageExtent().translateVkEntent2D()}")
    println("   maxImageArrayLayers ${maxImageArrayLayers()}")
    println("   supportedTransforms ${supportedTransforms()}")
    println("   currentTransform = ${currentTransform()}")
    println("   supportedCompositeAlpha ${supportedCompositeAlpha()}")
    println("   supportedUsageFlags = ${supportedUsageFlags().translateVkImageUsageFlags()}")
}
fun VkPhysicalDeviceMemoryProperties.dump() {
    println("Physical device memory properties:")

    this.memoryTypes().forEachIndexed { i, type ->
        val isLocal      = (type.propertyFlags() and VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0
        val hostVisible  = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0
        val hostCoherent = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0
        val hostCached   = (type.propertyFlags() and VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0
        val lazyAlloc    = (type.propertyFlags() and VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT) != 0
        val protected    = (type.propertyFlags() and VK_MEMORY_PROPERTY_PROTECTED_BIT) != 0

        println("   Type[$i]: heap: ${type.heapIndex()}, " +
                "isLocal: $isLocal, " +
                "hostVisible: $hostVisible, " +
                "hostCoherent: $hostCoherent, " +
                "hostCached: $hostCached, " +
                "lazyAlloc: $lazyAlloc, " +
                "protected: $protected")
    }
    this.memoryHeaps().forEachIndexed { i, heap ->
        val isLocal = (heap.flags() and VK_MEMORY_HEAP_DEVICE_LOCAL_BIT) != 0
        println("   Heap[$i] size: ${heap.size()/(1024*1024)} MB, local: $isLocal")
    }
}
//fun dump(presentModes:VkPresentModeKHR[]) {
//    log("Present modes:");
//    foreach(i, pm; presentModes) {
//        log("   [%s] %s", i, pm.to!translateVkEntent2D);
//    }
//}
fun dump(m: VkMemoryRequirements, name:String="") {
    println("Memory requirements ($name) :")
    println("   size:${m.size()}, alignment:${m.alignment()} memoryTypeBits:${m.memoryTypeBits()}")
}
