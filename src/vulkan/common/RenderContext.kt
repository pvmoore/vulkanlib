package vulkan.common

import org.lwjgl.vulkan.VkDevice
import vulkan.api.VkRenderPass
import vulkan.app.VulkanApplication

class RenderContext(
    val vk: VulkanApplication,
    val device: VkDevice,
    val renderPass: VkRenderPass,
    val buffers: VulkanBuffers
)