package vulkan.api

import org.lwjgl.system.MemoryUtil
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkQueryPoolCreateInfo
import vulkan.misc.*

class VkQueryPool(private val device: VkDevice, val handle:Long) {

    fun getResults(first:Int, count:Int, waitFor:Boolean, includeAvailability : Boolean) : List<Long> {

        val results = ArrayList<Long>()
        val buf     = MemoryUtil.memAllocLong(count + includeAvailability.toInt() )
        val flags   = VK_QUERY_RESULT_64_BIT or (if(waitFor) VK_QUERY_RESULT_WAIT_BIT else 0) or
                        (if(includeAvailability) VK_QUERY_RESULT_WITH_AVAILABILITY_BIT else 0)

        when(val res = vkGetQueryPoolResults(device, handle, first, count, buf, 8L, flags)) {
            VK_SUCCESS   -> buf.forEach { results.add(it) }
            VK_NOT_READY -> {
                /** This is not an error - just return no results */
            }
            else -> res.check()
        }

        memFree(buf)
        return results
    }

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
