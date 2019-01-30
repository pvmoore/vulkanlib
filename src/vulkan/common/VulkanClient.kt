package vulkan.common

import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.VulkanApplication
import vulkan.api.VkRenderPass
import vulkan.api.createRenderPass
import vulkan.misc.VkFormat

abstract class VulkanClient(
    val windowed:Boolean             = true,
    val width:Int                    = 600,
    val height:Int                   = 600,
    val windowTitle:String           = "Vulkan CLient",
    val enableVsync:Boolean          = false,
    val swapChainUsage:Int           = 0,
    val targetFPS:Int                = 60,
    val prefNumGraphicsQueues:Int    = 1,
    val prefNumComputeQueues:Int     = 1,
    val prefNumTransferQueues:Int    = 1,
    val prefNumSwapChainBuffers:Int  = 2)
{
    val headless = prefNumGraphicsQueues==0

    abstract fun destroy()
    /**
     * Set features required by your application.
     */
    abstract fun enableFeatures(f:VkPhysicalDeviceFeatures)
    /**
     * Called by VulkanApplication to get the client VkRenderPass.
     * Override if you don't want the standard one.
     */
    open fun createRenderPass(device: VkDevice, surfaceFormat: VkFormat): VkRenderPass {
        return device.createRenderPass(colorFormat = surfaceFormat) { info -> }
    }
    /**
     *  Called once VulkanApplication is completely initialised.
     *  This is the opportunity for the client application to create it's own Vulkan objects.
     */
    abstract fun vulkanReady(vk: VulkanApplication)

    /**
     * Render a new frame using the provided resource.
     */
    open fun render(frame: FrameInfo, res: PerFrameResource) {

    }
}
