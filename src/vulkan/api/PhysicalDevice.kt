package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.QueueFamilies
import vulkan.common.SurfaceFormat
import vulkan.common.VulkanClient
import vulkan.common.log
import vulkan.misc.*

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
         *  This check is a bit vague and propably incorrect. At the moment
         *  I just check the linear tiling features for any set flags. The spec
         *  says if no flags are set at all then the format is not usable.
         */
        fun isFormatSupported(device:VkPhysicalDevice, format:Int):Boolean {
            val fp = getFormatProperties(device, format)
            if(fp.linearTilingFeatures()==0) return false
            return true
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
                }
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
fun selectQueueFamilies(queueFamilies: QueueFamilies,
                        physicalDevice:VkPhysicalDevice,
                        client: VulkanClient,
                        surface:Long)
{
    log.info("Selecting queue families")

    MemoryStack.stackPush().use { stack ->
        val count = stack.mallocInt(1)

        fun getQueueFamilyProps(pDevice: VkPhysicalDevice): VkQueueFamilyProperties.Buffer {
            vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, null)
            val queueCount = count.get(0)
            val qFamilies = VkQueueFamilyProperties.callocStack(queueCount)
            vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, qFamilies)
            return qFamilies
        }
        fun isGraphics(f:Int):Boolean = (f and VK_QUEUE_GRAPHICS_BIT) != 0
        fun isCompute(f:Int):Boolean  = (f and VK_QUEUE_COMPUTE_BIT)  != 0
        fun isTransfer(f:Int):Boolean = (f and VK_QUEUE_TRANSFER_BIT) != 0
        fun canPresent(i:Int):Boolean {
            return surface!=0L && canPresent(physicalDevice, surface, i)
        }

        val queueFamilyProps = getQueueFamilyProps(physicalDevice)

        queueFamilyProps.dump()

        // - Prefer graphics and compute on different queues
        // - Prefer a dedicated transfer queue

        queueFamilyProps.forEachIndexed { i, family->
            if(family.queueCount()>0) {

                if(client.prefNumGraphicsQueues>0 && isGraphics(family.queueFlags()) and canPresent(i)) {
                    if(queueFamilies.graphics==-1 || !isCompute(family.queueFlags())) {
                        queueFamilies.graphics = i
                        queueFamilies.numGraphicsQueues = Math.min(family.queueCount(), client.prefNumGraphicsQueues)
                    }
                }
                if(client.prefNumComputeQueues>0 && isCompute(family.queueFlags())) {

                    if(queueFamilies.compute==-1) {
                        queueFamilies.compute = i
                        queueFamilies.numComputeQueues = Math.min(family.queueCount(), client.prefNumComputeQueues)
                    } else if(client.prefNumComputeQueues > queueFamilies.numComputeQueues &&
                              family.queueCount() > queueFamilies.numComputeQueues)
                    {
                        /** We need more queues and this family has more */
                        queueFamilies.compute = i
                        queueFamilies.numComputeQueues = Math.min(family.queueCount(), client.prefNumComputeQueues)
                    } else if(queueFamilies.numComputeQueues <= family.queueCount() && !isGraphics(family.queueFlags())) {
                        /** This family is dedicated and still has the right amount of queues */
                        queueFamilies.compute = i
                        queueFamilies.numComputeQueues = Math.min(family.queueCount(), client.prefNumComputeQueues)
                    }
                }
                if(client.prefNumTransferQueues>0 && isTransfer(family.queueFlags())) {

                    if(queueFamilies.transfer==-1 ||
                      (!isGraphics(family.queueFlags()) && !isCompute(family.queueFlags())))
                    {
                        queueFamilies.transfer = i
                        queueFamilies.numTransferQueues = Math.min(family.queueCount(), client.prefNumTransferQueues)
                    }
                }
            }
        }
        if(client.prefNumComputeQueues < queueFamilies.numComputeQueues) {
            log.warn("We could only find ${queueFamilies.numComputeQueues}. The client requested ${client.prefNumComputeQueues}")
        }
        /// Let the app make adjustments and validate
        //app.selectQueueFamilies(vprops, queueFamilyProps, queueFamily);
    }
    println("\t$queueFamilies")
}
fun selectSurfaceFormat(pDevice:VkPhysicalDevice, surface: VkSurfaceKHR, surfaceFormat: SurfaceFormat) {
    MemoryStack.stackPush().use { stack ->
        val formats = getFormats(stack, pDevice, surface)
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
fun canPresent(device:VkPhysicalDevice, surface:Long, queueFamilyIndex:Int):Boolean {
    MemoryStack.stackPush().use { stack ->
        val result = stack.mallocInt(1)

        vkGetPhysicalDeviceSurfaceSupportKHR(device, queueFamilyIndex, surface, result).check()
        return result.get(0) == 1
    }
}
fun VkPhysicalDevice.getProperties(): VkPhysicalDeviceProperties {
    val properties = VkPhysicalDeviceProperties.calloc()
    vkGetPhysicalDeviceProperties(this, properties)
    return properties
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