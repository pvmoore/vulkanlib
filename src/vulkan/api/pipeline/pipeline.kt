package vulkan.api.pipeline

import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.system.MemoryUtil.memUTF8
import org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo
import org.lwjgl.vulkan.VkSpecializationInfo
import org.lwjgl.vulkan.VkSpecializationMapEntry
import vulkan.common.SpecConstants
import vulkan.misc.VkShaderStageFlags

/**
 * Constructs a VkSpecializationInfo given an object representing a block of specialisation constants
 * eg.
 * data class MySpecConstants(val a:Int, val b:Float, val c:Boolean) : SpecConstants()
 *
 * val buffer = specialisationConstants(MySpecConstants(0,1f,true))
 *
 * @return buffer and it's contents needs to be freed by the caller:
 *              info.pMapEntries()?.free()
 *              info.free()
 *
 * Note that specialisation constants can only be int, float or bool (4 bytes each).
 */
fun specialisationInfo(specConstants: SpecConstants):VkSpecializationInfo {

    println("numConstants = ${specConstants.numConstants}")

    val entries = VkSpecializationMapEntry.calloc(specConstants.numConstants)

    entries.forEachIndexed { i, e->
        e.constantID(i)
        e.offset(i*4)
        e.size(4)
    }

    return VkSpecializationInfo.calloc()
        .pMapEntries(entries)
        .pData(specConstants.byteBuffer)
}
fun shaderStage(shader:VkShaderModule,
                stage:VkShaderStageFlags,
                entry:String = "main",
                specConstants:VkSpecializationInfo? = null) : VkPipelineShaderStageCreateInfo
{
    // VK_SHADER_STAGE_VERTEX_BIT
    // VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT
    // VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT
    // VK_SHADER_STAGE_GEOMETRY_BIT
    // VK_SHADER_STAGE_FRAGMENT_BIT
    // VK_SHADER_STAGE_COMPUTE_BIT
    // VK_SHADER_STAGE_ALL_GRAPHICS
    // VK_SHADER_STAGE_ALL

    val pName = memUTF8(entry)

    val info = VkPipelineShaderStageCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
        .flags(0) // reserved
        .stage(stage)
        .module(shader.handle)
        .pName(pName)
        .pSpecializationInfo(specConstants)

    memFree(pName)

    return info
}