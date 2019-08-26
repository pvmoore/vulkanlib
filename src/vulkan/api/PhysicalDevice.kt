package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.log
import vulkan.misc.check
import vulkan.misc.dump
import vulkan.misc.forEach

fun selectBestPhysicalDevice(instance: VkInstance,
                             requiredExtensions:List<String>)
    :VkPhysicalDevice
{
    log.info("Selecting best physical device...")
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)

        fun getProperties(pDevice:VkPhysicalDevice): VkPhysicalDeviceProperties {
            val properties = VkPhysicalDeviceProperties.callocStack()
            vkGetPhysicalDeviceProperties(pDevice, properties)
            return properties
        }
        fun getExtensions(pDevice:VkPhysicalDevice):VkExtensionProperties.Buffer {
            vkEnumerateDeviceExtensionProperties(pDevice, null as CharSequence?, count, null).check()
            val extensions = VkExtensionProperties.callocStack(count.get(0))
            vkEnumerateDeviceExtensionProperties(pDevice, null as CharSequence?, count, extensions).check()
            return extensions
        }
        fun getFormatProperties(device:VkPhysicalDevice, format:Int):VkFormatProperties {
            val props = VkFormatProperties.callocStack()
            vkGetPhysicalDeviceFormatProperties(device, format, props)
            return props
        }
        fun supportsRequiredExtensions(device:VkPhysicalDevice):Boolean {
            val extensions = getExtensions(device)
            return requiredExtensions.all { r->
                extensions.any { e-> e.extensionNameString()==r }
            }
        }
        /**
         *  The spec says if no flags are set at all then the format is not usable.
         */
        fun isFormatSupported(device:VkPhysicalDevice, format:Int):Boolean {
            val fp = getFormatProperties(device, format)
            return  fp.linearTilingFeatures()!=0 ||
                    fp.optimalTilingFeatures()!=0 ||
                    fp.bufferFeatures()!=0
        }

        vkEnumeratePhysicalDevices(instance, count, null).check()
        val physicalDevices = stack.mallocPointer(count.get(0))
        vkEnumeratePhysicalDevices(instance, count, physicalDevices).check()

        log.info("Found ${count.get(0)} devices")

        when(count.get(0)) {
            0    -> throw Error("No Vulkan devices found")
            else -> {
                var preferredDevice:VkPhysicalDevice? = null

                physicalDevices.forEach { it: Long ->
                    val device = VkPhysicalDevice(it, instance)

                    if(supportsRequiredExtensions(device)) {
                        val props = getProperties(device)
                        if(props.deviceType()==VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU) {
                            preferredDevice = device
                        }
                    }
                }
                if(preferredDevice==null) {
                    throw Error("No Vulkan device found with required extensions")
                }
                preferredDevice?.let {
                    log.info("Found device matching required extensions")
                    log.info("Format R8G8B8A8_UNORM supported: ${isFormatSupported(it, VK_FORMAT_R8G8B8A8_UNORM)}")
                    log.info("Format B8G8R8A8_UNORM supported: ${isFormatSupported(it, VK_FORMAT_B8G8R8A8_UNORM)}")
                    log.info("Format R8G8B8_UNORM supported: ${isFormatSupported(it, VK_FORMAT_R8G8B8_UNORM)}")
                    log.info("Format B8G8R8_UNORM supported: ${isFormatSupported(it, VK_FORMAT_B8G8R8_UNORM)}")
                    log.info("Format R8_UNORM supported: ${isFormatSupported(it, VK_FORMAT_R8_UNORM)}")
                    log.info("Format R16_SFLOAT supported: ${isFormatSupported(it, VK_FORMAT_R16_SFLOAT)}")
                    log.info("Format R32_SFLOAT supported: ${isFormatSupported(it, VK_FORMAT_R32_SFLOAT)}")

                    // depth/stencil formats
                    log.info("Format VK_FORMAT_D32_SFLOAT supported: ${isFormatSupported(it, VK_FORMAT_D32_SFLOAT)}")
                    log.info("Format VK_FORMAT_D16_UNORM_S8_UINT supported: ${isFormatSupported(it, VK_FORMAT_D16_UNORM_S8_UINT)}")
                    log.info("Format VK_FORMAT_D24_UNORM_S8_UINT supported: ${isFormatSupported(it, VK_FORMAT_D24_UNORM_S8_UINT)}")
                    log.info("Format VK_FORMAT_D32_SFLOAT_S8_UINT supported: ${isFormatSupported(it, VK_FORMAT_D32_SFLOAT_S8_UINT)}")

                }

                getExtensions(preferredDevice!!).dump()

                return preferredDevice!!
            }
        }
    }
}
// Returned memory needs to be freed
fun getMemoryProperties(device:VkPhysicalDevice):VkPhysicalDeviceMemoryProperties {
    val props = VkPhysicalDeviceMemoryProperties.calloc()
    vkGetPhysicalDeviceMemoryProperties(device, props)
    return props
}
fun canPresent(device:VkPhysicalDevice, surface:Long, queueFamilyIndex:Int):Boolean {
    MemoryStack.stackPush().use { stack ->
        val result = stack.mallocInt(1)

        vkGetPhysicalDeviceSurfaceSupportKHR(device, queueFamilyIndex, surface, result).check()
        return result.get(0) == 1
    }
}
/** @return object that needs to be freed by the caller */
fun VkPhysicalDevice.getProperties(): VkPhysicalDeviceProperties {
    val properties = VkPhysicalDeviceProperties.calloc()
    vkGetPhysicalDeviceProperties(this, properties)
    return properties
}
/** @return object that needs to be freed by the caller */
fun VkPhysicalDevice.getFeatures() : VkPhysicalDeviceFeatures2 {
    val features = VkPhysicalDeviceFeatures2.calloc()
    VK11.vkGetPhysicalDeviceFeatures2(this, features)
    return features
}
/** @return Buffer that needs to be freed by caller */
fun VkPhysicalDevice.getExtensions():VkExtensionProperties.Buffer {
    val count = MemoryUtil.memAllocInt(1)
    vkEnumerateDeviceExtensionProperties(this, null as CharSequence?, count, null).check()
    val extensions = VkExtensionProperties.calloc(count.get(0))
    vkEnumerateDeviceExtensionProperties(this, null as CharSequence?, count, extensions).check()
    memFree(count)
    return extensions
}

