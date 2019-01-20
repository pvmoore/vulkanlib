package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkFenceCreateInfo
import vulkan.misc.check
import java.nio.LongBuffer

class VkFence(private val device:VkDevice, val handle:Long) {

    fun reset() {
        val pFences = memAllocLong(1).put(handle).flip()
        vkResetFences(device, pFences)
        memFree(pFences)
    }
    fun isSignalled():Boolean {
        return VK_SUCCESS==vkGetFenceStatus(device, handle)
    }
    fun waitFor(timeoutNanos:Long = -1):Boolean {
        return device.waitFor(arrayOf(this), true, timeoutNanos)
    }
    fun destroy() {
        vkDestroyFence(device, handle, null)
    }
}

fun LongBuffer.put(a:Array<VkFence>): LongBuffer {
    a.forEach { put(it.handle) }
    return this
}

fun VkDevice.createFence(signalled:Boolean = false):VkFence {
    val info = VkFenceCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
        .flags(if(signalled) VK_FENCE_CREATE_SIGNALED_BIT else 0)

    val pFence = memAllocLong(1)
    vkCreateFence(this, info, null, pFence).check()

    val fence = VkFence(this, pFence.get(0))

    info.free()
    memFree(pFence)

    return fence
}

/**
 * @return true  - all fences are signalled (or 1 was signalled if waitForAll is false)
 *         false - timeout occurred
 */
fun VkDevice.waitFor(fences:Array<VkFence>, waitForAll:Boolean, timeoutNanos:Long):Boolean {
    MemoryStack.stackPush().use { stack ->
        val pFences = stack.mallocLong(fences.size).put(fences).flip()

        return VK_SUCCESS == vkWaitForFences(this, pFences, waitForAll, timeoutNanos)
    }
}
