package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.Queues
import vulkan.common.log
import vulkan.misc.check


fun createLogicalDevice(physicalDevice:VkPhysicalDevice,
                        queues: Queues,
                        extensions:List<String>,
                        features: VkPhysicalDeviceFeatures)
    : VkDevice
{
    log.info("Creating logical device")

    MemoryStack.stackPush().use { stack ->

        val numQueues = queues.totalQueues()

        log.info("Requesting $numQueues queues")
        val queueCreateInfoBuffer = VkDeviceQueueCreateInfo.callocStack(numQueues)
        var index = 0
        val priorities = stack.mallocFloat(1).put(1.0f).flip()

        queues.getSelectedQueueFamilies().forEach { (family, count) ->

            (0 until count).forEach {
                queueCreateInfoBuffer[index++].run {
                    sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    queueFamilyIndex(family)
                    pQueuePriorities(priorities)
                }
            }
        }

        val enabledExtensions = stack.mallocPointer(extensions.size)
        extensions.forEach { enabledExtensions.put(stack.UTF8(it)) }
        enabledExtensions.flip()

        val deviceCreateInfo = VkDeviceCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
            .pNext(NULL)
            .pQueueCreateInfos(queueCreateInfoBuffer)
            .ppEnabledExtensionNames(enabledExtensions)
            .pEnabledFeatures(features)


        val pDevice = stack.mallocPointer(1)
        vkCreateDevice(physicalDevice, deviceCreateInfo, null, pDevice).check()

        return VkDevice(pDevice.get(0), physicalDevice, deviceCreateInfo)
    }
}
fun VkDevice.destroy() {
    vkDestroyDevice(this, null)
}
fun VkDevice.waitForIdle() {
    vkDeviceWaitIdle(this)
}
