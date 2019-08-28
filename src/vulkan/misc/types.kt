package vulkan.misc

import org.joml.Vector3f
import org.joml.Vector4f
import java.nio.ByteBuffer

inline class QueueFamily(val index:Int)

typealias VkAccessMaskFlags             = Int
typealias VkBufferUsageFlags            = Int
typealias VkColorSpaceKHR               = Int
typealias VkDependencyFlags             = Int
typealias VkDescriptorType              = Int
typealias VkFilter                      = Int
typealias VkFormat                      = Int
typealias VkImageAspectFlags            = Int
typealias VkImageLayout                 = Int
typealias VkImageTiling                 = Int
typealias VkImageType                   = Int
typealias VkImageUsageFlags             = Int
typealias VkImageViewCreateFlags        = Int
typealias VkImageViewType               = Int
typealias VkMemoryPropertyFlags         = Int
typealias VkPipelineBindPoint           = Int
typealias VkPipelineCreateFlags         = Int
typealias VkPipelineStageFlags          = Int
typealias VkPresentModeKHR              = Int
typealias VkPrimitiveTopology           = Int
typealias VkQueueFlags                  = Int
typealias VkQueryControlFlags           = Int
typealias VkQueryPipelineStatisticFlags = Int
typealias VkQueryResultFlags            = Int
typealias VkQueryType                   = Int
typealias VkShaderStageFlags            = Int
typealias VkSharingMode                 = Int
typealias VkSurfaceTransformFlags       = Int
typealias VkSwapchainKHR                = Long
typealias VkSurfaceKHR                  = Long

val BLACK  = RGBA(0f,0f,0f,1f)
val WHITE  = RGBA(1f,1f,1f,1f)
val RED    = RGBA(1f,0f,0f,1f)
val GREEN  = RGBA(0f,1f,0f,1f)
val BLUE   = RGBA(0f,0f,1f,1f)
val YELLOW = RGBA(1f,1f,0f,1f)

data class RGBA(val r:Float, val g:Float, val b:Float, val a:Float) {
    constructor(r:Float, g:Float, b:Float) : this(r, g, b, 1f)
    constructor(v: Vector4f) : this(v.x, v.y, v.z, v.w)

    fun gamma(f:Float):RGBA = RGBA(r*f,g*f,b*f,a)

    operator fun times(f:Float) : RGBA {
        return RGBA(r*f,g*f,b*f,a*f)
    }
    operator fun plus(f:Float) : RGBA {
        return RGBA(r+f,g+f,b+f,a+f)
    }
    fun blend(o : RGBA) : RGBA {
        return RGBA((r + o.r) / 2, (g + o.g) / 2, (b + o.b) / 2, (a + o.a) / 2)
    }
    fun alpha(a : Float) : RGBA {
        return RGBA(r, g, b, a)
    }
}
fun RGBA.toVector3f() = Vector3f(r,g,b)
fun RGBA.toVector4f() = Vector4f(r,g,b,a)

fun ByteBuffer.put(c:RGBA) {
    this.putFloat(c.r)
    this.putFloat(c.g)
    this.putFloat(c.b)
    this.putFloat(c.a)
}
fun Vector4f.set(c:RGBA) {
    this.x = c.r
    this.y = c.g
    this.z = c.b
    this.w = c.a
}
//==================================================================================================
