package vulkan.common

import org.lwjgl.vulkan.VK10.*
import vulkan.misc.VkMemoryPropertyFlags
import vulkan.misc.isSet
import vulkan.misc.translateVkMemoryPropertyFlags

data class MemoryType(var index:Int = -1, var flags: VkMemoryPropertyFlags = 0) {
    val isSet get() = index != -1
    fun set(index:Int, flags: VkMemoryPropertyFlags) {
        this.index = index; this.flags = flags
    }
    val isDeviceLocal get()  = flags.isSet(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
    val isHostVisible get()  = flags.isSet(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT)
    val isHostCoherent get() = flags.isSet(VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)

    override fun toString() = "MemoryType(index=$index,flags=${flags.translateVkMemoryPropertyFlags()})"
}
