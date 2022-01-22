package vulkan.common

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import org.lwjgl.vulkan.VkDescriptorImageInfo
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkWriteDescriptorSet
import vulkan.api.VkSampler
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.VkBuffer
import vulkan.api.descriptor.*
import vulkan.api.image.VkImageView
import vulkan.misc.VkDescriptorType
import vulkan.misc.VkImageLayout
import vulkan.misc.VkShaderStageFlags

/**
 *  val d = Descriptors(vk)
 *      .createLayout()
 *          .storageImage(VShaderStage.FRAGMENT)
 *          .storageBuffer(VShaderStage.FRAGMENT)
 *          .combinedImageSampler(VShaderStage.FRAGMENT)
 *          .uniform(VShaderStage.VERTEX)
 *          .numSets(1)
 *      .createLayout()
 *          .uniform(VShaderStage.FRAGMENT)
 *          .numSets(1)
 *      .build()
 *
 *  d.layout(0).createSet()
 *      .add(view, layout)
 *      .add(buffer, offset, size)
 *      .add(sampler, view, layout)
 *      .add(buffer, offset, size)
 *      .write()
 *  d.layout(1).createSet()
 *      .add(buffer, offset, size)
 *      .write()
 *
 *  d.layout(0).dsLayout
 *  d.layout(0).set(0)
 *
 */
class Descriptors {
    private lateinit var context:RenderContext
    internal lateinit var pool : VkDescriptorPool

    private val layouts = ArrayList<Layout>()

    fun layout(index:Int) : Layout = layouts[index]
    fun allDSLayouts() : Array<VkDescriptorSetLayout> = layouts.map { l -> l.dsLayout }.toTypedArray()

    fun init(context : RenderContext) : Descriptors {
        this.context = context
        return this
    }
    fun destroy() {
        layouts.forEach { l->
            pool.freeSets(l.sets.map { s-> s.set }.toTypedArray())
            l.dsLayout.destroy()
        }
        pool.destroy()
    }
    fun createLayout() : Layout {
        val l = Layout()
        layouts.add(l)
        return l
    }
    fun build() : Descriptors {
        createPool()
        createLayouts()
        return this
    }

    //=================================================================================================
    internal data class Binding(val type:VkDescriptorType, val shaderStages:VkShaderStageFlags)

