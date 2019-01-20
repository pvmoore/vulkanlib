package vulkan.texture

import vulkan.api.image.VkImage

data class Texture(
    val name:String,
    val width:Int,
    val height:Int,
    val channels:Int,
    val levels:Int,
    val image: VkImage
)
