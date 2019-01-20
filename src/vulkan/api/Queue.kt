package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.*
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.misc.check

fun VkDevice.getQueue(familyIndex:Int, queueIndex:Int): VkQueue {
    assert(familyIndex!=-1)
    assert(queueIndex >= 0)

    MemoryStack.stackPush().use { stack ->
        val handle = stack.mallocPointer(1)
        vkGetDeviceQueue(this, familyIndex, queueIndex, handle)

        return VkQueue(handle.get(0), this)
    }
}
fun VkDevice.getQueues(familyIndex:Int, numQueues:Int):Array<VkQueue> {
    if(familyIndex==-1 || numQueues==0) return arrayOf()

    val queues = ArrayList<VkQueue>()
    val handle = memAllocPointer(1)

    (0 until numQueues).forEach { i->

        vkGetDeviceQueue(this, familyIndex, i, handle)

        queues.add(VkQueue(handle.get(0), this))
    }
    memFree(handle)
    return queues.toTypedArray()
}

fun VkQueue.submit(cmdBuffers: Array<VkCommandBuffer>, fence: VkFence? = null) {
    this.submit(cmdBuffers, arrayOf(), intArrayOf(), arrayOf(), fence)
}

fun VkQueue.submit(cmdBuffers:Array<VkCommandBuffer>,
                   waitSemaphores:Array<VkSemaphore>,
                   waitStages:IntArray,
                   signalSemaphores:Array<VkSemaphore>,
                   fence:VkFence? = null)
{
    assert(waitStages.size==waitSemaphores.size)
    assert(cmdBuffers.isNotEmpty())

    MemoryStack.stackPush().use { stack ->

        val pCmdBuffers = stack.mallocPointer(cmdBuffers.size).put(cmdBuffers).flip()

        val info = VkSubmitInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            .pCommandBuffers(pCmdBuffers)

        if(waitSemaphores.isNotEmpty()) {
            val pWaitSemaphores = stack.mallocLong(waitSemaphores.size).put(waitSemaphores).flip()
            val pWaitStages = memAllocInt(waitStages.size).put(waitStages).flip()

            info.waitSemaphoreCount(waitSemaphores.size)
                .pWaitSemaphores(pWaitSemaphores)
                .pWaitDstStageMask(pWaitStages)
        }
        if(signalSemaphores.isNotEmpty()) {
            val pSignalSemaphores = stack.mallocLong(signalSemaphores.size).put(signalSemaphores).flip()

            info.pSignalSemaphores(pSignalSemaphores)
        }

        vkQueueSubmit(this, info, fence?.handle ?: VK_NULL_HANDLE).check()
    }
}

fun VkQueue.submitAndWait(cmdBuffers:Array<VkCommandBuffer>,
                          waitSemaphores:Array<VkSemaphore>,
                          waitStages:IntArray,
                          signalSemaphores:Array<VkSemaphore>)
{
    val fence = this.device.createFence()
    this.submit(cmdBuffers, waitSemaphores, waitStages, signalSemaphores, fence)
    fence.waitFor()
    fence.destroy()
}
fun VkQueue.submitAndWait(cmdBuffer:VkCommandBuffer) {
    this.submitAndWait(arrayOf(cmdBuffer), arrayOf(), intArrayOf(), arrayOf())
}
fun VkQueue.bindSparse(fence:VkFence, sparseInfos: VkBindSparseInfo.Buffer) {
    vkQueueBindSparse(this, sparseInfos, fence.handle)
}
