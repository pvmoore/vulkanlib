package vulkan.misc

import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.lwjgl.vulkan.EXTBlendOperationAdvanced.VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT
import org.lwjgl.vulkan.EXTConditionalRendering.*
import org.lwjgl.vulkan.EXTFragmentDensityMap.*
import org.lwjgl.vulkan.EXTImageDrmFormatModifier.VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT
import org.lwjgl.vulkan.EXTInlineUniformBlock.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK_EXT
import org.lwjgl.vulkan.EXTTransformFeedback.*
import org.lwjgl.vulkan.KHRSharedPresentableImage.VK_IMAGE_LAYOUT_SHARED_PRESENT_KHR
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR
import org.lwjgl.vulkan.NVMeshShader.*
import org.lwjgl.vulkan.NVRayTracing.*
import org.lwjgl.vulkan.NVShadingRateImage.*
import org.lwjgl.vulkan.NVXDeviceGeneratedCommands.*
import org.lwjgl.vulkan.VK11.*
import org.lwjgl.vulkan.VkExtent2D
import org.lwjgl.vulkan.VkExtent3D
import java.nio.IntBuffer

fun IntBuffer.string():String {
    val b = StringBuilder("(")
    for(i in 0 until this.limit()) {
        if(i>0) b.append(",")
        b.append(this.get(i))
    }
    return b.append(")").toString()
}

fun Vector2i.string():String = "(${this.x}, ${this.y})"
fun Vector2f.string():String = String.format("(%.4f, %.4f)", this.x, this.y)
fun Vector3i.string():String = "(${this.x}, ${this.y},${this.z})"
fun Vector3f.string():String = String.format("(%.4f, %.4f, %.4f)", this.x, this.y, this.z)

private fun Int.translateBits(map:Map<Int,String>):String {
    val buf = StringBuilder("[")
    var bits = this
    map.forEach { k, v ->
        if((bits and k)!=0) {
            bits = bits and k.inv()

            if(buf.length>1) buf.append(", ")
            buf.append(v)
        }
    }
    if(bits!=0) buf.append("UNKNOWN BIT")
    return buf.toString().trim() + "]"
}
fun VkExtent2D.translateVkEntent2D():String {
    return "(${this.width()},${this.height()})"
}
fun VkExtent3D.translateVkEntent3D():String {
    return "(${this.width()},${this.height()},${this.depth()})"
}

