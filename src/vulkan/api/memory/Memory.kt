package vulkan.api.memory

import org.lwjgl.system.MemoryUtil.memAllocLong
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.common.MEGABYTE
import vulkan.common.MemoryType
import vulkan.app.VulkanApplication
import vulkan.misc.VkBufferUsageFlags
import vulkan.misc.VkImageUsageFlags
import vulkan.misc.VkMemoryPropertyFlags
import vulkan.misc.check

/**
 *            -------VkDeviceMemory----------
 *            |        |             |      |
 *      --VkBuffer VkBuffer      VkImage VkImage--------
 *     |        |                           |          |
 * BufferAlloc BufferAlloc              ImageAlloc ImageAlloc
 *
 */

fun VulkanApplication.selectDeviceMemoryType(size:Int): MemoryType? {
    val desiredMemFlags   = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
    val undesiredMemFlags = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
    val bufferUsage       = VK_BUFFER_USAGE_TRANSFER_DST_BIT
    val imageUsage        = VK_IMAGE_USAGE_TRANSFER_DST_BIT
    return device.selectMemoryType(physicalDeviceMemoryProperties, desiredMemFlags, undesiredMemFlags, size, bufferUsage, imageUsage)
}
fun VulkanApplication.selectStagingUploadMemoryType(size:Int): MemoryType? {
    val desiredMemFlags   = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
    val undesiredMemFlags = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT or VK_MEMORY_PROPERTY_HOST_CACHED_BIT
    val bufferUsage       = VK_BUFFER_USAGE_TRANSFER_SRC_BIT
    val imageUsage        = VK_IMAGE_USAGE_TRANSFER_SRC_BIT
    return device.selectMemoryType(physicalDeviceMemoryProperties, desiredMemFlags, undesiredMemFlags, size, bufferUsage, imageUsage)
}
fun VulkanApplication.selectSharedMemoryType(size:Int): MemoryType? {
    val desiredMemFlags   = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT or VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
    val undesiredMemFlags = 0
    val bufferUsage       = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT
    val imageUsage        = 0
    return device.selectMemoryType(physicalDeviceMemoryProperties, desiredMemFlags, undesiredMemFlags, size, bufferUsage, imageUsage)
}
fun VulkanApplication.selectMemoryType(size:Int,
                                                  desiredMemFlags:VkMemoryPropertyFlags,
                                                  undesiredMemFlags:VkMemoryPropertyFlags,
                                                  bufferUsage:VkBufferUsageFlags = 0,
                                                  imageUsage:VkImageUsageFlags = 0)
    : MemoryType?
{
    return device.selectMemoryType(physicalDeviceMemoryProperties, desiredMemFlags, undesiredMemFlags, size, bufferUsage, imageUsage)
}

/**
 *  Recommended max size per allocation for GPU heap is 256MB
 *                                          CPU heap is 256MB
 *                                          Shared heap (of 256MB) is 64MB
 */
