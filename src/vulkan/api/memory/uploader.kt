package vulkan.api.memory

import org.lwjgl.vulkan.VkQueue
import vulkan.VulkanApplication
import vulkan.api.VkCommandPool
import vulkan.api.beginOneTimeSubmit
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.VkBuffer
import vulkan.api.buffer.copyBuffer
import vulkan.api.end
import vulkan.api.submitAndWait
import vulkan.common.Transferable
import vulkan.common.log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals

fun VulkanApplication.createStagingTransfer(stagingBuffer: VkBuffer):StagingTransfer {
    return StagingTransfer(this.transferCP, this.transferQueues[0], stagingBuffer)
}

class StagingTransfer(private val transferCP: VkCommandPool,
                      private val transferQueue: VkQueue,
                      private val stagingBuffer: VkBuffer)
{
    fun upload(dest: BufferAlloc, src: Transferable):StagingTransfer {
        val bytes = ByteBuffer.allocateDirect(src.size()).order(ByteOrder.nativeOrder())
        src.writeTo(bytes)
        bytes.flip()

        return upload(dest.buffer, dest.bufferOffset, bytes)
    }
    fun upload(dest: BufferAlloc, src:ByteBuffer):StagingTransfer {
        return upload(dest.buffer, dest.bufferOffset, src)
    }
    fun upload(dest: VkBuffer, destOffset:Int, src:ByteBuffer):StagingTransfer {

        val size = src.limit()

        /** Allocate space in staging buffer */
        stagingBuffer.allocate(size)?.let { staging ->

            /** Write the data to the staging buffer */
            staging.mapForWriting { bb->
                bb.put(src)
                src.flip()
            }

            /** Upload from the staging buffer */
            copy(staging.buffer, staging.bufferOffset, dest, destOffset, size)

            staging.free()
        }
        return this
    }

    fun download(src: BufferAlloc, dest: ByteBuffer):StagingTransfer {
        return download(src.buffer, src.bufferOffset, dest)
    }
    fun download(src: VkBuffer, srcOffset:Int, dest: ByteBuffer):StagingTransfer {

        val size = dest.limit()

        /** Allocate space in staging buffer */
        stagingBuffer.allocate(size)?.let { staging ->

            /** Download to the staging buffer */
            copy(src, srcOffset, staging.buffer, staging.bufferOffset, size)

            /** Copy the data */
            staging.mapForReading { bb->
                dest.put(bb)
                dest.flip()
            }

            staging.free()
        }

        return this
    }
    //=======================================================================================================
    //     _____       _____       _____     __      __                 _______     ______
    //    |  __ \     |  __ \     |_   _|    \ \    / /       /\       |__   __|   |  ____|
    //    | |__) |    | |__) |      | |       \ \  / /       /  \         | |      | |__
    //    |  ___/     |  _  /       | |        \ \/ /       / /\ \        | |      |  __|
    //    | |         | | \ \      _| |_        \  /       / ____ \       | |      | |____
    //    |_|         |_|  \_\    |_____|        \/       /_/    \_\      |_|      |______|
    //
    //=======================================================================================================
    private fun copy(src: BufferAlloc, dest: BufferAlloc) {
        assertEquals(src.size, dest.size)

        copy(src.buffer, src.bufferOffset, dest.buffer, dest.bufferOffset, src.size)
    }
    private fun copy(src: VkBuffer, srcOffset:Int, dest: VkBuffer, destOffset:Int, size:Int) {

        val b = transferCP.alloc()
            .beginOneTimeSubmit()
            .copyBuffer(src, srcOffset, dest, destOffset, size)
            /*  .pipelineBarrier(
                PIPELINE_STAGE_TRANSFER,
                PIPELINE_STAGE_TOP_OF_PIPE,
                0,      // dependencyFlags
                null,   // memory barriers
                [
                    bufferMemoryBarrier(
                        dest.handle, 0, dest.size,
                        ACCESS_MEMORY_WRITE,
                        ACCESS_MEMORY_READ,
                        vk.queueFamily.transfer,
                        vk.queueFamily.graphics
                    ),
                ],     // buffer memory barriers
                null    // image memory barriers
            );*/
            .end()


        val start = System.nanoTime()
        this.transferQueue.submitAndWait(b)
        this.transferCP.free(b)
        val end = System.nanoTime()

        log.debug("Blocking copy took ${(end-start)/1_000_000.0} ms")
    }
}