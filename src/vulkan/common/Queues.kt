package vulkan.common

import org.lwjgl.glfw.GLFWVulkan.glfwGetPhysicalDevicePresentationSupport
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.getQueues
import vulkan.misc.QueueFamily
import vulkan.misc.VkQueueFlags
import vulkan.misc.dump

class Queues(private val client:VulkanClient) {
    private class SelectedQueue(val name:String, val family:Int, val count:Int)

    private lateinit var instance:VkInstance
    private lateinit var physicalDevice:VkPhysicalDevice
    private lateinit var queueFamilyProps:VkQueueFamilyProperties.Buffer

    private val queues         = HashMap<String, Array<VkQueue>>()
    private val selectedQueues = ArrayList<SelectedQueue>()

    fun init(instance:VkInstance, physicalDevice:VkPhysicalDevice) {
        this.instance         = instance
        this.physicalDevice   = physicalDevice
        this.queueFamilyProps = getQueueFamilyProps(physicalDevice)

        selectQueueFamilies()
    }
    fun destroy() {
        queueFamilyProps.free()
    }
    fun deviceCreated(device: VkDevice) {
        /** Fetch the queues */
        selectedQueues.forEach {
            queues[it.name] = device.getQueues(it.family, it.count)
        }
    }

    fun hasQueue(name:String) = queues.containsKey(name)
    fun totalQueues() = selectedQueues.sumBy { it.count }

    fun get(name:String, index:Int = 0) : VkQueue {
        queues[name]?.let { return it[index] }
        throw Error("Queue $name not found")
    }
    fun getFamily(name:String) : QueueFamily {
        return QueueFamily(selectedQueues.first { it.name==name }.family)
    }
    fun select(name:String, family:Int, count:Int) {
        selectedQueues.removeIf { it.name==name }
        selectedQueues.add(SelectedQueue(name, family, count))
    }
    fun isSelected(name:String) : Boolean {
        return selectedQueues.firstOrNull { it.name == name } != null
    }
    fun getSelectedQueueFamilies() : List<Pair<Int,Int>> {
        return selectedQueues.map { it.family to it.count}
    }

    fun isGraphics(flags:Int):Boolean = (flags and VK_QUEUE_GRAPHICS_BIT) != 0
    fun isCompute(flags:Int):Boolean  = (flags and VK_QUEUE_COMPUTE_BIT)  != 0
    fun isTransfer(flags:Int):Boolean = (flags and VK_QUEUE_TRANSFER_BIT) != 0

    fun hasGraphics(family: QueueFamily):Boolean {
        return (queueFamilyProps[family.index].queueFlags() and VK_QUEUE_GRAPHICS_BIT) !=0
    }
    fun hasCompute(family: QueueFamily):Boolean {
        return (queueFamilyProps[family.index].queueFlags() and VK_QUEUE_COMPUTE_BIT) !=0
    }
    fun canTransfer(family: QueueFamily):Boolean {
        return (queueFamilyProps[family.index].queueFlags() and VK_QUEUE_TRANSFER_BIT) !=0
    }
    fun canPresent(family: QueueFamily):Boolean {
        return glfwGetPhysicalDevicePresentationSupport(instance, physicalDevice, family.index)
    }

    /**
     *  @return the first family with the given flags, or -1 if none found
     */
    fun findFirstFamilyWith(flags:VkQueueFlags) : VkQueueFlags {
        val list = findAllFamiliesWith(flags)
        if(list.isNotEmpty()) return list.first()
        return -1
    }
    /**
     *  @return all families with the given flags
     */
    fun findAllFamiliesWith(flags:VkQueueFlags) : List<VkQueueFlags> {
        val list = mutableListOf<VkQueueFlags>()

        queueFamilyProps.forEachIndexed { index, prop ->
            if(prop.queueCount()>0 && (prop.queueFlags() and flags)==flags) {
                list.add(index)
            }
        }
        return list
    }

    override fun toString() : String {
        val buf = StringBuilder("Selected queues ==>")
        selectedQueues.forEachIndexed { i, it ->
            if(i>0) buf.append(", ")
            buf.append("'${it.name}' family:${it.family} count:${it.count} ")
        }
        return buf.toString()
    }

    companion object {
        const val GRAPHICS = "_graphics"
        const val TRANSFER = "_transfer"
        const val COMPUTE  = "_compute"
    }
    //========================================================================================
    private fun selectQueueFamilies() {
        log.info("Selecting queue families")

        client.selectQueues(queueFamilyProps, this)

        println("\t$this")
    }
    private fun getQueueFamilyProps(pDevice: VkPhysicalDevice): VkQueueFamilyProperties.Buffer {
        val count = MemoryUtil.memAllocInt(1)
        vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, null)
        val queueCount = count.get(0)
        val qFamilies = VkQueueFamilyProperties.calloc(queueCount)
        vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, qFamilies)
        MemoryUtil.memFree(count)

        qFamilies.dump()

        return qFamilies
    }
}