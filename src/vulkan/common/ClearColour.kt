package vulkan.common

import org.lwjgl.vulkan.VkClearValue
import vulkan.misc.RGBA

class ClearColour(private val rgba: RGBA, hasDepth:Boolean = false) {

    val value:VkClearValue.Buffer

    fun destroy() {
        value.free()
    }

    init{
        value = when(hasDepth) {
            true -> VkClearValue.calloc(2)
            else -> VkClearValue.calloc(1)
        }

        value[0].color {
            it.float32(0, rgba.r)
              .float32(1, rgba.g)
              .float32(2, rgba.b)
              .float32(3, rgba.a)
        }

        if(hasDepth) {
            value[1].depthStencil {
                it.depth(0f)      // 0.0 is furthest away, 1.0 is nearest
                it.stencil(0)
            }
        }
    }
}