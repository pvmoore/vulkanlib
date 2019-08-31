
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkQueueFamilyProperties
import vulkan.api.*
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.misc.megabytes
import vulkan.misc.orThrow

/**
 * Vulkan headless compute example
 */
fun main(args:Array<String>) {
    println("Testing Vulkan Compute...")

    val client = ShaderPrintfComputeExample()
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

private class ShaderPrintfComputeExample : VulkanClient(
    headless = true)
{
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
        val computeQIndex = queues.findFirstFamilyWith(VK_QUEUE_COMPUTE_BIT)
        if(computeQIndex==-1) throw Error("Can't find a compute queue")
        queues.select(Queues.COMPUTE, computeQIndex, 1)
    }
    override fun vulkanReady(vk : VulkanApplication) {
        this.vk     = vk
        this.device = vk.device

        doCompute()
    }
    fun doCompute() {
        log.info("Ready ...................................................................")

        val memory = VulkanMemory().init(vk)
            .createOnDevice(VulkanMemory.DEVICE, 32.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 32.megabytes())
            .createStagingDownload(VulkanMemory.STAGING_DOWNLOAD, 32.megabytes())

        val buffers = VulkanBuffers().init(vk)
            .createStorageBuffer(VulkanBuffers.STORAGE, memory.get(VulkanMemory.DEVICE), 16.megabytes())
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 16.megabytes())
            .createStagingDownloadBuffer(VulkanBuffers.STAGING_DOWNLOAD, memory.get(VulkanMemory.STAGING_DOWNLOAD), 16.megabytes())

        val context = RenderContext(vk, vk.device, null, buffers)

        val storage = buffers.get(VulkanBuffers.STORAGE).allocate(1.megabytes()).orThrow()

        val printf = ShaderPrintf().init(context)

        val commandPool = device.createCommandPool(vk.queues.getFamily(Queues.COMPUTE))
        val descriptors = Descriptors().init(context)

        /** Create layout 0 */
        descriptors.createLayout()
                .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
            .numSets(1)

        /** Create printf layout 1 */
        printf.createLayout(descriptors)

        /** Build the layouts */
        descriptors.build()

        /** Create 1 set for layout 0 */
        descriptors.layout(0)
            .createSet()
                .add(storage)
            .write()

        /** Create 1 set for layout 1 */
        printf.createDescriptorSet(descriptors, 1)

        val pipeline = ComputePipeline().init(context)
            .withDSLayouts(descriptors.allDSLayouts())
            .withShader("TestPrintf.comp")
            .build()

        val buf = commandPool.alloc().apply {

            beginOneTimeSubmit()

            bindPipeline(pipeline)

            bindDescriptorSets(
                VK_PIPELINE_BIND_POINT_COMPUTE,
                pipeline.layout,
                0,
                arrayOf(descriptors.layout(0).set(0),
                        descriptors.layout(1).set(0)),
                intArrayOf()
            )

            clear(printf)

            dispatch(1.megabytes(), 1, 1)

            fetchResults(printf)

            end()
        }

        log.info("Submitting queue ........................................................")
        vk.queues.get(Queues.COMPUTE).submitAndWait(buf)

        log.info("Results .................................................................")

        log.info(printf.getOutput())

        log.info("Done ....................................................................")

        device.let {
            device.waitForIdle()

            commandPool.destroy()
            descriptors.destroy()
            pipeline.destroy()
            printf.destroy()
            buffers.destroy()
            memory.destroy()
        }
    }
}
