package vulkan.common

import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.VkFence
import vulkan.api.VkFrameBuffer
import vulkan.api.VkSemaphore
import vulkan.api.image.VkImage
import vulkan.api.image.VkImageView

data class PerFrameResource(
    val index: Int,                 /** Frame resource index */
    val image: VkImage,             /** Current swapchain image */
    val imageView: VkImageView,     /** Current swapchain image view */
    val frameBuffer: VkFrameBuffer, /** Current framebuffer */
    val adhocCB: VkCommandBuffer,   /** Use this for adhoc commands per frame on the graphics queue */
    val imageAvailable: VkSemaphore,
    val renderFinished: VkSemaphore,
    val fence: VkFence
)
