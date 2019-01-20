package vulkan.api

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSamplerCreateInfo
import vulkan.misc.check

class VkSampler(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroySampler(device, handle, null)
    }
}

fun VkDevice.createSampler(overrides:(VkSamplerCreateInfo)->Unit):VkSampler {
    val info = VkSamplerCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
        .magFilter(VK_FILTER_LINEAR)
        .minFilter(VK_FILTER_LINEAR)
        .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
        .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
        .compareEnable(false)
        .compareOp(VK_COMPARE_OP_ALWAYS)
        .mipLodBias(0f)
        .minLod(0f)
        .maxLod(0f)
        .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
        .anisotropyEnable(false)
        .maxAnisotropy(1f)
        .unnormalizedCoordinates(false)

    /** Allow the user to change some values */
    overrides(info)

    val pHandle = memAllocLong(1)
    vkCreateSampler(this, info, null, pHandle).check()

    val sampler = VkSampler(this, pHandle.get(0))
    memFree(pHandle)
    info.free()

    return sampler
}
