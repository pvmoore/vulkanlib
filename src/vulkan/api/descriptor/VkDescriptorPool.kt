package vulkan.api.descriptor

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.misc.check

class VkDescriptorPool(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroyDescriptorPool(device, handle, null)
    }
    fun reset() {
        vkResetDescriptorPool(device, handle, 0).check()
    }
    fun allocSet(layout:VkDescriptorSetLayout) : VkDescriptorSet {
        return allocSets(arrayOf(layout)).first()
    }
    fun allocSets(layouts:Array<VkDescriptorSetLayout>):Array<VkDescriptorSet> {
        MemoryStack.stackPush().use { stack ->

            val pLayouts = stack.mallocLong(layouts.size).put(layouts).flip()

            val info = VkDescriptorSetAllocateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(handle)
                .pSetLayouts(pLayouts)

            val pSets = stack.mallocLong(layouts.size)
            vkAllocateDescriptorSets(device, info, pSets).check()

            return (0 until pSets.limit()).map{ VkDescriptorSet(pSets.get(it)) }.toTypedArray()
        }
    }
    fun freeSet(set:VkDescriptorSet) {
        freeSets(arrayOf(set))
    }
    fun freeSets(sets:Array<VkDescriptorSet>) {
        val ptr = memAllocLong(sets.size).put(sets).flip()
        vkFreeDescriptorSets(device, handle, ptr).check()
        memFree(ptr)
    }
    fun updateSets(writes: VkWriteDescriptorSet.Buffer, copies: VkCopyDescriptorSet.Buffer) {
        vkUpdateDescriptorSets(device, writes, copies)
    }
}

/**
 * eg.
 * device.createDescriptorPool(1) { info, sizes ->
 *      info.maxSets(1)
 *      info.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
 *      sizes[0].type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
 *      size[0].descriptorCount(1)
 * }
 */
fun VkDevice.createDescriptorPool(numSizes:Int,
                                  callback:(info:VkDescriptorPoolCreateInfo,
                                            sizes:VkDescriptorPoolSize.Buffer)->Unit)
    : VkDescriptorPool
{
    MemoryStack.stackPush().use { stack ->

        val sizes = VkDescriptorPoolSize.callocStack(numSizes)

        // Possible flags
        //      VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT
        //      VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT_EXT

        /** Set default values */
        val info = VkDescriptorPoolCreateInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
            .pPoolSizes(sizes)
            .maxSets(0)
            .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)

        /** Allow client to set values */
        callback(info, sizes)

        /** Ensure maxSets has been set */
        assert(info.maxSets() > 0)

        val pPool = stack.mallocLong(1)
        vkCreateDescriptorPool(this, info, null, pPool).check()

        return VkDescriptorPool(this, pPool.get(0))
    }
}