fun getFormats(stack:MemoryStack, pDevice:VkPhysicalDevice, surface:Long):VkSurfaceFormatKHR.Buffer {
    val count = stack.mallocInt(1)
    vkGetPhysicalDeviceSurfaceFormatsKHR(pDevice, surface, count, null).check()

    val formats = VkSurfaceFormatKHR.callocStack(count.get(0))
    vkGetPhysicalDeviceSurfaceFormatsKHR(pDevice, surface, count, formats).check()
    return formats
}
fun getCapabilities(stack:MemoryStack, pDevice:VkPhysicalDevice, surface:Long):VkSurfaceCapabilitiesKHR {
    val caps = VkSurfaceCapabilitiesKHR.callocStack()
    vkGetPhysicalDeviceSurfaceCapabilitiesKHR(pDevice, surface, caps).check()
    return caps
}
fun getPresentModes(pDevice:VkPhysicalDevice, surface:Long):IntArray {
    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)
        vkGetPhysicalDeviceSurfacePresentModesKHR(pDevice, surface, count, null).check()
        val buffer = stack.mallocInt(count.get(0))
        vkGetPhysicalDeviceSurfacePresentModesKHR(pDevice, surface, count, buffer).check()

        val array = IntArray(count.get(0))
        buffer.get(array)

        return array
    }
}
/*
auto getSparseImageFormatProperties(
    VkPhysicalDevice pDevice,
    VkFormat format,
    VkImageType type,
    uint samples,
    VkImageUsageFlags usage,
    VkImageTiling tiling)
{
    VkSparseImageFormatProperties[] properties;
    uint count;
    vkGetPhysicalDeviceSparseImageFormatProperties(
        pDevice, format, type, cast(VkSampleCountFlagBits)samples, usage, tiling, &count, null);
    properties.length = count;
    vkGetPhysicalDeviceSparseImageFormatProperties(
            pDevice, format, type, cast(VkSampleCountFlagBits)samples, usage, tiling, &count, properties.ptr);
    return properties;
}
*/