package vulkan.common

import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc

abstract class AbsUBO : AbsTransferable() {
    private var stagingBuf: BufferAlloc? = null
    private var deviceBuf:BufferAlloc? = null

    val deviceBuffer get() = deviceBuf!!

    fun init(context:RenderContext) {
        stagingBuf = context.buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(size())
        deviceBuf  = context.buffers.get(VulkanBuffers.UNIFORM).allocate(size())
    }
    fun destroy() {
        stagingBuf?.free()
        deviceBuf?.free()
    }
    fun transfer(cmd: VkCommandBuffer) {
        assert(deviceBuf!=null)
        super.transfer(cmd, stagingBuf, deviceBuf!!)
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