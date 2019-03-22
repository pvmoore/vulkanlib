package vulkan.api

import org.joml.Vector2i
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkFramebufferCreateInfo
import vulkan.api.image.VkImageView
import vulkan.api.image.put
import vulkan.misc.check

class VkFrameBuffer(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroyFramebuffer(device, handle, null)
    }
}

fun VkDevice.createFrameBuffer(imageViews: Array<VkImageView>,
                               extent: Vector2i,
                               renderPass:VkRenderPass)
    :VkFrameBuffer
{

    val pAttachments =
        memAllocLong(imageViews.size)
            .put(imageViews)
            .flip()

    val info = VkFramebufferCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
        .pAttachments(pAttachments)
        .height(extent.x)
        .width(extent.y)
        .layers(1)
        .renderPass(renderPass.handle)

    val pFB = memAllocLong(1)

    vkCreateFramebuffer(this, info, null, pFB).check()

    val fb = VkFrameBuffer(this, pFB.get(0))

    info.free()
    memFree(pFB)
    memFree(pAttachments)

    return fb
}
