package vulkan.font

import org.apache.log4j.Logger
import org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM
import vulkan.VulkanApplication
import vulkan.misc.megabytes
import vulkan.misc.readStringResource
import vulkan.texture.Textures
import java.util.regex.Pattern

object Fonts {
    private const val resourceDirectory:String = "fonts/"
    private val SIZE = 512

    private lateinit var vk:VulkanApplication

    private val log: Logger = Logger.getLogger("Fonts")
    private val textures    = Textures("Fonts")
    private val map         = HashMap<String, Font>()

    fun init(vk:VulkanApplication) {
        log.info("Initialising Fonts")
        this.vk = vk
        this.textures.init(vk, 10.megabytes())
        log.info("Created 10 MB of font memory")
    }
    fun destroy() {
        log.info("Initialising Fonts")
        map.entries.forEach { e->
            log.info("Destroying font ${e.key}")
        }
        textures.destroy()
    }
    fun get(name:String) : Font {
        return map.getOrPut(name) { loadFont(name) }
    }
    //==============================================================================================
    private fun loadFont(name:String): Font {
        log.info("Loading font $name")

        val font = Font(name)
        readPage(font, name)

        font.texture = textures.get("$name.png", VK_FORMAT_R8_UNORM)
        log.info("Got texture ${font.texture}")

        return font
    }
    private fun readPage(font : Font, name : String) {

        val text = readStringResource("$resourceDirectory$name.fnt")

        val getFirstToken = { line:String, offset:Int ->
            var p = offset
            while(p < line.length && line[p] > ' ') p++
            line.substring(offset, p)
        }
        val getInt = { line:String, key:String ->
            val p = line.indexOf(key)
            val token = getFirstToken(line, p + key.length)
            token.toInt()
        }
        val readChar = { lineIn:String ->
            val line = lineIn.trim()

            // assumes there are no spaces around '='
            val tokens = line.split(Pattern.compile("\\s+"))

            val map = HashMap<String, String>()
            for(it in tokens) {
                val pair = it.split("=")
                map[pair[0]] = pair[1]
            }
            val x = map["x"]!!.toFloat()
            val y = map["y"]!!.toFloat()

            val width  = map["width"]!!.toInt()
            val height = map["height"]!!.toInt()

            Font.Char(
                id       = map["id"]!!.toInt(),
                width    = width,
                height   = height,
                xoffset  = map["xoffset"]!!.toInt(),
                yoffset  = map["yoffset"]!!.toInt(),
                xadvance = map["xadvance"]!!.toInt(),
                u        =  x / SIZE,
                v        =  y / SIZE,
                u2       = (x + width - 1) / SIZE,
                v2       = (y + height - 1) / SIZE
            )
        }

        text.lines().forEach { lineIn ->

            val line = lineIn.trim()
            if(!line.isEmpty()) {

                val firstToken = getFirstToken(line, 0)

                when(firstToken) {
                    "char" -> {
                        val fc = readChar(line.substring(4))
                        font.chars[fc.id] = fc
                    }
                    "kerning" -> {
                        val first = getInt(line, "first=")
                        val second = getInt(line, "second=")
                        val amount = getInt(line, "amount=")
                        font.kernings[(first.toLong() shl 32) or second.toLong()] = amount
                    }
                    "info" -> font.size = getInt(line, "size=")
                    "common" -> {
                        font.width      = getInt(line, "scaleW=")
                        font.height     = getInt(line, "scaleH=")
                        font.lineHeight = getInt(line, "lineHeight=")

                        assert(font.width==SIZE) { "Expecting png width to be $SIZE" }
                    }
                }
            }
        }
    }
}
