package vulkan.api.buffer

import org.lwjgl.vulkan.VK10.VK_NULL_HANDLE
import org.lwjgl.vulkan.VK10.vkCmdCopyBuffer
import org.lwjgl.vulkan.VkBufferCopy
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDescriptorBufferInfo
import java.nio.ByteBuffer

class BufferAlloc(val buffer: VkBuffer,
                  val bufferOffset:Int,
                  val size:Int,
                  val isRange:Boolean = false)
{
    val memoryOffset get() = bufferOffset + buffer.memoryOffset
    val memory get()       = buffer.memory
    val type get()         = buffer.memory.type
    val usage get()        = buffer.usage

    fun free() {
        assert(!isRange)
        buffer.deallocate(this)
    }

    fun mapForWriting(functor:(ByteBuffer)->Unit) {
        memory.mapForWriting(buffer, bufferOffset, size, functor)
    }
    fun mapForReading(functor:(ByteBuffer)->Unit) {
        memory.mapForReading(buffer, bufferOffset, size, functor)
    }

    fun write(info:VkDescriptorBufferInfo) {
        info.buffer(buffer.handle)
            .offset(bufferOffset.toLong())
            .range(size.toLong())
    }

    fun rangeOf(offset:Int, size:Int):BufferAlloc {
        assert(offset>=0 && offset+size <= this.size)

        return BufferAlloc(buffer, bufferOffset+offset, size, true)
    }

    override fun toString() = "BufferAlloc(memoryOffset=$bufferOffset, size=$size)"
}

//==========================================================================================

fun VkCommandBuffer.copyBuffer(src: BufferAlloc, dest: BufferAlloc)
    : VkCommandBuffer
{
    assert(src.size==dest.size)
    assert(src.buffer.handle!=VK_NULL_HANDLE)
    assert(dest.buffer.handle!=VK_NULL_HANDLE)

    val regions = VkBufferCopy.calloc(1)
        .srcOffset(src.bufferOffset.toLong())
        .dstOffset(dest.bufferOffset.toLong())
        .size(src.size.toLong())

    vkCmdCopyBuffer(this, src.buffer.handle, dest.buffer.handle, regions)

    regions.free()
    return this
}
fun VkCommandBuffer.bindIndexBuffer(buffer:BufferAlloc, useShorts:Boolean=true):VkCommandBuffer {
    return this.bindIndexBuffer(buffer.buffer, buffer.bufferOffset, useShorts)
}
fun VkCommandBuffer.bindVertexBuffer(binding:Int, buffer: BufferAlloc):VkCommandBuffer {
    return this.bindVertexBuffers(binding, arrayOf(buffer.buffer), longArrayOf(buffer.bufferOffset.toLong()))
}