    inner class Layout {
        internal val bindings = ArrayList<Binding>()
        internal var numSets:Int = 0
        internal val sets = ArrayList<Set>()

        lateinit var dsLayout: VkDescriptorSetLayout

        fun set(index:Int): VkDescriptorSet = sets[index].set

        fun createSet() : Set {
            val set = Set(context.device, this, pool.allocSet(dsLayout))
            sets.add(set)
            return set
        }
        fun combinedImageSampler(shaderStages:VkShaderStageFlags) : Layout {
            bindings.add(Binding(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, shaderStages))
            return this
        }
        fun sampledImage(shaderStages:VkShaderStageFlags) : Layout {
            bindings.add(Binding(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE, shaderStages))
            return this
        }
        fun storageImage(shaderStages:VkShaderStageFlags) : Layout {
            bindings.add(Binding(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, shaderStages))
            return this
        }
        fun storageBuffer(shaderStages:VkShaderStageFlags) : Layout {
            bindings.add(Binding(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, shaderStages))
            return this
        }
        fun uniformBuffer(shaderStages:VkShaderStageFlags) : Layout {
            bindings.add(Binding(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, shaderStages))
            return this
        }
        fun numSets(n:Int) : Descriptors {
            numSets = n
            return this@Descriptors
        }
    }
    class Set(private val device:VkDevice,
              private val layout: Layout,
              val set: VkDescriptorSet
    )
    {
        private abstract class Descriptor
        private class BufferAllocDescriptor(val buffer: BufferAlloc) : Descriptor()
        private class BufferDescriptor(val buffer: VkBuffer, offset:Int, size:Int) : Descriptor()
        private open class ImageDescriptor(val imageView: VkImageView, val imageLayout:VkImageLayout) : Descriptor()
        private class ImageSamplerDescriptor(val imageView: VkImageView, val imageLayout:VkImageLayout, val sampler: VkSampler) : Descriptor()

        private val descriptors = ArrayList<Descriptor>()
        private var numBuffers = 0
        private var numImages  = 0

        fun add(imageView:VkImageView, imageLayout:VkImageLayout) : Set {
            descriptors.add(ImageDescriptor(imageView, imageLayout))
            numImages++
            return this
        }
        fun add(imageView:VkImageView, imageLayout:VkImageLayout, sampler:VkSampler) : Set {
            descriptors.add(ImageSamplerDescriptor(imageView, imageLayout, sampler))
            numImages++
            return this
        }
        fun add(buffer:VkBuffer, offset:Int, size:Int) : Set {
            descriptors.add(BufferDescriptor(buffer, offset, size))
            numBuffers++
            return this
        }
        fun add(buffer:BufferAlloc) : Set {
            descriptors.add(BufferAllocDescriptor(buffer))
            numBuffers++
            return this
        }
        fun write() : Set {
            assert(descriptors.size>0)

            MemoryStack.stackPush().use {

                val writes = VkWriteDescriptorSet.callocStack(descriptors.size)

                descriptors.forEachIndexed { i, d ->

                    val binding = layout.bindings[i]

                    writes[i]
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstBinding(i)
                        .dstSet(set.handle)
                        .descriptorType(binding.type)
                        .descriptorCount(1)
                        .dstArrayElement(0)

                    when(d) {
                        is BufferAllocDescriptor -> {
                            val info = VkDescriptorBufferInfo.callocStack(1)
                            d.buffer.write(info.first())
                            writes[i].pBufferInfo(info)
                        }
                        is BufferDescriptor -> {
                            val info = VkDescriptorBufferInfo.callocStack(1)
                            d.buffer.write(info.first())
                            writes[i].pBufferInfo(info)
                        }
                        is ImageSamplerDescriptor -> {
                            val info = VkDescriptorImageInfo.callocStack(1)
                            d.imageView.write(info.first(), d.imageLayout, d.sampler)
                            writes[i].pImageInfo(info)
                        }
                        is ImageDescriptor -> {
                            val info = VkDescriptorImageInfo.callocStack(1)
                            d.imageView.write(info.first(), d.imageLayout, null)
                            writes[i].pImageInfo(info)
                        }
                    }
                }
                vkUpdateDescriptorSets(device, writes, null)
            }

            return this
        }
    }
    //==========================================================================================
    private fun createPool() {

        val types = HashMap<VkDescriptorType,Int>()
        layouts.forEach { l->
            l.bindings.forEach { b->
                types.merge(b.type, l.numSets, Integer::sum)
            }
        }
        val maxSets = layouts.sumBy { it.numSets }

        //println("types = ${types.map { it.key.translateVkDescriptorType() +":" + it.value} }")
        //println("maxSets = $maxSets")

        pool = context.device.createDescriptorPool(types.size) { info, sizes ->
            info.maxSets(maxSets)


            types.entries.forEachIndexed { i, e->
                sizes[i].type(e.key)
                sizes[i].descriptorCount(e.value)

                //println("[$i] type:${e.key.translateVkDescriptorType()} count:${e.value}")
            }
        }
    }
    private fun createLayouts() {

        layouts.forEach { layout->
            layout.dsLayout = context.device.createDescriptorSetLayout(layout.bindings.size) { bindings ->

                layout.bindings.forEachIndexed { i, d->
                    bindings[i].run {
                        binding(i)
                        descriptorType(d.type)
                        descriptorCount(1)
                        stageFlags(d.shaderStages)
                    }
                }
            }
        }
    }
}
