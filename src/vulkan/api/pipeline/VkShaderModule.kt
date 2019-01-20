package vulkan.api.pipeline

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import org.lwjgl.vulkan.VkShaderModuleCreateInfo
import vulkan.misc.check
import java.nio.ByteBuffer

class VkShaderModule(private val device: VkDevice, var handle:Long) {

    fun destroy() {
        if(handle!= VK_NULL_HANDLE) {
            vkDestroyShaderModule(device, handle, null)
            handle = VK_NULL_HANDLE
        }
    }
}

fun VkDevice.createShaderModule(code: ByteBuffer): VkShaderModule {
    val info = VkShaderModuleCreateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
        .pCode(code)

    val ptr = memAllocLong(1)

    vkCreateShaderModule(this, info, null, ptr).check()

    val shader = VkShaderModule(this, ptr.get(0))

    info.free()
    memFree(ptr)

    return shader
}
