package vulkan.api

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo
import org.lwjgl.vulkan.VkCommandPoolCreateInfo
import org.lwjgl.vulkan.VkDevice
import vulkan.misc.check
import vulkan.misc.toList

class VkCommandPool(private val device:VkDevice, val handle:Long, val flags:Int) {

    fun destroy() {
        vkDestroyCommandPool(device, handle, null)
    }
    fun alloc(): VkCommandBuffer {
        return this.alloc(1, VK_COMMAND_BUFFER_LEVEL_PRIMARY).first()
    }
    fun allocSecondary(): VkCommandBuffer {
        return this.alloc(1, VK_COMMAND_BUFFER_LEVEL_SECONDARY).first()
    }

    fun free(buffer:VkCommandBuffer) {
        MemoryStack.stackPush().use { stack ->
            val p = stack.mallocPointer(1).put(buffer.address()).flip()
            vkFreeCommandBuffers(device, handle, p)
        }
    }
    fun free(buffers:List<VkCommandBuffer>) {
        assert(buffers.isNotEmpty())

        MemoryStack.stackPush().use { stack ->
            val p = stack.mallocPointer(buffers.size)

            buffers.forEach { p.put(it) }
            p.flip()

            vkFreeCommandBuffers(device, handle, p)
        }
    }
    //=======================================================================================
    private fun alloc(num:Int, level:Int): List<VkCommandBuffer> {
        MemoryStack.stackPush().use { stack ->
            val info = VkCommandBufferAllocateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(handle)
                .level(level)
                .commandBufferCount(num)

            val pBuffer = stack.mallocPointer(num)
            vkAllocateCommandBuffers(device, info, pBuffer).check()

            return pBuffer.toList { VkCommandBuffer(it, device) }
        }
    }
}

/**
 * @param flags VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT
 *              VK_COMMAND_POOL_CREATE_TRANSIENT_BIT
 */
fun VkDevice.createCommandPool(family:Int, flags:Int = 0):VkCommandPool {

    val info = VkCommandPoolCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
        .queueFamilyIndex(family)
        .flags(flags)

    val pPool = memAllocLong(1)
    vkCreateCommandPool(this, info, null, pPool).check()

    val pool = VkCommandPool(this, pPool.get(0), flags)

    info.free()
    memFree(pPool)

    return pool
}
/**
 *  Allocate a buffer and allow client to use and submit it
 *  before automatically freeing after use.
 *  This is expected to be used for throw-away buffers only. ie. transient but not reset-able
 *
 *  eg.
 *
 *  pool.beginOneTimeSubmit() { b->
 *      // do something with buffer
 *      b.end();
 *      submit(b)
 *  }
 */
fun VkCommandPool.beginOneTimeSubmit(f:(VkCommandBuffer)->Unit) {
    assert(this.flags == VK_COMMAND_POOL_CREATE_TRANSIENT_BIT) {
        "Expecting to be used on a command pool with TRANSIENT bit set"
    }

    this.alloc().let { b->
        b.beginOneTimeSubmit()
        f(b)
        this.free(b)
    }
}
