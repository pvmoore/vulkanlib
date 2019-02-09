package vulkan.common

import org.lwjgl.glfw.GLFWVulkan.glfwGetPhysicalDevicePresentationSupport
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK10.*
import vulkan.api.getQueues
import vulkan.misc.dump

class Queues(private val client:VulkanClient) {
    private class SelectedQueue(val name:String, val family:Int, val count:Int)

    private lateinit var instance:VkInstance
    private lateinit var physicalDevice:VkPhysicalDevice
    private val queues         = HashMap<String, Array<VkQueue>>()
    private val selectedQueues = ArrayList<SelectedQueue>()

    fun init(instance:VkInstance, physicalDevice:VkPhysicalDevice) {
        this.instance       = instance
        this.physicalDevice = physicalDevice
        selectQueueFamilies()
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
    fun getFamily(name:String) : Int {
        return selectedQueues.first { it.name==name }.family
    }
    fun select(name:String, family:Int, count:Int) {
        selectedQueues.removeIf { it.name==name }
        selectedQueues.add(SelectedQueue(name, family, count))
    }
    fun getSelectedQueueFamilies() : List<Pair<Int,Int>> {
        return selectedQueues.map { it.family to it.count}
    }

    fun isGraphics(f:Int):Boolean = (f and VK_QUEUE_GRAPHICS_BIT) != 0
    fun isCompute(f:Int):Boolean  = (f and VK_QUEUE_COMPUTE_BIT)  != 0
    fun isTransfer(f:Int):Boolean = (f and VK_QUEUE_TRANSFER_BIT) != 0
    fun canPresent(i:Int):Boolean = glfwGetPhysicalDevicePresentationSupport(instance, physicalDevice, i)

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

        MemoryStack.stackPush().use { stack ->
            val count = stack.mallocInt(1)

            fun getQueueFamilyProps(pDevice: VkPhysicalDevice): VkQueueFamilyProperties.Buffer {
                vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, null)
                val queueCount = count.get(0)
                val qFamilies = VkQueueFamilyProperties.callocStack(queueCount)
                vkGetPhysicalDeviceQueueFamilyProperties(pDevice, count, qFamilies)
                return qFamilies
            }

            val queueFamilyProps = getQueueFamilyProps(physicalDevice)
            queueFamilyProps.dump()

            client.selectQueues(queueFamilyProps, this)
        }
        println("\t$this")
    }
}