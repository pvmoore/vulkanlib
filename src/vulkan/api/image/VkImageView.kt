package vulkan.api.image

import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkDestroyImageView
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkDevice
import vulkan.api.VkSampler
import vulkan.misc.VkImageLayout
import java.nio.LongBuffer

class VkImageView(private val device: VkDevice, var handle:Long) {
    fun destroy() {
        if(handle!= VK_NULL_HANDLE) {
            vkDestroyImageView(device, handle, null)
            handle = VK_NULL_HANDLE
        }
    }
    fun write(info: VkDescriptorImageInfo, layout:VkImageLayout, sampler: VkSampler?) {
        info.imageView(handle)
            .imageLayout(layout)
            .sampler(sampler?.handle ?: VK_NULL_HANDLE)
    }
}

fun LongBuffer.put(a:Array<VkImageView>): LongBuffer {
    a.forEach { put(it.handle) }
    return this
}