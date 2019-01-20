package vulkan.texture

import vulkan.common.KILOBYTE
import vulkan.misc.VkFormat
import vulkan.misc.translateVkFormat
import java.nio.ByteBuffer

data class RawImageData(
    val name:String,
    val data: ByteBuffer,
    val width:Int,
    val height:Int,
    val channels:Int,
    val levels:Int,
    val size:Int,
    val format: VkFormat)
{
    fun free() {
        ImageLoader.free(this)
    }

    override fun toString() = "RawImageData(\"$name\", ${width}x$height (${size/KILOBYTE} KB), $channels channels, format:${format.translateVkFormat()})"
}