fun VkPresentModeKHR.translateVkPresentModeKHR():String = when(this) {
    VK_PRESENT_MODE_IMMEDIATE_KHR    -> "IMMEDIATE"
    VK_PRESENT_MODE_MAILBOX_KHR      -> "MAILBOX"
    VK_PRESENT_MODE_FIFO_KHR         -> "FIFO"
    VK_PRESENT_MODE_FIFO_RELAXED_KHR -> "FIFO_RELAXED"
    else -> "$this"
}
fun VkQueueFlags.translateVkQueueFlags():String {
    return this.translateBits(mapOf(
        VK_QUEUE_GRAPHICS_BIT       to "GRAPHICS",
        VK_QUEUE_COMPUTE_BIT        to "COMPUTE",
        VK_QUEUE_TRANSFER_BIT       to "TRANSFER",
        VK_QUEUE_SPARSE_BINDING_BIT to "SPARSE-BINDING",
        VK_QUEUE_PROTECTED_BIT      to "PROTECTED"
    ))
}
fun VkBufferUsageFlags.translateVkBufferUsageFlags():String {
    return this.translateBits(mapOf(
        VK_BUFFER_USAGE_TRANSFER_SRC_BIT                          to "TRANSFER_SRC",
        VK_BUFFER_USAGE_TRANSFER_DST_BIT                          to "TRANSFER_DST",
        VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT                  to "UNIFORM_TEXEL_BUFFER",
        VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT                  to "STORAGE_TEXEL_BUFFER",
        VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT                        to "UNIFORM_BUFFER",
        VK_BUFFER_USAGE_STORAGE_BUFFER_BIT                        to "STORAGE_BUFFER",
        VK_BUFFER_USAGE_INDEX_BUFFER_BIT                          to "INDEX_BUFFER",
        VK_BUFFER_USAGE_VERTEX_BUFFER_BIT                         to "VERTEX_BUFFER",
        VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT                       to "INDIRECT_BUFFER",
        VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT         to "TRANSFORM_FEEDBACK_BUFFER",
        VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_COUNTER_BUFFER_BIT_EXT to "TRANSFORM_FEEDBACK_COUNTER_BUFFER",
        VK_BUFFER_USAGE_CONDITIONAL_RENDERING_BIT_EXT             to "CONDITIONAL_RENDERING",
        VK_BUFFER_USAGE_RAY_TRACING_BIT_NV                        to "RAY_TRACING_BIT_NV"
    ))
}
fun VkImageUsageFlags.translateVkImageUsageFlags():String {
    return this.translateBits(mapOf(
        VK_IMAGE_USAGE_TRANSFER_SRC_BIT             to "TRANSFER_SRC",
        VK_IMAGE_USAGE_SAMPLED_BIT                  to "SAMPLED",
        VK_IMAGE_USAGE_STORAGE_BIT                  to "STORAGE",
        VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT         to "COLOR_ATTACHMENT",
        VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT to "DEPTH_STENCIL_ATTACHMENT",
        VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT     to "TRANSIENT_ATTACHMENT",
        VK_IMAGE_USAGE_INPUT_ATTACHMENT_BIT         to "INPUT_ATTACHMENT"
    ))
}
fun VkImageType.translateVkImageType():String = when(this) {
    VK_IMAGE_TYPE_1D -> "1D"
    VK_IMAGE_TYPE_2D -> "2D"
    VK_IMAGE_TYPE_3D -> "3D"
    else -> "$this"
}
fun VkImageViewType.translateVkImageViewType():String = when(this) {
    VK_IMAGE_VIEW_TYPE_1D         -> "1D"
    VK_IMAGE_VIEW_TYPE_2D         -> "2D"
    VK_IMAGE_VIEW_TYPE_3D         -> "3D"
    VK_IMAGE_VIEW_TYPE_CUBE       -> "CUBE"
    VK_IMAGE_VIEW_TYPE_1D_ARRAY   -> "1D_ARRAY"
    VK_IMAGE_VIEW_TYPE_2D_ARRAY   -> "2D_ARRAY"
    VK_IMAGE_VIEW_TYPE_CUBE_ARRAY -> "CUBE_ARRAY"
    else ->"$this"
}
fun VkFormat.translateVkFormat():String = when(this) {
    VK_FORMAT_UNDEFINED           -> "UNDEFINED"
    VK_FORMAT_R8_UNORM            -> "R8_UNORM"
    VK_FORMAT_R8G8B8_UNORM        -> "R8G8B8_UNORM"
    VK_FORMAT_R8G8B8A8_UNORM      -> "R8G8B8A8_UNORM"
    VK_FORMAT_R16_UINT            -> "R16_UINT"
    VK_FORMAT_R16_SINT            -> "R16_SINT"
    VK_FORMAT_R16_SFLOAT          -> "R16_SFLOAT"
    VK_FORMAT_R32_UINT            -> "R32_UINT"
    VK_FORMAT_R32_SINT            -> "R32_SINT"
    VK_FORMAT_R32G32_SINT         -> "R32G32_SINT"
    VK_FORMAT_R32G32B32_SINT      -> "R32G32B32_SINT"
    VK_FORMAT_R32G32B32A32_SINT   -> "R32G32B32A32_SINT"
    VK_FORMAT_R32_SFLOAT          -> "R32_SFLOAT"
    VK_FORMAT_R32G32_SFLOAT       -> "R32G32_SFLOAT"
    VK_FORMAT_R32G32B32_SFLOAT    -> "R32G32B32_SFLOAT"
    VK_FORMAT_R32G32B32A32_SFLOAT -> "R32G32B32A32_SFLOAT"
    VK_FORMAT_BC1_RGB_UNORM_BLOCK -> "BC1_RGB_UNORM_BLOCK"
    VK_FORMAT_BC2_UNORM_BLOCK     -> "BC2_UNORM_BLOCK"
    VK_FORMAT_BC3_UNORM_BLOCK     -> "BC3_UNORM_BLOCK"

    else -> "$this"
}
fun VkImageTiling.translateVkImageTiling():String = when(this) {
    VK_IMAGE_TILING_OPTIMAL -> "OPTIMAL"
    VK_IMAGE_TILING_LINEAR  -> "LINEAR"
    VK_IMAGE_TILING_DRM_FORMAT_MODIFIER_EXT -> "DRM_FORMAT_MODIFIER_EXT"
    else -> "$this"
}
fun VkImageAspectFlags.translateVkImageAspectFlags():String {
    return this.translateBits(mapOf(
        VK_IMAGE_ASPECT_COLOR_BIT    to "COLOR",
        VK_IMAGE_ASPECT_DEPTH_BIT    to "DEPTH",
        VK_IMAGE_ASPECT_STENCIL_BIT  to "STENCIL",
        VK_IMAGE_ASPECT_METADATA_BIT to "METADATA"
    ))
}
fun VkImageLayout.translateVkImageLayout():String = when(this) {
    VK_IMAGE_LAYOUT_UNDEFINED                                  -> "UNDEFINED"
    VK_IMAGE_LAYOUT_GENERAL                                    -> "GENERAL"
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL                   -> "COLOR_ATTACHMENT_OPTIMAL"
    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL           -> "DEPTH_STENCIL_ATTACHMENT_OPTIMAL"
    VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL            -> "DEPTH_STENCIL_READ_ONLY_OPTIMAL"
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL                   -> "SHADER_READ_ONLY_OPTIMAL"
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL                       -> "TRANSFER_SRC_OPTIMAL"
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL                       -> "TRANSFER_DST_OPTIMAL"
    VK_IMAGE_LAYOUT_PREINITIALIZED                             -> "PREINITIALIZED"
    VK_IMAGE_LAYOUT_DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL -> "DEPTH_READ_ONLY_STENCIL_ATTACHMENT_OPTIMAL"
    VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_STENCIL_READ_ONLY_OPTIMAL -> "DEPTH_ATTACHMENT_STENCIL_READ_ONLY_OPTIMAL"
    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR                            -> "PRESENT_SRC_KHR"
    VK_IMAGE_LAYOUT_SHARED_PRESENT_KHR                         -> "SHARED_PRESENT_KHR"
    VK_IMAGE_LAYOUT_SHADING_RATE_OPTIMAL_NV                    -> "SHADING_RATE_OPTIMAL_NV"
    VK_IMAGE_LAYOUT_FRAGMENT_DENSITY_MAP_OPTIMAL_EXT           -> "FRAGMENT_DENSITY_MAP_OPTIMAL_EXT"

    else -> "$this"
}
fun VkQueryType.translateVkQueryType():String = when(this) {
    VK_QUERY_TYPE_OCCLUSION                                 -> "OCCLUSION"
    VK_QUERY_TYPE_PIPELINE_STATISTICS                       -> "PIPELINE_STATISTICS"
    VK_QUERY_TYPE_TIMESTAMP                                 -> "TIMESTAMP"
    VK_QUERY_TYPE_TRANSFORM_FEEDBACK_STREAM_EXT             -> "TRANSFORM_FEEDBACK_STREAM_EXT"
    VK_QUERY_TYPE_ACCELERATION_STRUCTURE_COMPACTED_SIZE_NV  -> "ACCELERATION_STRUCTURE_COMPACTED_SIZE_NV"
    else -> "$this"
}
fun VkQueryPipelineStatisticFlags.translateVkQueryPipelineStatisticFlags():String {
    return this.translateBits(mapOf(
        VK_QUERY_PIPELINE_STATISTIC_INPUT_ASSEMBLY_VERTICES_BIT                    to "INPUT_ASSEMBLY_VERTICES",
        VK_QUERY_PIPELINE_STATISTIC_INPUT_ASSEMBLY_PRIMITIVES_BIT                  to "INPUT_ASSEMBLY_PRIMITIVES",
        VK_QUERY_PIPELINE_STATISTIC_VERTEX_SHADER_INVOCATIONS_BIT                  to "VERTEX_SHADER_INVOCATIONS",
        VK_QUERY_PIPELINE_STATISTIC_GEOMETRY_SHADER_INVOCATIONS_BIT                to "GEOMETRY_SHADER_INVOCATIONS",
        VK_QUERY_PIPELINE_STATISTIC_GEOMETRY_SHADER_PRIMITIVES_BIT                 to "GEOMETRY_SHADER_PRIMITIVES",
        VK_QUERY_PIPELINE_STATISTIC_CLIPPING_INVOCATIONS_BIT                       to "CLIPPING_INVOCATIONS",
        VK_QUERY_PIPELINE_STATISTIC_CLIPPING_PRIMITIVES_BIT                        to "CLIPPING_PRIMITIVES",
        VK_QUERY_PIPELINE_STATISTIC_FRAGMENT_SHADER_INVOCATIONS_BIT                to "FRAGMENT_SHADER_INVOCATIONS",
        VK_QUERY_PIPELINE_STATISTIC_TESSELLATION_CONTROL_SHADER_PATCHES_BIT        to "TESSELLATION_CONTROL_SHADER_PATCHES",
        VK_QUERY_PIPELINE_STATISTIC_TESSELLATION_EVALUATION_SHADER_INVOCATIONS_BIT to "TESSELLATION_EVALUATION_SHADER_INVOCATIONS",
        VK_QUERY_PIPELINE_STATISTIC_COMPUTE_SHADER_INVOCATIONS_BIT                 to "COMPUTE_SHADER_INVOCATIONS"
    ))
}
fun VkPipelineStageFlags.translateVkPipelineStageFlags():String {
    return this.translateBits(mapOf(
        VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT                     to "TOP_OF_PIPE",
        VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT                   to "DRAW_INDIRECT",
        VK_PIPELINE_STAGE_VERTEX_INPUT_BIT                    to "VERTEX_INPUT",
        VK_PIPELINE_STAGE_VERTEX_SHADER_BIT                   to "VERTEX_SHADER",
        VK_PIPELINE_STAGE_TESSELLATION_CONTROL_SHADER_BIT     to "TESSELLATION_CONTROL_SHADER",
        VK_PIPELINE_STAGE_TESSELLATION_EVALUATION_SHADER_BIT  to "TESSELLATION_EVALUATION_SHADER",
        VK_PIPELINE_STAGE_GEOMETRY_SHADER_BIT                 to "GEOMETRY_SHADER",
        VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT                 to "FRAGMENT_SHADER",
        VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT            to "EARLY_FRAGMENT_TESTS",
        VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT             to "LATE_FRAGMENT_TESTS",
        VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT         to "COLOR_ATTACHMENT_OUTPUT",
        VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT                  to "COMPUTE_SHADER",
        VK_PIPELINE_STAGE_TRANSFER_BIT                        to "TRANSFER",
        VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT                  to "BOTTOM_OF_PIPE",
        VK_PIPELINE_STAGE_HOST_BIT                            to "HOST",
        VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT                    to "ALL_GRAPHICS",
        VK_PIPELINE_STAGE_ALL_COMMANDS_BIT                    to "ALL_COMMANDS",
        VK_PIPELINE_STAGE_TRANSFORM_FEEDBACK_BIT_EXT          to "TRANSFORM_FEEDBACK",
        VK_PIPELINE_STAGE_CONDITIONAL_RENDERING_BIT_EXT       to "CONDITIONAL_RENDERING",
        VK_PIPELINE_STAGE_COMMAND_PROCESS_BIT_NVX             to "COMMAND_PROCESS",
        VK_PIPELINE_STAGE_SHADING_RATE_IMAGE_BIT_NV           to "SHADING_RATE_IMAGE",
        VK_PIPELINE_STAGE_RAY_TRACING_SHADER_BIT_NV           to "RAY_TRACING_SHADER",
        VK_PIPELINE_STAGE_ACCELERATION_STRUCTURE_BUILD_BIT_NV to "ACCELERATION_STRUCTURE_BUILD",
        VK_PIPELINE_STAGE_TASK_SHADER_BIT_NV                  to "TASK_SHADER",
        VK_PIPELINE_STAGE_MESH_SHADER_BIT_NV                  to "MESH_SHADER",
        VK_PIPELINE_STAGE_FRAGMENT_DENSITY_PROCESS_BIT_EXT    to "FRAGMENT_DENSITY_PROCESS"
    ))
}
fun VkAccessMaskFlags.translateVkAccessMaskFlags():String {
    return this.translateBits(mapOf(
        VK_ACCESS_INDIRECT_COMMAND_READ_BIT                 to "INDIRECT_COMMAND_READ",
        VK_ACCESS_INDEX_READ_BIT                            to "INDEX_READ",
        VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT                 to "VERTEX_ATTRIBUTE_READ",
        VK_ACCESS_UNIFORM_READ_BIT                          to "UNIFORM_READ",
        VK_ACCESS_INPUT_ATTACHMENT_READ_BIT                 to "INPUT_ATTACHMENT_READ",
        VK_ACCESS_SHADER_READ_BIT                           to "SHADER_READ",
        VK_ACCESS_SHADER_WRITE_BIT                          to "SHADER_WRITE",
        VK_ACCESS_COLOR_ATTACHMENT_READ_BIT                 to "COLOR_ATTACHMENT_READ",
        VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT                to "COLOR_ATTACHMENT_WRITE",
        VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT         to "DEPTH_STENCIL_ATTACHMENT_READ",
        VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT        to "DEPTH_STENCIL_ATTACHMENT_WRITE",
        VK_ACCESS_TRANSFER_READ_BIT                         to "TRANSFER_READ",
        VK_ACCESS_TRANSFER_WRITE_BIT                        to "TRANSFER_WRITE",
        VK_ACCESS_HOST_READ_BIT                             to "HOST_READ",
        VK_ACCESS_HOST_WRITE_BIT                            to "HOST_WRITE",
        VK_ACCESS_MEMORY_READ_BIT                           to "MEMORY_READ",
        VK_ACCESS_MEMORY_WRITE_BIT                          to "MEMORY_WRITE",
        VK_ACCESS_TRANSFORM_FEEDBACK_WRITE_BIT_EXT          to "TRANSFORM_FEEDBACK_WRITE",
        VK_ACCESS_TRANSFORM_FEEDBACK_COUNTER_READ_BIT_EXT   to "TRANSFORM_FEEDBACK_COUNTER_READ",
        VK_ACCESS_TRANSFORM_FEEDBACK_COUNTER_WRITE_BIT_EXT  to "TRANSFORM_FEEDBACK_COUNTER_WRITE",
        VK_ACCESS_CONDITIONAL_RENDERING_READ_BIT_EXT        to "CONDITIONAL_RENDERING_READ",
        VK_ACCESS_COMMAND_PROCESS_READ_BIT_NVX              to "COMMAND_PROCESS_READ",
        VK_ACCESS_COMMAND_PROCESS_WRITE_BIT_NVX             to "COMMAND_PROCESS_WRITE",
        VK_ACCESS_COLOR_ATTACHMENT_READ_NONCOHERENT_BIT_EXT to "COLOR_ATTACHMENT_READ_NONCOHERENT",
        VK_ACCESS_SHADING_RATE_IMAGE_READ_BIT_NV            to "SHADING_RATE_IMAGE_READ",
        VK_ACCESS_ACCELERATION_STRUCTURE_READ_BIT_NV        to "ACCELERATION_STRUCTURE_READ",
        VK_ACCESS_ACCELERATION_STRUCTURE_WRITE_BIT_NV       to "ACCELERATION_STRUCTURE_WRITE",
        VK_ACCESS_FRAGMENT_DENSITY_MAP_READ_BIT_EXT         to "FRAGMENT_DENSITY_MAP_READ"
    ))
}
fun VkMemoryPropertyFlags.translateVkMemoryPropertyFlags():String {
    return this.translateBits(mapOf(
        VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT     to "DEVICE_LOCAL",
        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT     to "HOST_VISIBLE",
        VK_MEMORY_PROPERTY_HOST_COHERENT_BIT    to "HOST_COHERENT",
        VK_MEMORY_PROPERTY_HOST_CACHED_BIT      to "HOST_CACHED",
        VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT to "LAZILY_ALLOCATED",
        VK_MEMORY_PROPERTY_PROTECTED_BIT        to "PROTECTED"
    ))
}
fun VkShaderStageFlags.translateVkShaderStageFlags() = when(this) {
    VK_SHADER_STAGE_VERTEX_BIT                  ->  "VERTEX"
    VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT    ->  "TESSELLATION_CONTROL"
    VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT ->  "TESSELLATION_EVALUATION"
    VK_SHADER_STAGE_GEOMETRY_BIT                ->  "GEOMETRY"
    VK_SHADER_STAGE_FRAGMENT_BIT                ->  "FRAGMENT"
    VK_SHADER_STAGE_COMPUTE_BIT                 ->  "COMPUTE"
    VK_SHADER_STAGE_ALL_GRAPHICS                ->  "ALL_GRAPHICS"
    VK_SHADER_STAGE_ALL                         ->  "ALL"
    VK_SHADER_STAGE_RAYGEN_BIT_NV               ->  "RAYGEN"
    VK_SHADER_STAGE_ANY_HIT_BIT_NV              ->  "ANY_HIT"
    VK_SHADER_STAGE_CLOSEST_HIT_BIT_NV          ->  "CLOSEST_HIT"
    VK_SHADER_STAGE_MISS_BIT_NV                 ->  "MISS"
    VK_SHADER_STAGE_INTERSECTION_BIT_NV         ->  "INTERSECTION"
    VK_SHADER_STAGE_CALLABLE_BIT_NV             ->  "CALLABLE"
    VK_SHADER_STAGE_TASK_BIT_NV                 ->  "TASK"
    VK_SHADER_STAGE_MESH_BIT_NV                 ->  "MESH"
    else -> "$this"
}
fun VkPipelineCreateFlags.translateVkPipelineCreateFlags() = translateBits(mapOf(
    VK_PIPELINE_CREATE_DISABLE_OPTIMIZATION_BIT         to   "DISABLE_OPTIMIZATION",
    VK_PIPELINE_CREATE_ALLOW_DERIVATIVES_BIT            to   "ALLOW_DERIVATIVES",
    VK_PIPELINE_CREATE_DERIVATIVE_BIT                   to   "DERIVATIVE",
    VK_PIPELINE_CREATE_VIEW_INDEX_FROM_DEVICE_INDEX_BIT to   "VIEW_INDEX_FROM_DEVICE_INDEX",
    VK_PIPELINE_CREATE_DISPATCH_BASE                    to   "DISPATCH_BASE",
    VK_PIPELINE_CREATE_DEFER_COMPILE_BIT_NV             to   "VDEFER_COMPILE"
))
fun VkSurfaceTransformFlags.translateVkSurfaceTransformFlags() = translateBits(mapOf(
    VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR                     to "IDENTITY",
    VK_SURFACE_TRANSFORM_ROTATE_90_BIT_KHR                    to "ROTATE_90",
    VK_SURFACE_TRANSFORM_ROTATE_180_BIT_KHR                   to "ROTATE_180",
    VK_SURFACE_TRANSFORM_ROTATE_270_BIT_KHR                   to "ROTATE_270",
    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_BIT_KHR            to "HORIZONTAL_MIRROR",
    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_90_BIT_KHR  to "HORIZONTAL_MIRROR_ROTATE_90",
    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_180_BIT_KHR to "HORIZONTAL_MIRROR_ROTATE_180",
    VK_SURFACE_TRANSFORM_HORIZONTAL_MIRROR_ROTATE_270_BIT_KHR to "HORIZONTAL_MIRROR_ROTATE_270",
    VK_SURFACE_TRANSFORM_INHERIT_BIT_KHR                      to "INHERIT"
))
fun VkDependencyFlags.translateVkDependencyFlags() = when(this) {
    VK_DEPENDENCY_BY_REGION_BIT    -> "BY_REGION"
    VK_DEPENDENCY_DEVICE_GROUP_BIT -> "DEVICE_GROUP"
    VK_DEPENDENCY_VIEW_LOCAL_BIT   -> "VIEW_LOCAL"
    else -> "$this"
}
fun VkImageViewCreateFlags.translateVkImageViewCreateFlags() = translateBits(mapOf(
    VK_IMAGE_VIEW_CREATE_FRAGMENT_DENSITY_MAP_DYNAMIC_BIT_EXT to "FRAGMENT_DENSITY_MAP_DYNAMIC"
))
fun VkPrimitiveTopology.translateVkPrimitiveTopology() = when(this) {
    VK_PRIMITIVE_TOPOLOGY_POINT_LIST                    -> "POINT_LIST"
    VK_PRIMITIVE_TOPOLOGY_LINE_LIST                     -> "LINE_LIST"
    VK_PRIMITIVE_TOPOLOGY_LINE_STRIP                    -> "LINE_STRIP"
    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST                 -> "TRIANGLE_LIST"
    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP                -> "TRIANGLE_STRIP"
    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_FAN                  -> "TRIANGLE_FAN"
    VK_PRIMITIVE_TOPOLOGY_LINE_LIST_WITH_ADJACENCY      -> "LINE_LIST_WITH_ADJACENCY"
    VK_PRIMITIVE_TOPOLOGY_LINE_STRIP_WITH_ADJACENCY     -> "LINE_STRIP_WITH_ADJACENCY"
    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST_WITH_ADJACENCY  -> "TRIANGLE_LIST_WITH_ADJACENCY"
    VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP_WITH_ADJACENCY -> "TRIANGLE_STRIP_WITH_ADJACENCY"
    VK_PRIMITIVE_TOPOLOGY_PATCH_LIST                    -> "PATCH_LIST"
    else -> "$this"
}
fun VkDescriptorType.translateVkDescriptorType() = when(this) {
    VK_DESCRIPTOR_TYPE_SAMPLER                   -> "SAMPLER"
    VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER    -> "COMBINED_IMAGE_SAMPLER"
    VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE             -> "SAMPLED_IMAGE"
    VK_DESCRIPTOR_TYPE_STORAGE_IMAGE             -> "STORAGE_IMAGE"
    VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER      -> "UNIFORM_TEXEL_BUFFER"
    VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER      -> "STORAGE_TEXEL_BUFFER"
    VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER            -> "UNIFORM_BUFFER"
    VK_DESCRIPTOR_TYPE_STORAGE_BUFFER            -> "STORAGE_BUFFER"
    VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC    -> "UNIFORM_BUFFER_DYNAMIC"
    VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC    -> "STORAGE_BUFFER_DYNAMIC"
    VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT          -> "INPUT_ATTACHMENT"
    VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK_EXT  -> "INLINE_UNIFORM_BLOCK_EXT"
    VK_DESCRIPTOR_TYPE_ACCELERATION_STRUCTURE_NV -> "ACCELERATION_STRUCTURE_NV"
    else -> "$this"
}