package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.QueueFamilies
import vulkan.common.log
import vulkan.misc.check


fun createLogicalDevice(physicalDevice:VkPhysicalDevice,
                        queueFamilies: QueueFamilies,
                        extensions:List<String>,
                        features: VkPhysicalDeviceFeatures)
    : VkDevice
{
    log.info("Creating logical device")

    MemoryStack.stackPush().use { stack ->

        val numQueues = queueFamilies.numGraphicsQueues +
                        queueFamilies.numComputeQueues +
                        queueFamilies.numTransferQueues

        log.info("Requesting $numQueues queues")
        val queueCreateInfoBuffer = VkDeviceQueueCreateInfo.callocStack(numQueues)
        var index = 0
        val priorities = stack.mallocFloat(1).put(1.0f).flip()

        log.info("Requesting ${queueFamilies.numTransferQueues} transfer queues")
        (0 until queueFamilies.numTransferQueues).forEach {
            queueCreateInfoBuffer[index++].let { q->
                q.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                q.queueFamilyIndex(queueFamilies.transfer)
                q.pQueuePriorities(priorities)
            }
        }

        log.info("Requesting ${queueFamilies.numGraphicsQueues} graphics queues")
        (0 until queueFamilies.numGraphicsQueues).forEach {
            queueCreateInfoBuffer[index++].let { q->
                q.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                q.queueFamilyIndex(queueFamilies.graphics)
                q.pQueuePriorities(priorities)
            }
        }

        log.info("Requesting ${queueFamilies.numComputeQueues} compute queues")
        (0 until queueFamilies.numComputeQueues).forEach {
            queueCreateInfoBuffer[index++].let { q->
                q.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                q.queueFamilyIndex(queueFamilies.compute)
                q.pQueuePriorities(priorities)
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
