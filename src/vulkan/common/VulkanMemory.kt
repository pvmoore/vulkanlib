package vulkan.common

import vulkan.api.memory.*
import vulkan.app.VulkanApplication
import vulkan.misc.orThrow

class VulkanMemory {

    private lateinit var vk:VulkanApplication
    private data class Mem(val name:String, val size:Int, val mem:VkDeviceMemory)

    private val map = HashMap<String,Mem>()

    companion object {
        /** Some common IDs */
        const val DEVICE            = "_device"
        const val SHARED            = "_shared"
        const val STAGING_UPLOAD    = "_staging_upload"
        const val STAGING_DOWNLOAD  = "_staging_download"
    }

    fun init(vk:VulkanApplication) : VulkanMemory {
        this.vk = vk
        return this
    }
    fun destroy() {
        map.forEach { k, v -> v.mem.free() }
        map.clear()
    }

    fun get(name:String) : VkDeviceMemory = map[name]?.mem ?: throw Error("Memory '$name' not found")

    fun create(name:String, size:Int, type:MemoryType) : VulkanMemory {
        map[name] = Mem(name, size, vk.allocateMemory(size, type))
        return this
    }
    fun createOnDevice(name:String, size:Int) : VulkanMemory {
        return create(name, size, vk.selectDeviceMemoryType(size).orThrow())
    }
    /** This could return device memory if shared is not available */
    fun createShared(name:String, size:Int) : VulkanMemory {
        return create(name, size, vk.selectSharedMemoryType(size).orThrow())
    }
    fun createStagingUpload(name:String, size:Int) : VulkanMemory {
        return create(name, size, vk.selectStagingUploadMemoryType(size).orThrow())
    }
    fun createStagingDownload(name:String, size:Int) : VulkanMemory {
        return create(name, size, vk.selectStagingDownloadMemoryType(size).orThrow())
    }
    override fun toString(): String {
        val buf = StringBuilder("VulkanMemory {\n")
        map.forEach { k, v ->
            buf.append("\t$k : ").append(v.mem).append("\n")
        }
        return buf.append("}").toString()
    }
}