
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkCommandBuffer
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures
import org.lwjgl.vulkan.VkQueueFamilyProperties
import vulkan.api.*
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.memory.createStagingTransfer
import vulkan.api.pipeline.ComputePipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.app.VulkanApplication
import vulkan.common.*
import vulkan.misc.megabytes
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vulkan headless compute example
 */
fun main(args:Array<String>) {
    println("Testing Vulkan Compute...")

    val client = ComputeBenchmark()
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
private class ComputeBenchmark : VulkanClient(headless = true) {

    private lateinit var vk: VulkanApplication
    private lateinit var device: VkDevice

    private val memory      = VulkanMemory()
    private val buffers     = VulkanBuffers()
    private val descriptors = Descriptors()
    private val pipeline    = ComputePipeline()
    private var computeCP   = null as VkCommandPool?
    private var computeCB   = null as VkCommandBuffer?
    private var queryPool   = null as VkQueryPool?

    override fun destroy() {
        device?.let {
            device.waitForIdle()

            computeCP?.destroy()
            queryPool?.destroy()

            descriptors.destroy()
            pipeline.destroy()
            buffers.destroy()
            memory.destroy()
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

        vk.shaders.setCompilerPath("/pvmoore/_tools/glslangValidator.exe")

        memory.init(vk)
            .createOnDevice(VulkanMemory.DEVICE, 32.megabytes())
            .createStagingUpload(VulkanMemory.STAGING_UPLOAD, 32.megabytes())
            .createStagingDownload(VulkanMemory.STAGING_DOWNLOAD, 32.megabytes())
        log.info("$memory")

        buffers.init(vk)
            .createStagingUploadBuffer(VulkanBuffers.STAGING_UPLOAD, memory.get(VulkanMemory.STAGING_UPLOAD), 4.megabytes())
            .createStagingUploadBuffer(VulkanBuffers.STAGING_DOWNLOAD, memory.get(VulkanMemory.STAGING_DOWNLOAD), 4.megabytes())
            .createUniformBuffer(VulkanBuffers.UNIFORM, memory.get(VulkanMemory.DEVICE), 1.megabytes())
            .createStorageBuffer("input", memory.get(VulkanMemory.DEVICE), 4.megabytes())
            .createStorageBuffer("output", memory.get(VulkanMemory.DEVICE), 4.megabytes())
        log.info("$buffers")

        val context = RenderContext(vk, device, null, buffers)

        /**
         * Bindings:
         *    0     storage buffer (in)
         *    1     storage buffer (out)
         */
        this.descriptors
            .init(context)
            .createLayout()
            .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
            .storageBuffer(VK_SHADER_STAGE_COMPUTE_BIT)
            .numSets(1)
            .build()

        descriptors.layout(0).createSet()
            .add(buffers.get("input"), 0, 4.megabytes())
            .add(buffers.get("output"), 0, 4.megabytes())
            .write()

        pipeline
            .init(context)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader("Benchmark/bench.comp")
            .build()
        println("pipeline = $pipeline")

        this.queryPool = device.createQueryPool(VK_QUERY_TYPE_TIMESTAMP, 2)

        computeCP = device.createCommandPool(vk.queues.getFamily(Queues.COMPUTE)).apply {
            computeCB = this.alloc()

            computeCB!!.run {
                begin()

                bindPipeline(pipeline)
                bindDescriptorSets(
                    VK_PIPELINE_BIND_POINT_COMPUTE,
                    pipeline.layout,
                    0,
                    arrayOf(descriptors.layout(0).set(0)),
                    intArrayOf()
                )

                resetQueryPool(queryPool!!, 0, 2)
                writeTimestamp(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, queryPool!!, 0)

                dispatch(1.megabytes() / 64, 1, 1)

                writeTimestamp(VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT, queryPool!!, 1)

                end()
            }
        }

        uploadData()

        val start = System.nanoTime()
        for(i in 0..99) {
            doCompute()
        }
        val end = System.nanoTime()
        println("Elapsed time: ${((end-start) * 1e-06)/100} ms")

        downloadData()
    }
    private fun uploadData() {
        val uploadByteBuffer = ByteBuffer.allocateDirect(4.megabytes()).order(ByteOrder.nativeOrder())

        val data = IntArray(size = 1.megabytes(), init = { it * 1 } )
        uploadByteBuffer.asIntBuffer().put(data).flip()

        println("inputs = ${uploadByteBuffer.asIntBuffer()[2]}")

        val uploader = vk.createStagingTransfer(buffers.get(VulkanBuffers.STAGING_UPLOAD))

        uploader.upload(buffers.get("input"), 0, uploadByteBuffer)

        uploader.destroy()
        println("Uploaded ${data.size*4} bytes")
    }
    private fun downloadData() {
        val downloadByteBuffer = ByteBuffer.allocateDirect(4.megabytes()).order(ByteOrder.nativeOrder())

        val downloader = vk.createStagingTransfer(buffers.get(VulkanBuffers.STAGING_DOWNLOAD))

        downloader.download(buffers.get("output"), 0, downloadByteBuffer)

        val ints  = downloadByteBuffer.asIntBuffer()
        val array = IntArray(size=1.megabytes())
        ints.get(array)

        println("results = ${array.copyOfRange(0,32).joinToString()}")

        downloader.destroy()
    }
    private fun doCompute() {
        vk.queues.get(Queues.COMPUTE).submitAndWait(
            arrayOf(computeCB!!),
            arrayOf(),      // wait semaphores
            intArrayOf(),   // wait stages
            arrayOf()       // signal semaphores
        )

        queryPool!!.getResults(0, 2, waitFor = false, includeAvailability = true).run {
            if(this.isNotEmpty()) {
                println("Query timestamp results = ${this[1]-this[0]} available=${this[2]}")
            }
        }
    }
}