package vulkan.common

import org.lwjgl.vulkan.VkDevice
import vulkan.app.VulkanApplication
import vulkan.api.VkRenderPass

class RenderContext(
    val vk: VulkanApplication,
    val device: VkDevice,
    val renderPass: VkRenderPass
)