fun VulkanApplication.allocateMemory(size:Int,
                                                desiredMemFlags:VkMemoryPropertyFlags,
                                                undesiredMemFlags:VkMemoryPropertyFlags,
                                                bufferUsage:VkBufferUsageFlags = 0,
                                                imageUsage:VkImageUsageFlags = 0)
    : VkDeviceMemory?
{
    val type =
        device.selectMemoryType(physicalDeviceMemoryProperties, desiredMemFlags, undesiredMemFlags, size, bufferUsage, imageUsage)
            ?: return null

    return this.allocateMemory(size, type)
}
fun VulkanApplication.allocateMemory(size:Int, type: MemoryType) : VkDeviceMemory {
    assert(size>0)
    assert(type.isSet)

    val info = VkMemoryAllocateInfo.calloc()
        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
        .allocationSize(size.toLong())
        .memoryTypeIndex(type.index)

    val pMem = memAllocLong(1)
    vkAllocateMemory(device, info, null, pMem).check()

    val memory = VkDeviceMemory(this.physicalDeviceProperties, device, pMem.get(0), size, type)

    memFree(pMem)
    info.free()

    return memory
}
//=================================================================================================
private fun VkDevice.selectMemoryType(props: VkPhysicalDeviceMemoryProperties,
                                      desiredMemFlags:VkMemoryPropertyFlags,
                                      undesiredMemFlags:VkMemoryPropertyFlags,
                                      minHeapSize:Int,
                                      bufferUsage:VkBufferUsageFlags,
                                      imageUsage:VkImageUsageFlags)
    : MemoryType?
{
    fun checkMemFlags(flags:VkMemoryPropertyFlags) = (flags and undesiredMemFlags)==0 &&
                                                     (flags and desiredMemFlags)==desiredMemFlags

    fun checkHeapSize(heapIndex:Int) = props.memoryHeaps()[heapIndex].size() >= minHeapSize

    fun checkBufferUsage(typeIndex:Int):Boolean {
        val info = VkBufferCreateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
            .size(1L* MEGABYTE)
            .usage(bufferUsage)
            .flags(0)
            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .pQueueFamilyIndices(null)

        val pLong = memAllocLong(1)
        vkCreateBuffer(this, info, null, pLong)

        val memReqs = VkMemoryRequirements.calloc()
        vkGetBufferMemoryRequirements(this, pLong.get(0), memReqs)

        val isValid = (memReqs.memoryTypeBits() and (1 shl typeIndex)) != 0

        memReqs.free()
        memFree(pLong)
        info.free()

        vkDestroyBuffer(this, pLong.get(0), null)

        return isValid
    }
    fun checkImageUsage(typeIndex:Int):Boolean {
        // todo
//        val info = VkImageCreateInfo.calloc()
//            .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
//            .flags(0)
//            .imageType(VK_IMAGE_TYPE_2D)
//            .format(VK_FORMAT_R8G8B8A8_UNORM)
//            .mipLevels(1)
//            .arrayLayers(1)
//            .samples(1)
//            .tiling(VK_IMAGE_TILING_OPTIMAL)
//            .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
//
//        val pLong = memAllocLong(1)
//
//
//        val isValid = false
//
//        memFree(pLong)
//        info.free()
//
//        return isValid
        return true
    }
    props.memoryTypes().forEachIndexed { i, type ->

        if(checkHeapSize(type.heapIndex()) &&
           checkMemFlags(type.propertyFlags()) &&
           checkBufferUsage(i) &&
           checkImageUsage(i))
        {
            return MemoryType(i, type.propertyFlags())
        }
    }

    return null
}



//fun selectMemoryTypeIndexes(props: VkPhysicalDeviceMemoryProperties, types: MemoryTypes) {
//    println("Selecting memory type indexes")
//
//    fun Int.isLocal()        = (this and VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) != 0
//    fun Int.isHostCached()   = (this and VK_MEMORY_PROPERTY_HOST_CACHED_BIT) != 0
//    fun Int.isHostCoherent() = (this and VK_MEMORY_PROPERTY_HOST_COHERENT_BIT) != 0
//    fun Int.isHostVisible()  = (this and VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT) != 0
//
//    props.memoryTypes().forEachIndexed { i, type ->
//        val flag = type.propertyFlags()
//
//        if(flag.isLocal()) {
//            /** Might be an integrated GPU. Pick this one first and let it be replaced later
//             *  if we find one with more specific requirements */
//            if(!types.GPU.isSet) types.GPU.set(i, type.propertyFlags())
//        }
//        if(flag.isLocal() && !flag.isHostVisible()) {
//            /** This one is the exclusive GPU memory */
//            types.GPU.set(i, type.propertyFlags())
//        }
//        if(!flag.isLocal() && flag.isHostVisible() && flag.isHostCoherent() && !flag.isHostCached()) {
//            types.stagingToGPU.set(i, type.propertyFlags())
//        }
//        if(flag.isLocal() && flag.isHostVisible() && flag.isHostCoherent() && !flag.isHostCached()) {
//            /** This one might not be available */
//            types.shared.set(i, type.propertyFlags())
//        }
//        if(!flag.isLocal() && flag.isHostVisible() && flag.isHostCoherent() && flag.isHostCached()) {
//            types.stagingToCPU.set(i, type.propertyFlags())
//            /** Replace this later with a non-cached type if we find one */
//            if(!types.stagingToGPU.isSet) types.stagingToGPU.set(i, type.propertyFlags())
//        }
//    }
//    if(!types.GPU.isSet || !types.stagingToCPU.isSet || !types.stagingToGPU.isSet) {
//        throw Error("Unable to find appropriate memory type indexes")
//    }
//    if(!types.shared.isSet) {
//        println("\tNo local and host visible memory found")
//    }
//    println("\t$types")
//}