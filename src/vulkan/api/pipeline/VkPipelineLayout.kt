package vulkan.api.pipeline

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import vulkan.api.descriptor.VkDescriptorSetLayout
import vulkan.api.descriptor.put
import vulkan.misc.check

class VkPipelineLayout(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroyPipelineLayout(device, handle, null)
    }
}

fun VkDevice.createPipelineLayout(dsLayouts: Array<VkDescriptorSetLayout>,
                                  pushConstantRanges: VkPushConstantRange.Buffer?)
    :VkPipelineLayout
{
    val pDSLayouts = memAllocLong(dsLayouts.size)
        .put(dsLayouts)
        .flip()

    val info = VkPipelineLayoutCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
        .pSetLayouts(pDSLayouts)
        .pPushConstantRanges(pushConstantRanges)
        .flags(0) // reserved

    val pPLLayout = memAllocLong(1)
    vkCreatePipelineLayout(this, info, null, pPLLayout).check()

    val pipelineLayout = VkPipelineLayout(this, pPLLayout.get(0))

    info.free()
    memFree(pDSLayouts)
    memFree(pPLLayout)

    return pipelineLayout
}
