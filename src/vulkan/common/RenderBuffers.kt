package vulkan.common

import vulkan.api.buffer.VkBuffer
import vulkan.api.memory.*
import vulkan.misc.orThrow

class RenderBuffers(
    val staging: VkBuffer,
    val readback: VkBuffer,
    val vertex: VkBuffer,
    val index: VkBuffer,
    val uniform: VkBuffer
)
{
    companion object {

        fun make(deviceMem: VkDeviceMemory, hostMem: VkDeviceMemory,
                 staging:Int, readback:Int, vertex:Int, index:Int, uniform:Int)
             : RenderBuffers
        {
            return RenderBuffers(
                staging = hostMem.allocStagingSrcBuffer(staging).orThrow(),
                readback = hostMem.allocStagingSrcBuffer(readback).orThrow(),
                vertex = deviceMem.allocVertexBuffer(vertex).orThrow(),
                index = deviceMem.allocIndexBuffer(index).orThrow(),
                uniform = deviceMem.allocUniformBuffer(uniform).orThrow()
            )
        }
    }
}