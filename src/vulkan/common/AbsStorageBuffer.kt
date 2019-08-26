package vulkan.common

import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer

abstract class AbsStorageBuffer<T : Transferable>(val numElements:Int) : AbsTransferable() {
    private var stagingBuf = null as BufferAlloc?
    private var deviceBuf  = null as BufferAlloc?
    private var isStale    = true

    val stagingBuffer get() = stagingBuf!!
    val deviceBuffer get()  = deviceBuf!!

    fun init(context:RenderContext) {
        stagingBuf = context.buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(size())
        deviceBuf  = context.buffers.get(VulkanBuffers.STORAGE).allocate(size())
    }
    fun destroy() {
        stagingBuf?.free()
        deviceBuf?.free()
    }
    fun setStale() {
        isStale = true
    }
    fun transfer(cmd: VkCommandBuffer) {
        if(isStale) {
            assert(deviceBuf != null)

            cmd.run {
                copyBuffer(stagingBuffer, deviceBuffer)
            }
            isStale = false
        }
    }
    /**
     *  Assumes the client handles the isStale state externally.
     */
    fun transferRange(cmd: VkCommandBuffer, offset:Int, size:Int) {
        assert(deviceBuf!=null)

        cmd.run {
            copyBuffer(stagingBuffer.rangeOf(offset, size), deviceBuffer.rangeOf(offset, size))
        }
    }

    abstract fun elementInstance():Transferable

    override fun size() : Int {
        return numElements * elementInstance().size()
    }
}