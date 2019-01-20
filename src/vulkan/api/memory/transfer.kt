package vulkan.api.memory

import org.lwjgl.vulkan.VkBufferCopy
import vulkan.VulkanApplication
import vulkan.api.beginOneTimeSubmit
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.end
import vulkan.api.submitAndWait


fun VulkanApplication.blockingCopy(src: BufferAlloc, dest: BufferAlloc) {
    assert(src.size==dest.size)
    blockingCopy(src, 0, dest, 0, src.size)
}
fun VulkanApplication.blockingCopy(src: BufferAlloc, srcOffset:Int, dest: BufferAlloc, destOffset:Int, size:Int) {
    assert(src.size >= size && dest.size >= size)

    val start = System.nanoTime()

    val b = this.transferCP.alloc()
        .beginOneTimeSubmit()
        .copyBuffer(src.buffer, srcOffset, dest.buffer, destOffset, size)
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

    this.transferQueues[0].submitAndWait(b)
    val end = System.nanoTime()

    this.transferCP.free(b)

    println("\tblockingCopy took ${(end-start)/1_000_000.0} ms")

}
/**
 *  Blocking copy of data to a BufferAlloc on the GPU via a staging buffer on the host.
 */
//fun copyToDevice(dest:BufferAlloc, data: ByteBuffer) {
//    assert(dest.size == data.limit())
//}
//void copyToDevice(DeviceBuffer destDB, void* data) {
//    expect(destDB.memory.isLocal);
//
//    auto dest = new SubBuffer(destDB, 0, destDB.size, destDB.usage);
//    auto src  = createStagingBuffer(destDB.size);
//
//    // memcpy the data to staging subbuffer
//    void* ptr = staging.map(src);
//    memcpy(ptr, data, destDB.size);
//    staging.flush(src);
//
//    blockingCopy(src, dest);
//    src.free();
//}