package vulkan.common

import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_COMPUTE
import org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS
import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.api.buffer.updateBuffer
import vulkan.api.descriptor.VkDescriptorSet
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.GraphicsPipeline
import vulkan.misc.VkShaderStageFlags
import vulkan.misc.orThrow
import vulkan.misc.sizeof
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ShaderPrintf {
    companion object {
        const val BUFFER_SIZE = 4 * 1024 * 1024
    }
    private lateinit var context:RenderContext
    private lateinit var stagingBuffer:BufferAlloc
    private lateinit var stagingStatsBuffer:BufferAlloc
    private lateinit var deviceBuffer:BufferAlloc
    private lateinit var deviceStatsBuffer:BufferAlloc
    private var descriptorSet = null as VkDescriptorSet?

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
    fun createLayout(d : Descriptors, stage:VkShaderStageFlags) {
        d.createLayout()
            .storageBuffer(stage)
            .storageBuffer(stage)
            .numSets(1)
    }
    fun clearBuffers(cmd:VkCommandBuffer) {
        cmd.updateBuffer(deviceStatsBuffer.buffer, deviceStatsBuffer.bufferOffset, zeroBuffer)
    }
    fun fetchBuffers(cmd:VkCommandBuffer) {
        cmd.copyBuffer(deviceStatsBuffer, stagingStatsBuffer)
        cmd.copyBuffer(deviceBuffer, stagingBuffer)
    }
    fun createDescriptorSet(d:Descriptors, layoutNumber:Int = 1) {
        val s = d.layout(layoutNumber)
            .createSet()
                .add(deviceBuffer)
                .add(deviceStatsBuffer)
            .write()

        this.descriptorSet = s.set
    }
    fun bindDescriptorSet(cmd:VkCommandBuffer, pipeline:GraphicsPipeline, setIndex:Int) {
        cmd.bindDescriptorSets(
            VK_PIPELINE_BIND_POINT_GRAPHICS,
            pipeline.layout,
            setIndex,
            arrayOf(descriptorSet!!),
            intArrayOf()
        )
    }
    fun bindDescriptorSet(cmd:VkCommandBuffer, pipeline:ComputePipeline, setIndex:Int) {
        cmd.bindDescriptorSets(
            VK_PIPELINE_BIND_POINT_COMPUTE,
            pipeline.layout,
            setIndex,
            arrayOf(descriptorSet!!),
            intArrayOf()
        )
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
                        if(components > 1) {
                            result.append("[")
                            result.append(String.format("%08x", ptr.get(i++).toInt()))
                            while(--components != 0) {
                                result.append(", ").append(String.format("%08x", ptr.get(i++).toInt()))
                            }
                            result.append("]")
                        } else {
                            result.append(String.format("%08x", ptr.get(i++).toInt()))
                        }
                    }
                    2 -> { // int
                        if(components > 1) {
                            result.append("[")
                            result.append(ptr.get(i++).toInt())
                            while(--components != 0) {
                                result.append(", ").append(ptr.get(i++).toInt())
                            }
                            result.append("]")
                        } else {
                            result.append(ptr.get(i++).toInt())
                        }
                    }
                    3 -> { // float
                        if(components > 1) {
                            result.append("[")
                            result.append(ptr.get(i++))
                            while(--components != 0) {
                                result.append(", ").append(ptr.get(i++))
                            }
                            result.append("]")
                        } else {
                            result.append(ptr.get(i++))
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
