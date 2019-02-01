package vulkan.common

import org.lwjgl.vulkan.VK10.*
import vulkan.api.buffer.VkBuffer
import vulkan.api.memory.VkDeviceMemory
import vulkan.app.VulkanApplication
import vulkan.misc.VkBufferUsageFlags

class VulkanBuffers {
    private data class Buffer(val name:String,
                              val size:Int,
                              val usage:VkBufferUsageFlags,
                              val buffer: VkBuffer)

    private lateinit var vk:VulkanApplication
    private val map = HashMap<String,Buffer>()

    companion object {
        /** Some common IDs */
        const val VERTEX           = "_vertex"
        const val INDEX            = "_index"
        const val UNIFORM          = "_uniform"
        const val STORAGE          = "_storage"
        const val STAGING_UPLOAD   = "_staging_upload"
        const val STAGING_DOWNLOAD = "_staging_download"
    }

    fun init(vk:VulkanApplication) : VulkanBuffers {
        this.vk = vk
        return this
    }
    fun destroy() {
        map.forEach { k, v -> v.buffer.destroy() }
        map.clear()
    }

    fun get(name:String) : VkBuffer = map[name]?.buffer ?: throw Error("Buffer '$name' not found")

    fun createBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags) : VulkanBuffers {
        memory.allocBuffer(
            size      = size,
            overrides = { info -> info.usage(usage) },
            onError   = { reason -> throw Error("Unable to allocate '$name' buffer: $reason") },
            onSuccess = { b -> map[name] = Buffer(name, size, usage, b) }
        )
        return this
    }
    fun createVertexBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_VERTEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
    }
    fun createIndexBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_INDEX_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
    }
    fun createUniformBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
    }
    fun createStorageBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_STORAGE_BUFFER_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
    }
    fun createStagingUploadBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
    }
    fun createStagingDownloadBuffer(name:String, memory:VkDeviceMemory, size:Int, usage:VkBufferUsageFlags = 0) : VulkanBuffers {
        return createBuffer(name, memory, size,
            usage or VK_BUFFER_USAGE_TRANSFER_SRC_BIT or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
    }

    override fun toString(): String {
        val buf = StringBuilder("VulkanBuffers {\n")
        map.forEach { k, v ->
            buf.append("\t$k : ").append(v.buffer).append("\n")
        }
        return buf.append("}").toString()
    }
}

