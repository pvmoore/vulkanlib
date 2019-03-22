package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.VK10.*
import vulkan.misc.VkFormat
import vulkan.misc.check

class VkRenderPass(private val device: VkDevice, val handle:Long) {
    fun destroy() {
        vkDestroyRenderPass(device, handle, null)
    }
}

/**
 * Creates a standard render pass with a color attachment and a depth attachment
 */
fun VkDevice.createColorAndDepthRenderPass(colorFormat:VkFormat, depthFormat:VkFormat) : VkRenderPass {
    MemoryStack.stackPush().use { stack ->
        val attachmentDescs = VkAttachmentDescription.callocStack(2)

        // color attachment
        attachmentDescs[0]
            .format(colorFormat)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)

        // depth attachment
        attachmentDescs[1]
            .format(depthFormat)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val colorAttachmentRefs = VkAttachmentReference.callocStack(1)
            .attachment(0)
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val depthAttachmentRef = VkAttachmentReference.callocStack()
            .attachment(1)
            .layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)

        val subpasses = VkSubpassDescription.callocStack(1)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(1)
            .pColorAttachments(colorAttachmentRefs)
            .pDepthStencilAttachment(depthAttachmentRef)

        val deps = VkSubpassDependency.callocStack(2)
        deps[0]
            .srcSubpass(VK_SUBPASS_EXTERNAL)
            .dstSubpass(0)
            .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
            .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)
        deps[1]
            .srcSubpass(0)
            .dstSubpass(VK_SUBPASS_EXTERNAL)
            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
            .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
            .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)

        val info = VkRenderPassCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(attachmentDescs)
            .pSubpasses(subpasses)
            .pDependencies(deps)

        val pRenderPass = stack.mallocLong(1)
        vkCreateRenderPass(this, info, null, pRenderPass).check()

        return VkRenderPass(this, pRenderPass.get(0))
    }
}

fun VkDevice.createRenderPass(numAttachments:Int = 1,
                              numSubpasses:Int   = 1,
                              colorFormat:VkFormat,
                              edits:(VkRenderPassCreateInfo)->Unit)
    : VkRenderPass
{
    MemoryStack.stackPush().use { stack ->

        val attachmentDescs = VkAttachmentDescription.callocStack(numAttachments)
            .format(colorFormat)
            .samples(VK_SAMPLE_COUNT_1_BIT)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
            .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            //.initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            //.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)


        val attachmentRef = VkAttachmentReference.callocStack(numAttachments)
            .attachment(0)
            .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

        val subpasses = VkSubpassDescription.callocStack(numSubpasses)
            .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
            .colorAttachmentCount(attachmentRef.remaining())
            .pColorAttachments(attachmentRef)

        val deps = VkSubpassDependency.callocStack(2)
        deps[0]
            .srcSubpass(VK_SUBPASS_EXTERNAL)
            .dstSubpass(0)
            .srcStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
            .srcAccessMask(VK_ACCESS_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)
        deps[1]
            .srcSubpass(0)
            .dstSubpass(VK_SUBPASS_EXTERNAL)
            .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT or VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT)
            .dstAccessMask(VK_ACCESS_MEMORY_READ_BIT)
            .dependencyFlags(VK_DEPENDENCY_BY_REGION_BIT)

        val info = VkRenderPassCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
            .pAttachments(attachmentDescs)
            .pSubpasses(subpasses)
            .pDependencies(deps)

        edits(info)

        val pRenderPass = stack.mallocLong(1)
        vkCreateRenderPass(this, info, null, pRenderPass).check()

        return VkRenderPass(this, pRenderPass.get(0))
    }
}
