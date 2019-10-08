package vulkan.api

import org.joml.Vector2i
import org.joml.Vector4i
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.buffer.VkBuffer
import vulkan.api.image.VkImage
import vulkan.api.pipeline.VkPipelineLayout
import vulkan.misc.*
import java.nio.FloatBuffer


fun PointerBuffer.put(a:Array<VkCommandBuffer>): PointerBuffer {
    a.forEach { put(it.address()) }
    return this
}

/**
 * @param flags VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT      - Each recording of the command buffer will only be submitted once
 *              VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT - A secondary command buffer is considered to be entirely inside a render pass
 *              VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT     - Allows the command buffer to be resubmitted to a queue while it is in the pending state
 */
fun VkCommandBuffer.beginOneTimeSubmit():VkCommandBuffer {
    return this.begin(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
}
fun VkCommandBuffer.beginRenderPassContinue():VkCommandBuffer {
    return this.begin(VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT)
}
fun VkCommandBuffer.beginSimultaneousUse():VkCommandBuffer {
    return this.begin(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
}
fun VkCommandBuffer.begin(flags:Int = 0):VkCommandBuffer {
    MemoryStack.stackPush().use { stack ->
        val info = VkCommandBufferBeginInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(flags)
            .pInheritanceInfo(null)

        vkBeginCommandBuffer(this, info).check()

        return this
    }
}
fun VkCommandBuffer.end():VkCommandBuffer {
    vkEndCommandBuffer(this).check()
    return this
}

/**
 *  Only call reset if VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT was set on the pool.
 */
fun VkCommandBuffer.reset(releaseResources:Boolean):VkCommandBuffer {
    val flags = if(releaseResources) VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT else 0
    vkResetCommandBuffer(this, flags).check()
    return this
}

fun VkCommandBuffer.setViewport(width:Int, height:Int, minDepth:Float = 0.0f, maxDepth:Float=1.0f):VkCommandBuffer {
    MemoryStack.stackPush().use { stack ->

        val viewport = VkViewport.calloc(1)
            .height(height.toFloat())
            .width(width.toFloat())
            .minDepth(minDepth)
            .maxDepth(maxDepth)

        vkCmdSetViewport(this, 0, viewport)

        return this
    }
}
fun VkCommandBuffer.setScissor(offset:Vector2i, extent:Vector2i):VkCommandBuffer {
    MemoryStack.stackPush().use { stack ->

        val scissors = VkRect2D.calloc(1)
        scissors.offset().set(offset.x, offset.y)
        scissors.extent().set(extent.x, extent.y)

        vkCmdSetScissor(this, 0, scissors)

        return this
    }
}
fun VkCommandBuffer.setLineWidth(width:Float):VkCommandBuffer {
    vkCmdSetLineWidth(this, width)
    return this
}
fun VkCommandBuffer.setDepthBias(fconstantFctor:Float, clamp:Float, slopeFactor:Float):VkCommandBuffer {
    vkCmdSetDepthBias(this, fconstantFctor, clamp, slopeFactor)
    return this
}
fun VkCommandBuffer.setBlendConstants(constants:FloatArray):VkCommandBuffer {
    vkCmdSetBlendConstants(this, constants)
    return this
}
fun VkCommandBuffer.setDepthBounds(min:Float, max:Float):VkCommandBuffer {
    vkCmdSetDepthBounds(this, min, max)
    return this
}
fun VkCommandBuffer.setStencilCompareMask(faceMask:Int, compareMask:Int):VkCommandBuffer {
    vkCmdSetStencilCompareMask(this, faceMask, compareMask)
    return this
}
fun VkCommandBuffer.setStencilWriteMask(faceMask:Int, writeMask:Int):VkCommandBuffer {
    vkCmdSetStencilWriteMask(this, faceMask, writeMask)
    return this
}
fun VkCommandBuffer.setStencilReference(faceMask:Int, reference:Int):VkCommandBuffer {
    vkCmdSetStencilReference(this, faceMask, reference)
    return this
}


fun VkCommandBuffer.draw(vertexCount:Int, instanceCount:Int, firstVertex:Int, firstInstance:Int):VkCommandBuffer {
    vkCmdDraw(this, vertexCount, instanceCount, firstVertex, firstInstance)
    return this
}
fun VkCommandBuffer.drawIndexed(indexCount:Int, instanceCount:Int, firstIndex:Int, vertexOffset:Int, firstInstance:Int):VkCommandBuffer {
    vkCmdDrawIndexed(this, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance)
    return this
}
fun VkCommandBuffer.drawIndirect(buffer: VkBuffer, offset:Long, drawCount:Int, stride:Int):VkCommandBuffer {
    vkCmdDrawIndirect(this, buffer.handle, offset, drawCount, stride)
    return this
}
fun VkCommandBuffer.drawIndexedIndirect(buffer: VkBuffer, offset:Long, drawCount:Int, stride:Int):VkCommandBuffer {
    vkCmdDrawIndexedIndirect(this, buffer.handle, offset, drawCount, stride)
    return this
}

fun VkCommandBuffer.dispatch(x:Int, y:Int, z:Int):VkCommandBuffer {
    vkCmdDispatch(this, x, y, z)
    return this
}
fun VkCommandBuffer.dispatchIndirect(buffer: VkBuffer, offset:Long):VkCommandBuffer {
    vkCmdDispatchIndirect(this, buffer.handle, offset)
    return this
}


fun VkCommandBuffer.copyBufferToImage(src: VkBuffer, dest: VkImage, destLayout:VkImageLayout, regions:VkBufferImageCopy.Buffer):VkCommandBuffer {
    vkCmdCopyBufferToImage(this, src.handle, dest.handle, destLayout, regions)
    return this
}
fun VkCommandBuffer.copyImageToBuffer(src: VkImage, srcLayout:VkImageLayout, dest: VkBuffer, regions:VkBufferImageCopy.Buffer):VkCommandBuffer {
    vkCmdCopyImageToBuffer(this, src.handle, srcLayout, dest.handle, regions)
    return this
}


fun VkCommandBuffer.clearColorImage(img: VkImage, layout:VkImageLayout, colour:VkClearColorValue, ranges:VkImageSubresourceRange.Buffer):VkCommandBuffer {
    vkCmdClearColorImage(this, img.handle, layout, colour, ranges)
    return this
}
fun VkCommandBuffer.clearDepthStencilImage(img: VkImage, layout:VkImageLayout, stencil:VkClearDepthStencilValue, range:VkImageSubresourceRange):VkCommandBuffer {
    vkCmdClearDepthStencilImage(this, img.handle, layout, stencil, range)
    return this
}
fun VkCommandBuffer.clearAttachments(attachments:VkClearAttachment.Buffer, rects:VkClearRect.Buffer):VkCommandBuffer {
    vkCmdClearAttachments(this, attachments, rects)
    return this
}
fun VkCommandBuffer.resolveImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions:VkImageResolve.Buffer):VkCommandBuffer {
    vkCmdResolveImage(this, src.handle, srcLayout, dest.handle, destLayout, regions)
    return this
}

fun VkCommandBuffer.setEvent(event:VkEvent, stageMask:VkPipelineStageFlags):VkCommandBuffer {
    vkCmdSetEvent(this, event.handle, stageMask)
    return this
}
fun VkCommandBuffer.resetEvent(event:VkEvent, stageMask:VkPipelineStageFlags):VkCommandBuffer {
    vkCmdResetEvent(this, event.handle, stageMask)
    return this
}
fun VkCommandBuffer.waitEvents(events:Array<VkEvent>,
                               srcStageMask:VkPipelineStageFlags,
                               destStageMask:VkPipelineStageFlags,
                               memoryBarriers:VkMemoryBarrier.Buffer,
                               bufferMemoryBarriers:VkBufferMemoryBarrier.Buffer,
                               imageMemoryBarriers:VkImageMemoryBarrier.Buffer)
    :VkCommandBuffer
{
    MemoryStack.stackPush().use { stack ->

        val pEvents = stack.mallocLong(events.size)
        events.forEach { pEvents.put(it.handle) }
        pEvents.flip()

        vkCmdWaitEvents(this, pEvents, srcStageMask, destStageMask, memoryBarriers, bufferMemoryBarriers, imageMemoryBarriers)
        return this
    }
}

fun VkCommandBuffer.pipelineBarrier(srcStageMask:VkPipelineStageFlags,
                                    destStageMask:VkPipelineStageFlags,
                                    dependencyFlags:VkDependencyFlags,
                                    memoryBarriers:VkMemoryBarrier.Buffer?,
                                    bufferMemoryBarriers:VkBufferMemoryBarrier.Buffer?,
                                    imageMemoryBarriers:VkImageMemoryBarrier.Buffer?)
    :VkCommandBuffer
{
    vkCmdPipelineBarrier(this,
        srcStageMask, destStageMask,
        dependencyFlags,
        memoryBarriers, bufferMemoryBarriers, imageMemoryBarriers)
    return this
}

fun VkCommandBuffer.beginQuery(queryPool:VkQueryPool, query:Int, flags:VkQueryControlFlags):VkCommandBuffer {
    vkCmdBeginQuery(this, queryPool.handle, query, flags)
    return this
}
fun VkCommandBuffer.endQuery(queryPool:VkQueryPool, query:Int):VkCommandBuffer {
    vkCmdEndQuery(this, queryPool.handle, query)
    return this
}
fun VkCommandBuffer.resetQueryPool(queryPool:VkQueryPool, firstQuery:Int, queryCount:Int):VkCommandBuffer {
    vkCmdResetQueryPool(this, queryPool.handle, firstQuery, queryCount)
    return this
}
fun VkCommandBuffer.writeTimestamp(pipelineStage:VkPipelineStageFlags, queryPool:VkQueryPool, query:Int):VkCommandBuffer {
    vkCmdWriteTimestamp(this, pipelineStage, queryPool.handle, query)
    return this
}
fun VkCommandBuffer.copyQueryPoolResults(queryPool:VkQueryPool, firstQuery:Int, queryCount:Int, dest: VkBuffer, destOffset:Long, stride:Long, flags:VkQueryResultFlags):VkCommandBuffer {
    vkCmdCopyQueryPoolResults(this, queryPool.handle, firstQuery, queryCount, dest.handle, destOffset, stride, flags)
    return this
}

fun VkCommandBuffer.pushConstants(pipelineLayout: VkPipelineLayout, stageFlags:VkShaderStageFlags, offset:Int, values: FloatBuffer):VkCommandBuffer {
    vkCmdPushConstants(this, pipelineLayout.handle, stageFlags, offset, values)
    return this
}
fun VkCommandBuffer.beginRenderPass(renderPass:VkRenderPass,
                                    frameBuffer:VkFrameBuffer,
                                    clearValues:VkClearValue.Buffer,
                                    renderArea: Vector4i,
                                    inline:Boolean=true)
    :VkCommandBuffer
{
    MemoryStack.stackPush().use { stack ->

        val info = VkRenderPassBeginInfo.callocStack()
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
            .renderPass(renderPass.handle)
            .framebuffer(frameBuffer.handle)
            .pClearValues(clearValues)

        info.renderArea().offset().set(renderArea.x, renderArea.y)
        info.renderArea().extent().set(renderArea.z, renderArea.w)

        val subpassContents = if(inline) VK_SUBPASS_CONTENTS_INLINE else
                                         VK_SUBPASS_CONTENTS_SECONDARY_COMMAND_BUFFERS

        vkCmdBeginRenderPass(this, info, subpassContents)
        return this
    }
}
fun VkCommandBuffer.nextSubpass(subpassContents:Int):VkCommandBuffer {
    vkCmdNextSubpass(this, subpassContents)
    return this
}
fun VkCommandBuffer.endRenderPass():VkCommandBuffer {
    vkCmdEndRenderPass(this)
    return this
}
fun VkCommandBuffer.executeCommands(buffers:List<VkCommandBuffer>):VkCommandBuffer {
    MemoryStack.stackPush().use { stack ->

        val pBuffers = stack.mallocPointer(buffers.size)
        buffers.forEach { pBuffers.put(it.address()) }
        pBuffers.flip()

        vkCmdExecuteCommands(this, pBuffers)
        return this
    }
}
fun VkCommandBuffer.executeCommands(buffer:VkCommandBuffer):VkCommandBuffer {
    vkCmdExecuteCommands(this, buffer)
    return this
}
