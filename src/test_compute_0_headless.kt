
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import vulkan.VulkanApplication
import vulkan.api.waitForIdle
import vulkan.common.MEGABYTE
import vulkan.common.VulkanClient
import vulkan.common.log

/**
 * Vulkan headless compute example
 */
fun main() {
    println("Testing Vulkan Compute...")

    test()

    println("Finished")
}
private fun test() {
    val client = HeadlessCompute()
    var app: VulkanApplication? = null

    fun initialise() {
        app = VulkanApplication(client)
        app?.initialise()

        val totalMem = Runtime.getRuntime().totalMemory()
        val freeMem  = Runtime.getRuntime().freeMemory()
        println("total: ${totalMem/ MEGABYTE} MB");
        println("free : ${freeMem/ MEGABYTE} MB");
        println("used : ${(totalMem-freeMem)/ MEGABYTE} MB");
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
}
//=========================================================================================
private class HeadlessCompute : VulkanClient(
    prefNumGraphicsQueues = 0,
    prefNumComputeQueues  = 1)
{
    private lateinit var vk:VulkanApplication
    private lateinit var device: VkDevice

    override fun destroy() {
        device?.let {
            device.waitForIdle()

        }
    }
    override fun enableFeatures(f : VkPhysicalDeviceFeatures) {

    }
    override fun vulkanReady(vk : VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        doCompute()
    }
    fun doCompute() {
        log.info("Computing ...")


    }
}