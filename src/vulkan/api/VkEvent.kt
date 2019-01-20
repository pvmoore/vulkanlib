package vulkan.api

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkEventCreateInfo
import vulkan.misc.check

class VkEvent(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroyEvent(device, handle, null)
    }
    fun signal() {
        vkSetEvent(device, handle).check()
    }
    fun reset() {
        vkResetEvent(device, handle).check()
    }
    /**
     * @return VK_EVENT_SET or VK_EVENT_RESET
     */
    fun getStatus():Int {
        return vkGetEventStatus(device, handle)
    }
}

fun VkDevice.createEvent():VkEvent {

    val info = VkEventCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_EVENT_CREATE_INFO)

    val pEvent = memAllocLong(1)
    vkCreateEvent(this, info, null, pEvent)

    val event = VkEvent(this, pEvent.get(0))

    info.free()
    memFree(pEvent)

    return event
}
