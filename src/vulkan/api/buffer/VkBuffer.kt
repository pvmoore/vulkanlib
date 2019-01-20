package vulkan.api.buffer

import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.memory.VkDeviceMemory
import vulkan.common.MEGABYTE
import vulkan.misc.*
import java.nio.ByteBuffer
import java.nio.IntBuffer

class VkBuffer(private val physicalDeviceProperties: VkPhysicalDeviceProperties,
               private val device:VkDevice,
               val memory: VkDeviceMemory,
               val handle:Long,
               val memoryOffset:Int,
               val size:Int,
               val usage:VkBufferUsageFlags)
{
    private val allocator = Allocator(size)

    val bytesAllocated get() = allocator.bytesUsed
    val bytesFree get()      = allocator.bytesFree

    fun destroy() {
        vkDestroyBuffer(device, handle, null)
    }
    /**
     * @param offset must be a multiple of VkPhysicalDeviceLimits::minTexelBufferOffsetAlignment
     * @param range  must be a multiple of the texel block size of format
     */
    fun createView(format:VkFormat, offset:Long, range:Long = VK_WHOLE_SIZE) {
        assert(offset>=0)

        val info = VkBufferViewCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_VIEW_CREATE_INFO)
            .buffer(handle)
            .format(format)
            .offset(offset)
            .range(range)

        val pBV = memAllocLong(1)
        vkCreateBufferView(device, info, null, pBV)

        info.free()
        memFree(pBV)
    }

    fun allocate(size:Int): BufferAlloc? {

        val offset = allocateRange(size)
        if(offset==-1) return null

        return BufferAlloc(this, offset, size)
    }
    fun deallocate(alloc: BufferAlloc) {
        assert(!alloc.isRange)
        allocator.free(alloc.bufferOffset)
    }

    fun mapForWriting(offset:Int, size:Int, functor:(ByteBuffer)->Unit) {
        assert(offset>=0 && size>0)
        assert(this.size-offset >= size)
        memory.mapForWriting(this, offset, size, functor)
    }
    fun mapForReading(offset:Int, size:Int, functor:(ByteBuffer)->Unit) {
        assert(offset>=0 && size>0)
        assert(this.size-offset >= size)
        memory.mapForReading(this, offset, size, functor)
    }

    fun write(info:VkDescriptorBufferInfo) {
        info.buffer(handle)
            .offset(0)
            .range(size.toLong())
    }

    override fun toString() = "VkBuffer(size=$size (${size/ MEGABYTE} MBs), usage=${usage.translateVkBufferUsageFlags()}, allocated=$bytesAllocated bytes, memoryOffset=$memoryOffset)"

    private fun allocateRange(size:Int):Int {
        val align = when {
            usage.isSet(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT) ->
                physicalDeviceProperties.limits().minUniformBufferOffsetAlignment().toInt()
            usage.isSet(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT) ->
                physicalDeviceProperties.limits().minStorageBufferOffsetAlignment().toInt()
            else -> 4
        }
        return allocator.alloc(size, align)
    }
}
class VkBufferView(private val device: VkDevice, val handle:Long) {
    fun destroy() {
        vkDestroyBufferView(device, handle, null)
    }
}

//==========================================================================================

fun VkCommandBuffer.copyBuffer(src: VkBuffer, dest: VkBuffer): VkCommandBuffer {
    assert(src.size==dest.size)
    return this.copyBuffer(src, 0, dest, 0, src.size)
}
fun VkCommandBuffer.copyBuffer(src: VkBuffer, srcOffset:Int, dest: VkBuffer, destOffset:Int, size:Int): VkCommandBuffer {

    val regions = VkBufferCopy.calloc(1)
        .srcOffset(srcOffset.toLong())
        .dstOffset(destOffset.toLong())
        .size(size.toLong())

    vkCmdCopyBuffer(this, src.handle, dest.handle, regions)
    regions.free()
    return this
}
fun VkCommandBuffer.copyBuffer(src: VkBuffer, dest: VkBuffer, regions: VkBufferCopy.Buffer): VkCommandBuffer {
    vkCmdCopyBuffer(this, src.handle, dest.handle, regions)
    return this
}
fun VkCommandBuffer.updateBuffer(dest: VkBuffer, offset:Int, data: IntBuffer):VkCommandBuffer {
    assert((data.limit() and 3) == 0)
    assert(data.limit() <= 65536)
    vkCmdUpdateBuffer(this, dest.handle, offset.toLong(), data)
    return this
}
fun VkCommandBuffer.fillBuffer(dest: VkBuffer, offset:Int, size:Int, data:Int):VkCommandBuffer {
    vkCmdFillBuffer(this, dest.handle, offset.toLong(), size.toLong(), data)
    return this
}
fun VkCommandBuffer.bindIndexBuffer(buffer: VkBuffer, offset:Int, useShorts:Boolean = true):VkCommandBuffer {
    val indexType = if(useShorts) VK_INDEX_TYPE_UINT16 else VK_INDEX_TYPE_UINT32
    vkCmdBindIndexBuffer(this, buffer.handle, offset.toLong(), indexType)
    return this
}
fun VkCommandBuffer.bindVertexBuffers(firstBinding:Int, buffers:Array<VkBuffer>, offsets:LongArray):VkCommandBuffer {
    MemoryStack.stackPush().use { stack ->
        assert(buffers.size==offsets.size)

        val pBuffers = stack.mallocLong(buffers.size)
        buffers.forEach { pBuffers.put(it.handle) }
        pBuffers.flip()

        val pOffsets = stack.mallocLong(offsets.size)
        pOffsets.put(offsets).flip()
        vkCmdBindVertexBuffers(this, firstBinding, pBuffers, pOffsets)
        return this
    }
}
