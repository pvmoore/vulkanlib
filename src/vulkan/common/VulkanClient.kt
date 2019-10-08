package vulkan.common

import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkQueueFamilyProperties
import vulkan.api.VkRenderPass
import vulkan.api.createRenderPass
import vulkan.app.VulkanApplication
import vulkan.misc.QueueFamily
import vulkan.misc.VkFormat

abstract class VulkanClient(val params: Parameters) {

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
     * Select a default graphics and transfer queue. Allow client to override selections
     * and/or select extra queues.
     */
    open fun selectQueues(props: VkQueueFamilyProperties.Buffer, queues:Queues) {

        props.forEachIndexed { i, family->
            if(family.queueCount()>0) {

                if(!params.headless && queues.isGraphics(family.queueFlags()) and queues.canPresent(QueueFamily(i))) {
                    if(!queues.hasQueue(Queues.GRAPHICS) || !queues.isCompute(family.queueFlags())) {
                        queues.select(Queues.GRAPHICS, i, 1)
                    }
                }
                if(queues.isTransfer(family.queueFlags())) {

                    if(!queues.hasQueue(Queues.TRANSFER) ||
                      (!queues.isGraphics(family.queueFlags()) && !queues.isCompute(family.queueFlags())))
                    {
                        queues.select(Queues.TRANSFER, i, 1)
                    }
                }
            }
        }
    }

    /**
     * Render a new frame using the provided resource.
     */
    open fun render(frame: FrameInfo, res: PerFrameResource) {

    }

    class Parameters(
        var headless : Boolean = false,
        var windowed : Boolean = true,
        var width : Int = 600,
        var height : Int = 600,
        var windowTitle : String = "Vulkan Application",
        var enableVsync : Boolean = false,
        var swapChainUsage : Int = 0,
        var prefNumSwapChainBuffers : Int = 2,
        var depthStencilFormat : VkFormat = 0  // 0 = no depth/stencil will be created
    )
    {
        fun headless(flag:Boolean)    = apply { this.headless = flag }
        fun windowed(flag:Boolean)    = apply { this.windowed = flag }
        fun width(width:Int)          = apply { this.width = width }
        fun height(height:Int)        = apply { this.height = height }
        fun windowTitle(title:String) = apply { this.windowTitle = title }
        fun enableVsync(flag:Boolean) = apply { this.enableVsync = flag }
        fun swapChainUsage(usage:Int) = apply { this.swapChainUsage = usage }
        fun prefNumSwapChainBuffers(num:Int) = apply { this.prefNumSwapChainBuffers = num }
        fun depthStencilFormat(format:VkFormat) = apply { this.depthStencilFormat = format }
    }
}
