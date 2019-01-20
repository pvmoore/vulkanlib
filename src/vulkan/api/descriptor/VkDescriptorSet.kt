package vulkan.api.descriptor

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkWriteDescriptorSet
import vulkan.api.VkSampler
import vulkan.api.buffer.VkBuffer
import vulkan.api.image.VkImageView
import vulkan.api.pipeline.VkPipelineLayout
import vulkan.misc.VkDescriptorType
import vulkan.misc.VkImageLayout
import vulkan.misc.VkPipelineBindPoint
import java.nio.LongBuffer

class VkDescriptorSet(val handle:Long) {

    fun writeBuffer(write: VkWriteDescriptorSet.Buffer,
                    binding:Int,
                    type:VkDescriptorType,
                    buffers: VkDescriptorBufferInfo.Buffer,
                    arrayElement:Int=0)
    {
        write
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(handle)
            .descriptorType(type)
            .pBufferInfo(buffers)
            .dstBinding(binding)
            .dstArrayElement(arrayElement)
    }
    fun writeImage(write: VkWriteDescriptorSet.Buffer,
                   binding:Int,
                   type:VkDescriptorType,
                   images: VkDescriptorImageInfo.Buffer,
                   arrayElement:Int=0)
    {
        write
            .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
            .dstSet(handle)
            .descriptorType(type)
            .pImageInfo(images)
            .dstBinding(binding)
            .dstArrayElement(arrayElement)
    }
}

fun LongBuffer.put(a:Array<VkDescriptorSet>):LongBuffer {
    a.forEach { put(it.handle) }
    return this
}

fun descriptorBufferInfo(info:VkDescriptorBufferInfo.Buffer,
                         buffer: VkBuffer,
                         offset:Long,
                         size:Long = VK_WHOLE_SIZE)
{
    info.buffer(buffer.handle)
        .offset(offset)
        .range(size)
}
fun descriptorImageInfo(info:VkDescriptorImageInfo.Buffer,
                        view: VkImageView,
                        sampler: VkSampler,
                        layout:VkImageLayout)
{
    info.sampler(sampler.handle)
        .imageView(view.handle)
        .imageLayout(layout)
}

fun VkCommandBuffer.bindDescriptorSet(pipelineBindPoint:VkPipelineBindPoint,
                                      pipelineLayout: VkPipelineLayout,
                                      descriptorSet : VkDescriptorSet,
                                      firstSet:Int = 0)
    : VkCommandBuffer
{
    return bindDescriptorSets(pipelineBindPoint, pipelineLayout, firstSet, arrayOf(descriptorSet), intArrayOf())
}
fun VkCommandBuffer.bindDescriptorSets(pipelineBindPoint:VkPipelineBindPoint,
                                       pipelineLayout: VkPipelineLayout,
                                       firstSet:Int,
                                       descriptorSets:Array<VkDescriptorSet>,
                                       dynamicOffsets:IntArray = intArrayOf()
)
        : VkCommandBuffer
{
    MemoryStack.stackPush().use { stack ->

        val pDesriptorSets = stack.mallocLong(descriptorSets.size)
        descriptorSets.forEach { pDesriptorSets.put(it.handle) }
        pDesriptorSets.flip()

        val pDynamicOffsets = stack.mallocInt(dynamicOffsets.size)
        pDynamicOffsets.put(dynamicOffsets).flip()

        vkCmdBindDescriptorSets(this, pipelineBindPoint, pipelineLayout.handle, firstSet, pDesriptorSets, pDynamicOffsets)
        return this
    }
}