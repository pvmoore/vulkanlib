package vulkan.common

import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc

abstract class AbsUBO : AbsTransferable() {
    private var stagingBuf = null as BufferAlloc?
    private var deviceBuf  = null as BufferAlloc?
    private var isStale    = true

    val deviceBuffer get() = deviceBuf!!

    fun init(context:RenderContext) {
        stagingBuf = context.buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(size())
        deviceBuf  = context.buffers.get(VulkanBuffers.UNIFORM).allocate(size())
    }
    fun destroy() {
        stagingBuf?.free()
        deviceBuf?.free()
    }
    fun setStale() {
        isStale = true
    }
    fun transfer(cmd: VkCommandBuffer) {
        assert(deviceBuf!=null)
        if(isStale) {
            assert(deviceBuf != null)

            super.transfer(cmd, stagingBuf, deviceBuf!!)
            isStale = false
        }
    }
    /**
     * Uniform buffers must be a multiple of 16 bytes
     */
    override fun size() : Int {
        val s = super.size()
        assert(s%16==0)
        return s
    }
}

fun VkCommandBuffer.transfer(ubo:AbsUBO) {
    ubo.transfer(this)
}