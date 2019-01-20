package vulkan.api.image

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.pipelineBarrier
import vulkan.common.MEGABYTE
import vulkan.misc.*
import kotlin.test.assertTrue

class VkImage(
    private val device: VkDevice,
    val handle: Long,
    val memoryOffset: Int,
    val format: VkFormat,
    val dimensions: IntArray,   // size==3
    val size: Int)
{
    private var views = ArrayList<VkImageView>()

    init{
        assert(dimensions.size==3)
        assertTrue { dimensions.all { it>0 } }
    }
    fun destroy() {
        views.forEach { if(it.handle != VK_NULL_HANDLE) it.destroy() }
        vkDestroyImage(device, handle, null)
    }

    /**
     * @return The first view created from this image
     */
    fun getView():VkImageView {
        return when { views.isNotEmpty() -> views.first() else -> createView() }
    }
    /**
     *  Note that you can only create a view with the same format as
     *  the original image unless you specify VK_IMAGE_CREATE_MUTABLE_FORMAT_BIT flag
     *  when creating the image
     */
    fun createView(overrides:((VkImageViewCreateInfo)->Unit)? = null): VkImageView {

        // VkImageViewCreateFlags
        // VK_IMAGE_VIEW_CREATE_FRAGMENT_DENSITY_MAP_DYNAMIC_BIT_EXT

        /** Set default values */
        val info = VkImageViewCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
            .viewType(VK_IMAGE_VIEW_TYPE_2D)
            .format(format)
            .image(handle)
            .flags(0)

        info.components().set(
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY,
            VK_COMPONENT_SWIZZLE_IDENTITY)

        info.subresourceRange()
            .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
            .baseMipLevel(0)
            .levelCount(1)
            .baseArrayLayer(0)
            .layerCount(1)

        /** Allow the user to change some values */
        overrides?.invoke(info)

        val pView = memAllocLong(1)
        vkCreateImageView(device, info, null, pView)

        val view = VkImageView(device, pView.get(0))
        views.add(view)

        info.free()
        memFree(pView)

        return view
    }
    override fun toString() =
        "VkImage(${dimensions.contentToString()}, " +
        "$size (${size/ MEGABYTE.toFloat()} MB), " +
        "${format.translateVkFormat()}" +
        ")"
}

fun VkCommandBuffer.copyImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions: VkImageCopy.Buffer):VkCommandBuffer {
    vkCmdCopyImage(this, src.handle, srcLayout, dest.handle, destLayout, regions)
    return this
}
fun VkCommandBuffer.blitImage(src: VkImage, srcLayout:VkImageLayout, dest: VkImage, destLayout:VkImageLayout, regions:VkImageBlit.Buffer, filter:VkFilter):VkCommandBuffer {
    vkCmdBlitImage(this, src.handle, srcLayout, dest.handle, destLayout, regions, filter)
    return this
}

fun VkCommandBuffer.setImageLayout(image: VkImage,
                                   aspectMask:VkImageAspectFlags,
                                   oldLayout:VkImageLayout,
                                   newLayout:VkImageLayout,
                                   srcQueue:Int  = VK_QUEUE_FAMILY_IGNORED,
                                   destQueue:Int = VK_QUEUE_FAMILY_IGNORED)
{
    val barrier = VkImageMemoryBarrier.calloc(1)
        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
        .oldLayout(oldLayout)
        .newLayout(newLayout)
        .image(image.handle)
        .srcQueueFamilyIndex(srcQueue)
        .dstQueueFamilyIndex(destQueue)

    barrier.subresourceRange()
        .aspectMask(aspectMask)
        .baseMipLevel(0)
        .levelCount(VK_REMAINING_MIP_LEVELS)
        .baseArrayLayer(0)
        .layerCount(VK_REMAINING_ARRAY_LAYERS)

    var srcStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
    var dstStageMask = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT

    when {
        oldLayout==VK_IMAGE_LAYOUT_PREINITIALIZED &&
        newLayout==VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL ->
        {
            barrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT)
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
            srcStageMask = VK_PIPELINE_STAGE_HOST_BIT
            dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT
        }
        oldLayout==VK_IMAGE_LAYOUT_PREINITIALIZED &&
        newLayout==VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL ->
        {
            barrier.srcAccessMask(VK_ACCESS_HOST_WRITE_BIT)
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            srcStageMask = VK_PIPELINE_STAGE_HOST_BIT
            dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT
        }
        oldLayout==VK_IMAGE_LAYOUT_UNDEFINED &&
        newLayout==VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL ->
        {
            barrier.srcAccessMask(0)
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            srcStageMask = VK_PIPELINE_STAGE_HOST_BIT
            dstStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT
        }
        oldLayout==VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL &&
        newLayout==VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL ->
        {
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            srcStageMask = VK_PIPELINE_STAGE_TRANSFER_BIT
            dstStageMask = VK_PIPELINE_STAGE_ALL_COMMANDS_BIT
        }
    }

    this.pipelineBarrier(
        srcStageMask,
        dstStageMask,
        0,               // dependencyFlags
        null,
        null,
        barrier
    )

    barrier.free()
}
