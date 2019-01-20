package vulkan.api.pipeline

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.VkRenderPass
import vulkan.misc.VkPipelineCreateFlags
import vulkan.misc.check

class VkPipeline(private val device: VkDevice, val handle:Long) {
    fun destroy() {
        vkDestroyPipeline(device, handle, null)
    }
}

fun VkDevice.createGraphicsPipeline(layout:VkPipelineLayout,
                                    renderPass: VkRenderPass,
                                    shaderStages:VkPipelineShaderStageCreateInfo.Buffer,
                                    vertexInputState:VkPipelineVertexInputStateCreateInfo,
                                    inputAssemblyState:VkPipelineInputAssemblyStateCreateInfo,
                                    tessellationState:VkPipelineTessellationStateCreateInfo,
                                    viewportState:VkPipelineViewportStateCreateInfo,
                                    rasterisationState:VkPipelineRasterizationStateCreateInfo,
                                    multisampleState:VkPipelineMultisampleStateCreateInfo,
                                    depthStencilState:VkPipelineDepthStencilStateCreateInfo,
                                    colorBlendState:VkPipelineColorBlendStateCreateInfo,
                                    dynamicState:VkPipelineDynamicStateCreateInfo?,
                                    flags:VkPipelineCreateFlags = 0,
                                    subpass:Int = 0)
    : VkPipeline
{
    // VkPipelineCreateFlags:
    //    VK_PIPELINE_CREATE_DISABLE_OPTIMIZATION_BIT
    //    VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT
    //    VK_PIPELINE_CREATE_DERIVATIVE_BIT
    //    VK_PIPELINE_CREATE_VIEW_INDEX_FROM_DEVICE_INDEX_BIT
    //    VK_PIPELINE_CREATE_DISPATCH_BASE
    //    VK_PIPELINE_CREATE_DEFER_COMPILE_BIT_NV

    val createInfo = VkGraphicsPipelineCreateInfo.calloc(1)
        .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
        .flags(flags)
        .pStages(shaderStages)
        .pVertexInputState(vertexInputState)
        .pInputAssemblyState(inputAssemblyState)
        .pTessellationState(tessellationState)
        .pViewportState(viewportState)
        .pRasterizationState(rasterisationState)
        .pMultisampleState(multisampleState)
        .pDepthStencilState(depthStencilState)
        .pColorBlendState(colorBlendState)
        .pDynamicState(dynamicState)
        .layout(layout.handle)
        .renderPass(renderPass.handle)
        .subpass(subpass)
        .basePipelineHandle(0)
        .basePipelineIndex(-1)


    // VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST

//    val inputAssemblyState = VkPipelineInputAssemblyStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
//        .topology(topology)
//        .flags(0) // reserved
//        .primitiveRestartEnable(false)
//
//    val viewportState = VkPipelineViewportStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
//        .viewportCount(1)
//        .scissorCount(1)
//        .pViewports(viewports)
//        .pScissors(scissors)
//        .flags(0) // reserved
//
//    val rasterisationState = VkPipelineRasterizationStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
//        .flags(0) // reserved
//        .polygonMode(VK_POLYGON_MODE_FILL)          // fill, line or point
//        .cullMode(VK_CULL_MODE_NONE)                // none, front, back, front and back
//        .frontFace(VK_FRONT_FACE_CLOCKWISE)         // counterclockwise or clockwise
//        .depthBiasEnable(false)
//        .rasterizerDiscardEnable(false)
//        .depthClampEnable(false)
//        .lineWidth(1f)
//
//    val multisampleState = VkPipelineMultisampleStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
//        .flags(0)   // reserved
//        .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)    // 1-> 64
//
//    /** Disabled blend */
//    val colorWriteMask = VkPipelineColorBlendAttachmentState.calloc(1)
//        .blendEnable(false)
//        .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
//        .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
//        .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
//        .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
//        .colorBlendOp(VK_BLEND_OP_ADD)
//        .alphaBlendOp(VK_BLEND_OP_ADD)
//        .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or
//                        VK_COLOR_COMPONENT_G_BIT or
//                        VK_COLOR_COMPONENT_B_BIT or
//                        VK_COLOR_COMPONENT_A_BIT)
//
//    val colorBlendState = VkPipelineColorBlendStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
//        .logicOp(VK_LOGIC_OP_COPY)
//        .pAttachments(colorWriteMask)

//    val pDynamicStates = memAllocInt(2)
//    pDynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT)
//                  .put(VK_DYNAMIC_STATE_SCISSOR)
//                  .flip()
//    val dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
//        .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
//        .pDynamicStates(pDynamicStates)





    val pPipeline = memAllocLong(1)
    vkCreateGraphicsPipelines(this, 0, createInfo, null, pPipeline).check()

    val pipeline = VkPipeline(this, pPipeline.get(0))

    memFree(pPipeline)
    createInfo.free()

//    inputAssemblyState.free()
//    viewportState.free()
//    rasterisationState.free()
//    multisampleState.free()
//    colorWriteMask.free()
//    colorBlendState.free()


    return pipeline
}
fun VkDevice.createComputePipeline(layout:VkPipelineLayout,
                                   shaderStage: VkPipelineShaderStageCreateInfo)
    : VkPipeline
{
    // VkPipelineCreateFlags:
    //    VK_PIPELINE_CREATE_DISABLE_OPTIMIZATION_BIT
    //    VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT
    //    VK_PIPELINE_CREATE_DERIVATIVE_BIT
    //    VK_PIPELINE_CREATE_VIEW_INDEX_FROM_DEVICE_INDEX_BIT
    //    VK_PIPELINE_CREATE_DISPATCH_BASE
    //    VK_PIPELINE_CREATE_DEFER_COMPILE_BIT_NV

    val info = VkComputePipelineCreateInfo.calloc(1)
        .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
        .flags(0)
        .stage(shaderStage)
        .layout(layout.handle)
        .basePipelineHandle(0)
        .basePipelineIndex(-1)

    val pPipeline = memAllocLong(1)
    vkCreateComputePipelines(this, 0, info, null, pPipeline).check()

    val pipeline = VkPipeline(this, pPipeline.get(0))

    memFree(pPipeline)
    info.free()

    return pipeline
}

fun VkCommandBuffer.bindPipeline(p:GraphicsPipeline):VkCommandBuffer {
    return this.bindGraphicsPipeline(p.pipeline)
}
fun VkCommandBuffer.bindPipeline(p:ComputePipeline):VkCommandBuffer {
    return this.bindComputePipeline(p.pipeline)
}
fun VkCommandBuffer.bindGraphicsPipeline(pipeline: VkPipeline):VkCommandBuffer {
    vkCmdBindPipeline(this, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.handle)
    return this
}
fun VkCommandBuffer.bindComputePipeline(pipeline: VkPipeline):VkCommandBuffer {
    vkCmdBindPipeline(this, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.handle)
    return this
}