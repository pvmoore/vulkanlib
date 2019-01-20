package vulkan.texture

import org.apache.log4j.Logger
import org.lwjgl.stb.STBImage.stbi_load_from_memory
import org.lwjgl.system.MemoryUtil.memAllocInt
import org.lwjgl.system.MemoryUtil.memFree
import org.lwjgl.vulkan.VK10.*
import vulkan.misc.VkFormat
import vulkan.misc.readBinaryResource
import java.io.File
import java.nio.ByteBuffer

object ImageLoader {
    private const val resourceDirectory = "images/"
    private val log: Logger = Logger.getLogger("Textures")
    private val map = HashMap<String, RawImageData>()

    internal fun destroy() {
        map.values.forEach { memFree(it.data) }
    }

    /**
     *
     */
    fun load(name:String, desiredFormat:VkFormat = 0): RawImageData {
        log.info("Loading image \"$name\"")

        val data = readBinaryResource(resourceDirectory + name)

        val ext = File(name).extension.toLowerCase()

        return when(ext) {
            "dds" -> {
                assert(desiredFormat==0)
                loadDDS(name, data)
            }
            else -> loadStandard(name, data, desiredFormat)
        }
    }
    fun free(img: RawImageData) {
        map.remove(img.name)?.let { /*memFree(it.data)*/ }
    }
    //=========================================================================================
    // Private
    //=========================================================================================
    private fun loadStandard(name:String, data: ByteBuffer, desiredFormat:VkFormat): RawImageData {

        val desiredChannels = when(desiredFormat) {
            0                         -> 0
            VK_FORMAT_R8_UNORM        -> 4 // extract the alpha channel later
            VK_FORMAT_R8G8B8_UNORM    -> 3
            VK_FORMAT_R8G8B8A8_UNORM  -> 4
            else -> throw Error("Unsupported desired format: $desiredFormat")
        }

        val x     = memAllocInt(1)
        val y     = memAllocInt(1)
        val ch    = memAllocInt(1)
        val bytes = stbi_load_from_memory(data, x,y,ch, desiredChannels)
            ?: throw Error("Error loading image $name")

        log.info("Loaded image data: ${x.get(0)}, ${y.get(0)}, ${bytes.limit()}")

        fun oneChannel() : RawImageData {
            /** Convert to desired number of channels. This is to get around what look like a bug
             * when converting 4 channels to 1 in stb */
            val alpha = extractAlphaChannel(bytes, x.get(0)*y.get(0))
            memFree(bytes)
            return RawImageData(name, alpha, x.get(0), y.get(0), 1, 1, x.get(0)*y.get(0),  VK_FORMAT_R8_UNORM)
        }
        fun threeChannels() : RawImageData {
            return RawImageData(name, bytes, x.get(0), y.get(0), 3, 1, 3*x.get(0)*y.get(0), VK_FORMAT_R8G8B8_UNORM)
        }
        fun fourChannels() : RawImageData {
            return RawImageData(name, bytes, x.get(0), y.get(0), 4, 1, 4*x.get(0)*y.get(0), VK_FORMAT_R8G8B8A8_UNORM)
        }

        val img = when(desiredFormat) {
            VK_FORMAT_R8_UNORM -> oneChannel()
            VK_FORMAT_R8G8B8_UNORM -> threeChannels()
            VK_FORMAT_R8G8B8A8_UNORM -> fourChannels()
            else -> when(ch.get(0)) {
                3 -> threeChannels()
                4 -> fourChannels()
                else -> throw Error("Unsupported number of channels: ${ch.get(0)}")
            }
        }

        map[img.name] = img

        memFree(x)
        memFree(y)
        memFree(ch)

        return img
    }
    private fun loadDDS(name:String, data: ByteBuffer): RawImageData {
        log.info("Loading DDS")

        return DDSLoader.load(name, data)
    }
    fun extractAlphaChannel(data:ByteBuffer, size:Int) : ByteBuffer {
        assert(data.limit()==size*4)

        val alpha = ByteBuffer.allocateDirect(size)

        (0 until size).forEach { i->
            val src = i*4+3
            alpha.put(data.get(src))
        }

        return alpha.flip()
    }
}
