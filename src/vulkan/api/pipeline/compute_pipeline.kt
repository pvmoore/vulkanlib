package vulkan.api.pipeline

import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_COMPUTE_BIT
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkPushConstantRange
import org.lwjgl.vulkan.VkSpecializationInfo
import vulkan.api.descriptor.VkDescriptorSetLayout
import vulkan.common.RenderContext
import vulkan.common.SpecConstants
import kotlin.test.assertNull

class ComputePipeline(private val context:RenderContext) {
    private val device: VkDevice = context.vk.device

    private var descriptorSetLayouts:Array<VkDescriptorSetLayout>? = null
    private var pushConstantRanges:VkPushConstantRange.Buffer? = null
    private var shaderFilename        = ""
    private var specialisationInfo: VkSpecializationInfo? = null

    lateinit var pipeline: VkPipeline
    lateinit var layout: VkPipelineLayout

    init{

    }
    fun destroy() {
        println("Destroying ComputePipeline")
        specialisationInfo?.let {
            it.pMapEntries()?.free()
            it.free()
        }
        pushConstantRanges?.free()
        layout?.destroy()
        pipeline?.destroy()
    }
    fun withDSLayouts(dsLayouts:Array<VkDescriptorSetLayout>):ComputePipeline {
        this.descriptorSetLayouts = dsLayouts.copyOf()
        return this
    }
    fun withShader(filename:String, specConstants: SpecConstants? = null):ComputePipeline {
        this.shaderFilename = filename

        specConstants?.let {
            this.specialisationInfo = specialisationInfo(specConstants)
        }
        return this
    }
    /** Simple constant range */
    fun withPushConstants(firstIndex:Int, count:Int):ComputePipeline {
        assertNull(pushConstantRanges)

        pushConstantRanges = VkPushConstantRange.calloc(1)
            .stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
            .offset(firstIndex*4)
            .size(count*4)

        return this
    }
    /** @param ranges List of memoryOffset and size */
    fun withPushConstantRanges(ranges:List<Pair<Int,Int>>):ComputePipeline {
        assertNull(pushConstantRanges)

        pushConstantRanges = VkPushConstantRange.calloc(ranges.size)

        pushConstantRanges!!.forEachIndexed { i, info ->
            info.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT)
            info.offset(ranges[i].first)
            info.size(ranges[i].second)
        }

        return this
    }
    fun build():ComputePipeline {
        assert(shaderFilename != "")
        assert(descriptorSetLayouts!!.isNotEmpty())

        /** Pipeline Layout */
        layout = device.createPipelineLayout(
            descriptorSetLayouts!!,
            pushConstantRanges
        )

        /** Compute Shader */
        val shader = context.vk.shaders.get(shaderFilename, emptyMap(), listOf())
        val pName = memUTF8("main")

        val shaderStage = VkPipelineShaderStageCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
            .flags(0) // reserved
            .stage(VK_SHADER_STAGE_COMPUTE_BIT)
            .module(shader.handle)
            .pName(pName)
            .pSpecializationInfo(specialisationInfo)


        pipeline = device.createComputePipeline(layout, shaderStage)

        memFree(pName)
        shaderStage.free()
        shader.destroy()

        return this
    }
}
