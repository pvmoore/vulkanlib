package vulkan.api

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkSemaphoreCreateInfo
import vulkan.misc.check
import java.nio.LongBuffer

class VkSemaphore(private val device:VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroySemaphore(device, handle, null)
    }
}

fun LongBuffer.put(a:Array<VkSemaphore>): LongBuffer {
    a.forEach { put(it.handle) }
    return this
}

fun VkDevice.createSemaphore():VkSemaphore {
    val info = VkSemaphoreCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)

    val pSemaphore = memAllocLong(1)
    vkCreateSemaphore(this, info, null, pSemaphore).check()

    val semaphore = VkSemaphore(this, pSemaphore.get(0))

    info.free()
    memFree(pSemaphore)

    return semaphore
}
