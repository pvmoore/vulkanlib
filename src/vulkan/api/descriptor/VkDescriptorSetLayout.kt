package vulkan.api.descriptor

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo
import org.lwjgl.vulkan.VkDevice
import java.nio.LongBuffer

class VkDescriptorSetLayout(private val device:
                            VkDevice, val handle:Long)
{
    fun destroy() {
        vkDestroyDescriptorSetLayout(device, handle, null)
    }
}

fun LongBuffer.put(a:Array<VkDescriptorSetLayout>):LongBuffer {
    a.forEach { put(it.handle) }
    return this
}

/**
 * eg.
 * device.createDescriptorSetLayout(4) { bindings ->
 *  bindings[0]
 *      .binding(0)
 *      .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
 *      .descriptorCount(1)
 *      .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
 *      .pImmutableSamplers(null)
 * bindings[1]
 *      .binding(1)
 *      .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
 *      .descriptorCount(1)
 *      .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
 * bindings[2]
 *      .binding(2)
 *      .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER)
 *      .descriptorCount(1)
 *      .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
 * bindings[3]
 *      .binding(3)
 *      .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
 *      .descriptorCount(2)                             // create 2 of these
 *      .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
 */
fun VkDevice.createDescriptorSetLayout(numBindings:Int,
                                       overrides:(bindings:VkDescriptorSetLayoutBinding.Buffer)->Unit)
    : VkDescriptorSetLayout
{
    MemoryStack.stackPush().use { stack ->

        val bindings = VkDescriptorSetLayoutBinding.callocStack(numBindings)

        val info = VkDescriptorSetLayoutCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
            .flags(0)
            .pBindings(bindings)

        overrides(bindings)

        val pLayout = stack.mallocLong(1)
        vkCreateDescriptorSetLayout(this, info, null, pLayout)

        return VkDescriptorSetLayout(this, pLayout.get(0))
    }
}


