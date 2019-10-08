package vulkan.common

import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.*
import vulkan.api.buffer.*
import vulkan.api.descriptor.VkDescriptorSet
import vulkan.api.descriptor.bindDescriptorSet
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.image.VkImage
import vulkan.api.image.blitImage
import vulkan.api.image.copyImage
import vulkan.api.image.setImageLayout
import vulkan.api.pipeline.*
import vulkan.misc.*
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 *
 */
class CommandBufferBuilder(val cmd:VkCommandBuffer) {
    /**
     * @param flags VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT      - Each recording of the command buffer will only be submitted once
     *              VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT - A secondary command buffer is considered to be entirely inside a render pass
     *              VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT     - Allows the command buffer to be resubmitted to a queue while it is in the pending state
     */
    fun beginOneTimeSubmit() = apply {
        cmd.begin(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
    }
    fun beginRenderPassContinue() = apply {
        cmd.begin(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
    }
    fun VkCommandBuffer.beginSimultaneousUse() = apply {
        cmd.begin(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
    }
    fun begin(flags:Int = 0) = apply {
        cmd.begin(flags)
    }
    fun end() = apply {
        cmd.end()
    }

    /**
     *  Only call reset if VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT was set on the pool.
     */
    fun reset(releaseResources:Boolean) = apply {
        cmd.reset(releaseResources)
    }

    fun setViewport(width:Int, height:Int, minDepth:Float = 0.0f, maxDepth:Float=1.0f) = apply {
        cmd.setViewport(width, height, minDepth, maxDepth)
    }
    fun setScissor(offset: Vector2i, extent: Vector2i) = apply {
        cmd.setScissor(offset, extent)
    }

    fun setLineWidth(width:Float) = apply {
        cmd.setLineWidth(width)
    }
    fun setDepthBias(fconstantFctor:Float, clamp:Float, slopeFactor:Float) = apply {
        cmd.setDepthBias(fconstantFctor, clamp, slopeFactor)
    }
    fun setBlendConstants(constants:FloatArray) = apply {
        cmd.setBlendConstants(constants)
    }
    fun setDepthBounds(min:Float, max:Float) = apply {
        cmd.setDepthBounds(min, max)
    }
    fun setStencilCompareMask(faceMask:Int, compareMask:Int) = apply {
        cmd.setStencilCompareMask(faceMask, compareMask)
    }
    fun setStencilWriteMask(faceMask:Int, writeMask:Int) = apply {
        cmd.setStencilWriteMask(faceMask, writeMask)
    }
    fun setStencilReference(faceMask:Int, reference:Int) = apply {
        cmd.setStencilReference(faceMask, reference)
    }

    fun draw(vertexCount:Int, instanceCount:Int, firstVertex:Int, firstInstance:Int) = apply {
        cmd.draw(vertexCount, instanceCount, firstVertex, firstInstance)
    }
    fun drawIndexed(indexCount:Int, instanceCount:Int, firstIndex:Int, vertexOffset:Int, firstInstance:Int) = apply {
        cmd.drawIndexed(indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)
    }
    fun drawIndirect(buffer: VkBuffer, offset:Long, drawCount:Int, stride:Int) = apply {
        cmd.drawIndirect(buffer, offset, drawCount, stride)
    }
    fun drawIndexedIndirect(buffer: VkBuffer, offset:Long, drawCount:Int, stride:Int) = apply {
        cmd.drawIndexedIndirect(buffer, offset, drawCount, stride)
    }

    fun dispatch(x:Int, y:Int, z:Int) = apply {
        cmd.dispatch(x, y, z)
    }
    fun dispatchIndirect(buffer: VkBuffer, offset:Long) = apply {
        cmd.dispatchIndirect(buffer, offset)
    }

    fun copyBufferToImage(src: VkBuffer, dest: VkImage, destLayout:VkImageLayout, regions: VkBufferImageCopy.Buffer) = apply {
        cmd.copyBufferToImage(src, dest, destLayout, regions)
    }
    fun copyImageToBuffer(src: VkImage, srcLayout:VkImageLayout, dest: VkBuffer, regions: VkBufferImageCopy.Buffer) = apply {
        cmd.copyImageToBuffer(src, srcLayout, dest, regions)
    }

    fun clearColorImage(img: VkImage, layout:VkImageLayout, colour:VkClearColorValue, ranges:VkImageSubresourceRange.Buffer) = apply {
        cmd.clearColorImage(img, layout, colour, ranges)
    }
    fun clearDepthStencilImage(img: VkImage, layout:VkImageLayout, stencil:VkClearDepthStencilValue, range:VkImageSubresourceRange) = apply {
        cmd.clearDepthStencilImage(img, layout, stencil, range)
    }
    fun clearAttachments(attachments:VkClearAttachment.Buffer, rects:VkClearRect.Buffer) = apply {
        cmd.clearAttachments(attachments, rects)
    }
    fun resolveImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions:VkImageResolve.Buffer) = apply {
        cmd.resolveImage(src, srcLayout, dest, destLayout, regions)
    }

    fun setEvent(event: VkEvent, stageMask:VkPipelineStageFlags) = apply {
        cmd.setEvent(event, stageMask)
    }
    fun resetEvent(event: VkEvent, stageMask:VkPipelineStageFlags) = apply {
        cmd.resetEvent(event, stageMask)
    }
    fun waitEvents(events:Array<VkEvent>,
                   srcStageMask:VkPipelineStageFlags,
                   destStageMask:VkPipelineStageFlags,
                   memoryBarriers:VkMemoryBarrier.Buffer,
                   bufferMemoryBarriers:VkBufferMemoryBarrier.Buffer,
                   imageMemoryBarriers:VkImageMemoryBarrier.Buffer
    ) = apply {
        cmd.waitEvents(events, srcStageMask, destStageMask, memoryBarriers, bufferMemoryBarriers, imageMemoryBarriers)
    }

    fun pipelineBarrier(srcStageMask:VkPipelineStageFlags,
                        destStageMask:VkPipelineStageFlags,
                        dependencyFlags:VkDependencyFlags,
                        memoryBarriers:VkMemoryBarrier.Buffer?,
                        bufferMemoryBarriers:VkBufferMemoryBarrier.Buffer?,
                        imageMemoryBarriers:VkImageMemoryBarrier.Buffer?
    ) = apply {
        cmd.pipelineBarrier(srcStageMask, destStageMask, dependencyFlags, memoryBarriers, bufferMemoryBarriers, imageMemoryBarriers)
    }

    fun beginQuery(queryPool: VkQueryPool, query:Int, flags:VkQueryControlFlags) = apply {
        cmd.beginQuery(queryPool, query, flags)
    }
    fun endQuery(queryPool: VkQueryPool, query:Int) = apply {
        cmd.endQuery(queryPool, query)
    }
    fun resetQueryPool(queryPool: VkQueryPool, firstQuery:Int, queryCount:Int) = apply {
        cmd.resetQueryPool(queryPool, firstQuery, queryCount)
    }
    fun writeTimestamp(pipelineStage:VkPipelineStageFlags, queryPool: VkQueryPool, query:Int) = apply {
        cmd.writeTimestamp(pipelineStage, queryPool, query)
    }
    fun copyQueryPoolResults(queryPool: VkQueryPool, firstQuery:Int, queryCount:Int, dest: VkBuffer, destOffset:Long, stride:Long, flags:VkQueryResultFlags) = apply {
        cmd.copyQueryPoolResults(queryPool, firstQuery, queryCount, dest, destOffset, stride, flags)
    }

    fun pushConstants(pipelineLayout: VkPipelineLayout, stageFlags:VkShaderStageFlags, offset:Int, values: FloatBuffer) = apply {
        cmd.pushConstants(pipelineLayout, stageFlags, offset, values)
    }

    fun beginRenderPass(renderPass: VkRenderPass,
                        frameBuffer: VkFrameBuffer,
                        clearValues:VkClearValue.Buffer,
                        renderArea: Vector4i,
                        inline:Boolean=true
    ) = apply {
        cmd.beginRenderPass(renderPass, frameBuffer, clearValues, renderArea, inline)
    }
    fun nextSubpass(subpassContents:Int) = apply {
        cmd.nextSubpass(subpassContents)
    }
    fun endRenderPass() = apply {
        cmd.endRenderPass()
    }
    fun executeCommands(buffers:List<VkCommandBuffer>) = apply {
        cmd.executeCommands(buffers)
    }
    fun executeCommands(buffer:VkCommandBuffer) = apply {
        cmd.executeCommands(buffer)
    }

    fun copyBuffer(src: BufferAlloc, dest: BufferAlloc) = apply {
        cmd.copyBuffer(src, dest)
    }
    fun copyBuffer(src: VkBuffer, dest: VkBuffer) = apply {
        cmd.copyBuffer(src, dest)
    }
    fun copyBuffer(src: VkBuffer, srcOffset:Int, dest: VkBuffer, destOffset:Int, size:Int) = apply {
        cmd.copyBuffer(src, srcOffset, dest, destOffset, size)
    }
    fun copyBuffer(src: VkBuffer, dest: VkBuffer, regions: VkBufferCopy.Buffer) = apply {
        cmd.copyBuffer(src, dest, regions)
    }
    fun updateBuffer(dest: VkBuffer, offset:Int, data: IntBuffer) = apply {
        cmd.updateBuffer(dest, offset, data)
    }
    fun fillBuffer(dest: VkBuffer, offset:Int, size:Int, data:Int) = apply {
        cmd.fillBuffer(dest, offset, size, data)
    }

    fun bindIndexBuffer(buffer: BufferAlloc, useShorts:Boolean=true) = apply {
        cmd.bindIndexBuffer(buffer.buffer, buffer.bufferOffset, useShorts)
    }
    fun bindIndexBuffer(buffer: VkBuffer, offset:Int, useShorts:Boolean = true) = apply {
        cmd.bindIndexBuffer(buffer, offset, useShorts)
    }
    fun bindVertexBuffer(binding:Int, buffer: BufferAlloc) = apply {
        cmd.bindVertexBuffers(binding, arrayOf(buffer.buffer), longArrayOf(buffer.bufferOffset.toLong()))
    }
    fun bindVertexBuffers(firstBinding:Int, buffers:Array<VkBuffer>, offsets:LongArray) = apply {
        cmd.bindVertexBuffers(firstBinding, buffers, offsets)
    }

    fun bindDescriptorSet(pipelineBindPoint:VkPipelineBindPoint,
                          pipelineLayout: VkPipelineLayout,
                          descriptorSet : VkDescriptorSet,
                          firstSet:Int = 0
    ) = apply {
        cmd.bindDescriptorSet(pipelineBindPoint, pipelineLayout, descriptorSet, firstSet)
    }
    fun bindDescriptorSets(pipelineBindPoint:VkPipelineBindPoint,
                           pipelineLayout: VkPipelineLayout,
                           firstSet:Int,
                           descriptorSets:Array<VkDescriptorSet>,
                           dynamicOffsets:IntArray = intArrayOf()
    ) = apply {
        cmd.bindDescriptorSets(pipelineBindPoint, pipelineLayout, firstSet, descriptorSets, dynamicOffsets)
    }

    fun copyImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions: VkImageCopy.Buffer) = apply {
        cmd.copyImage(src, srcLayout, dest, destLayout, regions)
    }
    fun blitImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions:VkImageBlit.Buffer, filter:VkFilter) = apply {
        cmd.blitImage(src, srcLayout, dest, destLayout, regions, filter)
    }

    fun setImageLayout(image: VkImage,
                       aspectMask:VkImageAspectFlags,
                       oldLayout:VkImageLayout,
                       newLayout:VkImageLayout,
                       srcQueue:Int  = VK_QUEUE_FAMILY_IGNORED,
                       destQueue:Int = VK_QUEUE_FAMILY_IGNORED)
    {
        cmd.setImageLayout(image, aspectMask, oldLayout, newLayout, srcQueue, destQueue)
    }

    fun bindPipeline(p: GraphicsPipeline) = apply {
        cmd.bindPipeline(p)
    }
    fun bindPipeline(p: ComputePipeline) = apply {
        cmd.bindPipeline(p)
    }
    fun bindGraphicsPipeline(pipeline: VkPipeline) = apply {
        cmd.bindGraphicsPipeline(pipeline)
    }
    fun bindComputePipeline(pipeline: VkPipeline) = apply {
        cmd.bindComputePipeline(pipeline)
    }
}
