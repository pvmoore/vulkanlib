package vulkan.common

import org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_COMPUTE_BIT
import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.buffer.updateBuffer
import vulkan.misc.orThrow
import vulkan.misc.sizeof
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShaderPrintf {
    companion object {
        const val BUFFER_SIZE = 4 * 1024 * 1024
    }
    private lateinit var context:RenderContext
    internal lateinit var stagingBuffer:BufferAlloc
    internal lateinit var stagingStatsBuffer:BufferAlloc
    internal lateinit var deviceBuffer:BufferAlloc
    internal lateinit var deviceStatsBuffer:BufferAlloc

    internal val zeroBuffer = ByteBuffer.allocateDirect(4*Int.sizeof())
                                        .order(ByteOrder.nativeOrder())
                                        .asIntBuffer()
                                        .put(intArrayOf(0,0,0,0))
                                        .flip()

    fun init(context:RenderContext) : ShaderPrintf {
        this.context = context

        this.stagingBuffer      = context.buffers.get(VulkanBuffers.STAGING_DOWNLOAD).allocate(BUFFER_SIZE).orThrow()
        this.stagingStatsBuffer = context.buffers.get(VulkanBuffers.STAGING_DOWNLOAD).allocate(zeroBuffer.limit()*Int.sizeof()).orThrow()

        this.deviceBuffer       = context.buffers.get(VulkanBuffers.STORAGE).allocate(BUFFER_SIZE).orThrow()
        this.deviceStatsBuffer  = context.buffers.get(VulkanBuffers.STORAGE).allocate(zeroBuffer.limit()*Int.sizeof()).orThrow()

        return this
    }
    fun destroy() {
        stagingBuffer.free()
        stagingStatsBuffer.free()
        deviceBuffer.free()
        deviceStatsBuffer.free()
    }
    fun createLayout(d : Descriptors) {
        d.createLayout()
            .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
            .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
            .numSets(1)
    }
    fun createDescriptorSet(d:Descriptors, layoutNumber:Int = 1) {
        d.layout(layoutNumber)
            .createSet()
                .add(deviceBuffer)
                .add(deviceStatsBuffer)
            .write()
    }
    fun getOutput() : String {

        var suffix = '\n'
        var length = 0
        val result = StringBuilder()

        stagingStatsBuffer.mapForReading { b ->
            val ints = b.asIntBuffer()

            length = ints[1]
        }

        stagingBuffer.mapForReading { bytes->
            val ptr = bytes.asFloatBuffer()

            var i = 0
            while(i < length) {
                val type       = ptr.get(i++).toInt()
                var components = ptr.get(i++).toInt()

                when(type) {
                    0 -> { // char
                        result.append(ptr.get(i++).toChar())
                    }
                    1 -> { // uint
                        result.append(String.format("%08x", ptr.get(i++).toInt()))
                        while(--components != 0) {
                            result.append(", ").append(String.format("%08x", ptr.get(i++).toInt()))
                        }
                    }
                    2 -> { // int
                        result.append(ptr.get(i++).toInt())
                        while(--components != 0) {
                            result.append(", ").append(ptr.get(i++).toInt())
                        }
                    }
                    3 -> { // float
                        result.append(ptr.get(i++))
                        while(--components != 0) {
                            result.append(", ").append(ptr.get(i++))
                        }
                    }
                    6 -> { // matrix
                        val cols = ptr.get(i++).toInt()
                        val rows = ptr.get(i++).toInt()

                        // Read all the values
                        val values = Array(size = cols*rows, init = { 0f })
                        for(j in values.indices) values[j] = ptr.get(i++)

                        for(j in 0 until rows*cols) {
                            val c = j % cols
                            val r = j / cols

                            if(c>0) result.append(", ")

                            result.append(values[c*rows+r])

                            if(c == cols-1) result.append("\n")
                        }
                    }
                    7 -> { // set suffix
                        suffix = ptr.get(i++).toChar()
                    }
                    else -> {
                        log.info("Unhandled type $type")
                    }
                }
                if(suffix.toInt()!=0) {
                    result.append(suffix)
                }
            }
        }

        return result.toString()
    }
}

fun VkCommandBuffer.clear(printf : ShaderPrintf) {
    this.updateBuffer(printf.deviceStatsBuffer.buffer, 0, printf.zeroBuffer)
}
fun VkCommandBuffer.fetchResults(printf : ShaderPrintf) {
    this.copyBuffer(printf.deviceStatsBuffer, printf.stagingStatsBuffer)
    this.copyBuffer(printf.deviceBuffer, printf.stagingBuffer)
}
