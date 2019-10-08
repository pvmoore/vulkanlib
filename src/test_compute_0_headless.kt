
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkQueueFamilyProperties
import vulkan.api.waitForIdle
import vulkan.app.VulkanApplication
import vulkan.common.MEGABYTE
import vulkan.common.Queues
import vulkan.common.VulkanClient
import vulkan.common.log

/**
 * Vulkan headless compute example
 */
fun main(args:Array<String>) {
    println("Testing Vulkan Compute...")

    val client = HeadlessCompute()
    var app: VulkanApplication? = null

    fun initialise() {
        app = VulkanApplication(client)
        app?.initialise()

        val totalMem = Runtime.getRuntime().totalMemory()
        val freeMem  = Runtime.getRuntime().freeMemory()
        println("total: ${totalMem/ MEGABYTE} MB")
        println("free : ${freeMem/ MEGABYTE} MB")
        println("used : ${(totalMem-freeMem)/ MEGABYTE} MB")
    }
    fun destroy() {
        client.destroy()
        app?.destroy()
    }

    try{
        initialise()

    }catch(e:Throwable) {
        e.printStackTrace()
    }finally {
        destroy()
    }

    println("Finished")
}
//=========================================================================================
private class HeadlessCompute : VulkanClient(Parameters(headless = true)) {

    private lateinit var vk: VulkanApplication
    private lateinit var device: VkDevice

    override fun destroy() {
        device?.let {
            device.waitForIdle()

        }
    }
    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {

    }
    override fun selectQueues(props : VkQueueFamilyProperties.Buffer, queues : Queues) {
        super.selectQueues(props, queues)

        /** Select the first compute queue that we see */
        props.forEachIndexed { i, family ->
            if(family.queueCount() > 0) {
                if(queues.isCompute(family.queueFlags())) {
                    queues.select(Queues.COMPUTE, i, 1)
                }
            }
        }
    }
    override fun vulkanReady(vk : VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        doCompute()
    }
    fun doCompute() {
        log.info("Computing ...")

        log.info("Done")
    }
}