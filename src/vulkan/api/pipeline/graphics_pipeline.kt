package vulkan.api.pipeline

import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.descriptor.VkDescriptorSetLayout
import vulkan.common.RenderContext
import vulkan.common.SpecConstants
import vulkan.common.Transferable
import vulkan.common.log
import vulkan.maths.string
import vulkan.misc.VkPrimitiveTopology
import vulkan.misc.VkShaderStageFlags
import kotlin.test.assertNull

class GraphicsPipeline {
    private lateinit var context:RenderContext

    private var descriptorSetLayouts:Array<VkDescriptorSetLayout>?  = null
    private var pushConstantRanges:VkPushConstantRange.Buffer?      = null

    private data class ShaderInfo(val filename:String, val specConstants: SpecConstants?)
    private var shaderProps    = mapOf<String,String>()
    private var shaderIncludes = listOf<String>()
    private val shaders        = HashMap<Int, ShaderInfo>()

    private var subpass = 0
    private val vertexInputState = VkPipelineVertexInputStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
    private val inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
    private val tessellationState = VkPipelineTessellationStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO)

    private val viewportState = VkPipelineViewportStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
    private val rasterisationState = VkPipelineRasterizationStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
        .polygonMode(VK_POLYGON_MODE_FILL)          // fill, line or point
        .cullMode(VK_CULL_MODE_NONE)                // none, front, back, front and back
        .frontFace(VK_FRONT_FACE_CLOCKWISE)         // counterclockwise or clockwise
        .depthBiasEnable(false)
        .rasterizerDiscardEnable(false)
        .depthClampEnable(false)
        .lineWidth(1f)
    private val multisampleState = VkPipelineMultisampleStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
        .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
    private val depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
    private val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
        .pAttachments(VkPipelineColorBlendAttachmentState.calloc(1).colorWriteMask(0xf))

    private var dynamicState:VkPipelineDynamicStateCreateInfo? = null

    lateinit var pipeline: VkPipeline
    lateinit var layout: VkPipelineLayout

    fun init(context: RenderContext) : GraphicsPipeline {
        this.context = context
        return this
    }
    fun destroy() {
        vertexInputState.free()
        inputAssemblyState.free()
        tessellationState.free()
        viewportState.free()
        rasterisationState.free()
        multisampleState.free()
        depthStencilState.free()
        colorBlendState.free()
        dynamicState?.free()

        pushConstantRanges?.free()
        layout.destroy()
        pipeline.destroy()
    }
    fun withShaderProperties(props:Map<String,String>, includes:List<String>):GraphicsPipeline {
        this.shaderProps    = props.toMap()
        this.shaderIncludes = includes.toList()
        return this
    }
    fun withShader(stage:VkShaderStageFlags, filename:String, specConstants: SpecConstants? = null):GraphicsPipeline {
        shaders[stage] = ShaderInfo(filename, specConstants)
        return this
    }
    fun withVertexInputState(vertex: Transferable, topology:VkPrimitiveTopology) : GraphicsPipeline {

        inputAssemblyState
            .topology(topology)
            .flags(0) // reserved
            .primitiveRestartEnable(false)

        val bindings = VkVertexInputBindingDescription.calloc(1)
            .binding(0)
            .stride(vertex.size())
            .inputRate(VK_VERTEX_INPUT_RATE_VERTEX) // vertex or instance

        val attribs = VkVertexInputAttributeDescription.calloc(vertex.javaClass.declaredFields.size)
        var offset  = 0

        vertex.javaClass.declaredFields.forEachIndexed { i, field ->

            val (format,fieldsize) = when(field.type.name) {
                "int"               -> VK_FORMAT_R32_SINT            to 4
                "float"             -> VK_FORMAT_R32_SFLOAT          to 4
                "org.joml.Vector2f" -> VK_FORMAT_R32G32_SFLOAT       to 2*4
                "org.joml.Vector3f" -> VK_FORMAT_R32G32B32_SFLOAT    to 3*4
                "org.joml.Vector4f" -> VK_FORMAT_R32G32B32A32_SFLOAT to 4*4
                "org.joml.Vector2i" -> VK_FORMAT_R32G32_SINT         to 2*4
                "org.joml.Vector3i" -> VK_FORMAT_R32G32B32_SINT      to 3*4
                "org.joml.Vector4i" -> VK_FORMAT_R32G32B32A32_SINT   to 4*4
                else -> throw Error("Unknown format for vertex field ${field.type.name}")
            }

            attribs[i].run {
                location(i)
                binding(0)
                format(format)
                offset(offset)
            }
            offset += fieldsize
        }

        vertexInputState
            .pVertexBindingDescriptions(bindings)
            .pVertexAttributeDescriptions(attribs)

        return this
    }
    fun withViewports(num:Int, callback:(VkViewport.Buffer)->Unit):GraphicsPipeline {

        val viewports = VkViewport.calloc(num)

        callback(viewports)

        viewportState
            .viewportCount(num)
            .pViewports(viewports)

        return this
    }
    fun withScissors(num:Int, callback:(VkRect2D.Buffer)->Unit):GraphicsPipeline {

        val scissors = VkRect2D.calloc(num)

        callback(scissors)

        viewportState
            .scissorCount(num)
            .pScissors(scissors)

        return this
    }
    fun withRasterisationState(callback:(VkPipelineRasterizationStateCreateInfo)->Unit):GraphicsPipeline {
        callback(rasterisationState)
        return this
    }
    fun withMultisampleState(callback:(VkPipelineMultisampleStateCreateInfo)->Unit):GraphicsPipeline {
        callback(multisampleState)
        return this
    }
    fun withDepthStencilState(callback:(VkPipelineDepthStencilStateCreateInfo)->Unit):GraphicsPipeline {
        callback(depthStencilState)
        return this
    }
    fun withColorBlendState(callback:(VkPipelineColorBlendStateCreateInfo)->Unit):GraphicsPipeline {
        callback(colorBlendState)
        return this
    }
    fun withStandardColorBlend() : GraphicsPipeline {
        val attachment = VkPipelineColorBlendAttachmentState.calloc(1)
            .blendEnable(true)
            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
            .colorBlendOp(VK_BLEND_OP_ADD)
            .alphaBlendOp(VK_BLEND_OP_ADD)
            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or
                            VK_COLOR_COMPONENT_G_BIT or
                            VK_COLOR_COMPONENT_B_BIT or
                            VK_COLOR_COMPONENT_A_BIT)

        colorBlendState.pAttachments(attachment)
        return this
    }
    fun withDynamicState(callback:(VkPipelineDynamicStateCreateInfo)->Unit):GraphicsPipeline {

        dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
        callback(dynamicState!!)

        return this
    }
    fun withSubpass(subpass:Int):GraphicsPipeline {
        this.subpass = subpass
        return this
    }

    fun withDSLayouts(dsLayouts:Array<VkDescriptorSetLayout>):GraphicsPipeline {
        this.descriptorSetLayouts = dsLayouts.copyOf()
        return this
    }

    /** Simple constant range */
    fun withPushConstants(firstIndex:Int, count:Int, stageFlags:VkShaderStageFlags):GraphicsPipeline {
        assertNull(pushConstantRanges)

        pushConstantRanges = VkPushConstantRange.calloc(1)
            .stageFlags(stageFlags)
            .offset(firstIndex*4)
            .size(count*4)

        return this
    }
    fun build():GraphicsPipeline {
        assert(vertexInputState.vertexBindingDescriptionCount()>0)
        assert(vertexInputState.vertexAttributeDescriptionCount()>0)

        /** Pipeline Layout */
        layout = context.device.createPipelineLayout(
            descriptorSetLayouts!!,
            pushConstantRanges
        )

        MemoryStack.stackPush().use { stack ->

            val win = context.vk.graphics.windowSize

            if(viewportState.viewportCount() == 0) {

                log.info("Creating standard viewport: ${win.string()}")

                val viewports = VkViewport.callocStack(1)
                    .x(0f)
                    .y(0f)
                    .width(win.x.toFloat())
                    .height(win.y.toFloat())

                viewportState
                    .viewportCount(1)
                    .pViewports(viewports)
            }
            if(viewportState.scissorCount()==0) {
                log.info("Creating standard scissor: ${win.string()}")

                val scissors = VkRect2D.callocStack(1)
                scissors.extent().set(win.x, win.y)

                viewportState
                    .scissorCount(1)
                    .pScissors(scissors)
            }


            val shaderStages = VkPipelineShaderStageCreateInfo.callocStack(shaders.size)

            var i = 0
            shaders.forEach { k, v ->
                val shader = context.vk.shaders.get(v.filename, shaderProps, shaderIncludes)

                val specInfo = v.specConstants?.let { specialisationInfo(it) }

                shaderStages[i++].run {
                    sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    flags(0) // reserved
                    stage(k)
                    module(shader.handle)
                    pName(stack.UTF8("main"))
                    pSpecializationInfo(specInfo)
                }
            }

            pipeline = context.device.createGraphicsPipeline(
                layout,
                context.renderPass,
                shaderStages,
                vertexInputState,
                inputAssemblyState,
                tessellationState,
                viewportState,
                rasterisationState,
                multisampleState,
                depthStencilState,
                colorBlendState,
                dynamicState
            )
        }

        return this
    }
}
