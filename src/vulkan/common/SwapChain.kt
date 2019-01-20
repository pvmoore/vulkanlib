package vulkan.common

import org.joml.Vector2i
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackGet
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.KHRSurface.*
import org.lwjgl.vulkan.KHRSwapchain.*
import org.lwjgl.vulkan.VK10.*
import vulkan.VulkanApplication
import vulkan.api.*
import vulkan.api.image.VkImage
import vulkan.api.image.VkImageView
import vulkan.misc.*
import java.nio.IntBuffer

class SwapChain(private val app: VulkanApplication, surfaceFormat: SurfaceFormat) {
    private val surface:Long                    = app.surface
    private val physicalDevice:VkPhysicalDevice = app.physicalDevice
    private val device: VkDevice                = app.device
    private val colorFormat                     = surfaceFormat.colorFormat
    private val colorSpace                      = surfaceFormat.colorSpace
    private val nextImage:IntBuffer             = memAllocInt(1)
    private val handle:VkSwapchainKHR

    val extent       = Vector2i()
    val images       = ArrayList<VkImage>()
    val views        = ArrayList<VkImageView>()
    val frameBuffers = ArrayList<VkFrameBuffer>()

    var numImages:Int = 0

    init{
        log.info("Initialising SwapChain ...")

        handle = createSwapChain()
        getSwapChainImages()
        createImageViews()
        createFrameBuffers(app.renderPass)
    }
    fun destroy() {
        log.info("Destroying SwapChain")
        memFree(nextImage)
        if(handle!=VK_NULL_HANDLE) {
            views.forEach { it.destroy() }
            frameBuffers.forEach { it.destroy() }
            vkDestroySwapchainKHR(device, handle, null)
        }
    }
    fun acquireNext(imageAvailableSemaphore: VkSemaphore, fence: VkFence? = null):Int {

        vkAcquireNextImageKHR(device, handle, -1, imageAvailableSemaphore.handle, fence?.handle ?: 0, nextImage).let {
            when(it) {
                VK_SUCCESS               -> {}
                VK_ERROR_OUT_OF_DATE_KHR -> log.warn("acquire VK_ERROR_OUT_OF_DATE_KHR")
                VK_SUBOPTIMAL_KHR        -> log.warn("acquire VK_SUBOPTIMAL_KHR")
                VK_NOT_READY             -> log.warn("acquire VK_NOT_READY")
                else                     -> it.check()
            }
        }
        return nextImage.get(0)
    }
    fun queuePresent(queue: VkQueue, imageIndex:Int, waitSemaphores:Array<VkSemaphore>) {
        MemoryStack.stackPush().use { stack ->

            val pWaitSemaphores = stack.mallocLong(waitSemaphores.size).put(waitSemaphores).flip()
            val pSwapChains     = stack.mallocLong(1).put(handle).flip()
            val pImageIndex     = stack.mallocInt(1).put(imageIndex).flip()

            val info = VkPresentInfoKHR.callocStack()
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(pWaitSemaphores)
                .swapchainCount(1)
                .pSwapchains(pSwapChains)
                .pImageIndices(pImageIndex)

            vkQueuePresentKHR(queue, info).let {
                when(it) {
                    VK_SUCCESS               -> {}
                    VK_ERROR_OUT_OF_DATE_KHR -> log.warn("present VK_ERROR_OUT_OF_DATE_KHR")
                    VK_SUBOPTIMAL_KHR        -> log.warn("present VK_SUBOPTIMAL_KHR")
                    VK_NOT_READY             -> log.warn("present VK_NOT_READY")
                    else                     -> it.check()
                }
            }
        }
    }
    //==================================================================================
    private fun selectPresentMode():Int {
        val stack = stackGet()

        val presentModes = getPresentModes(physicalDevice, surface)
        log.info("\tSupported present modes:${presentModes.map { it.translateVkPresentModeKHR() }}")

        // FIFO is always available
        val mode:Int

        mode = if(app.client.enableVsync) {
            when {
                presentModes.contains(VK_PRESENT_MODE_FIFO_RELAXED_KHR) -> VK_PRESENT_MODE_FIFO_RELAXED_KHR
                else -> VK_PRESENT_MODE_FIFO_KHR
            }
        } else {
            // prefer mailbox
            when {
                presentModes.contains(VK_PRESENT_MODE_MAILBOX_KHR) -> VK_PRESENT_MODE_MAILBOX_KHR
                presentModes.contains(VK_PRESENT_MODE_IMMEDIATE_KHR) -> VK_PRESENT_MODE_IMMEDIATE_KHR
                else -> VK_PRESENT_MODE_FIFO_KHR
            }
        }
        log.info("\tSetting present mode to ${mode.translateVkPresentModeKHR()}")
        return mode
    }
    private fun getSwapChainImages() {
        MemoryStack.stackPush().use { stack ->
            val pCount  = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, handle, pCount, null).check()

            val pImages = stack.mallocLong(pCount.get(0))
            vkGetSwapchainImagesKHR(device, handle, pCount, pImages).check()

            pImages.forEach { images.add(
                VkImage(
                    device,
                    it,
                    0,
                    colorFormat,
                    intArrayOf(extent.x, extent.y, 1),
                    0
                )
            ) }

            assert(images.size==pCount.get(0))

            log.info("\tGot ${pCount.get(0)} images")
        }
    }
    private fun createImageViews() {
        MemoryStack.stackPush().use { stack ->

            val createInfo = VkImageViewCreateInfo.callocStack()
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .format(colorFormat)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)

            createInfo.components().apply {
                r(VK_COMPONENT_SWIZZLE_R)
                g(VK_COMPONENT_SWIZZLE_G)
                b(VK_COMPONENT_SWIZZLE_B)
                a(VK_COMPONENT_SWIZZLE_A)
            }
            createInfo.subresourceRange().apply {
                aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                baseMipLevel(0)
                levelCount(1)
                baseArrayLayer(0)
                layerCount(1)
            }

            val pView = stack.mallocLong(1)

            images.forEachIndexed { i,img ->

                createInfo.image(img.handle)
                vkCreateImageView(device, createInfo, null, pView).check()

                views.add(VkImageView(device, pView.get(0)))
            }
            log.info("\tCreated ${views.size} image views")
        }
    }
    fun createFrameBuffers(renderPass:VkRenderPass) {
        views.forEach {
            frameBuffers.add(device.createFrameBuffer(it, extent, renderPass))
        }
        log.info("\tCreated ${frameBuffers.size} frame buffers")
    }
    private fun createSwapChain():VkSwapchainKHR {
        MemoryStack.stackPush().use { stack ->
            val caps = getCapabilities(stack, physicalDevice, surface)

            val extentValid = caps.currentExtent().width()!=-1 && caps.currentExtent().height()!=-1
            val width  = if(extentValid) caps.currentExtent().width() else app.requestedWindowSize.x
            val height = if(extentValid) caps.currentExtent().height() else app.requestedWindowSize.y
            log.info("\tSwapChain images size $width x $height")

            this.extent.apply {
                x = width
                y = height
            }

            numImages = app.client.prefNumSwapChainBuffers.clamp(caps.minImageCount(), caps.maxImageCount())
            log.info("\tCreating $numImages images")

            val preTransform = when {
                (caps.supportedTransforms() and VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)!=0 -> VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR
                else -> caps.currentTransform()
            }
            log.info("\tPre-transform is ${preTransform.translateVkSurfaceTransformFlags()}")

            val createInfo = VkSwapchainCreateInfoKHR.callocStack()
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface)
                .minImageCount(numImages)
                .imageFormat(colorFormat)
                .imageColorSpace(colorSpace)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT or app.client.swapChainUsage)
                .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null)
                .preTransform(preTransform)
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(selectPresentMode())
                .clipped(true)
                .oldSwapchain(0)

            createInfo.imageExtent().apply { width(width).height(height) }

            val pLong = stack.mallocLong(1)
            vkCreateSwapchainKHR(device, createInfo, null, pLong).check()

            return pLong.get(0)
        }
    }
}
