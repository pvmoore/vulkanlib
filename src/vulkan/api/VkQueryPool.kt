package vulkan.api

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueryPoolCreateInfo
import vulkan.misc.VkQueryPipelineStatisticFlags
import vulkan.misc.VkQueryType
import vulkan.misc.check

class VkQueryPool(private val device: VkDevice, val handle:Long) {

    fun destroy() {
        vkDestroyQueryPool(device, handle, null)
    }
}

fun VkDevice.createQueryPool(type:VkQueryType, numQueries:Int, stats:VkQueryPipelineStatisticFlags = 0)
    :VkQueryPool
{
    val info = VkQueryPoolCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO)
        .flags(0)
        .queryType(type)
        .queryCount(numQueries)
        .pipelineStatistics(stats)

    val pPool = MemoryUtil.memAllocLong(1)
    vkCreateQueryPool(this, info, null, pPool).check()

    val pool = VkQueryPool(this, pPool.get(0))

    info.free()
    memFree(pPool)

    return pool
}
