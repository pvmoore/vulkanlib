package vulkan.font

import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import vulkan.misc.dimension
import vulkan.misc.height
import vulkan.misc.width
import vulkan.texture.Texture

class Font(var name:String) {
    class Char(
        val id : Int,
        val u : Float,
        val v : Float,
        val u2 : Float,
        val v2 : Float ,
        val width : Int,
        val height : Int ,
        val xoffset : Int,
        val yoffset : Int,
        val xadvance : Int
    )
    internal var chars    = HashMap<Int, Char>()
    internal var kernings = HashMap<Long, Int>() /// key = (from<<32 | to)

    lateinit var texture : Texture
    var size : Int = 0
    var width : Int = 0
    var height : Int = 0
    var lineHeight : Int = 0

    fun getKerning(from : Int, to : Int) : Int {
        return kernings[(from.toLong() shl 32) or to.toLong()] ?: 0
    }
    fun getChar(ch : Int) : Char {
        return chars[ch] ?: chars[32]!!
    }
    fun centreText(text : String, size : Float, mid : Vector2i) : Vector2i {
        val x = getDimension(text, size).x.toInt() / 2
        return Vector2i(mid.x - x, mid.y)
    }
    fun getDimension(text : String, size : Float) : Vector2f {
        return getRect(text, size).dimension()
    }

    fun getRect(text : String, size : Float) : Vector4f {
        val r = Vector4f(0f, 0f, 0f, 0f)
        if(text.isEmpty()) return r

        r.x = 1000f
        r.y = 1000f
        var X = 0f
        val i = 0
        for(ch in text.toCharArray()) {
            val g = getChar(ch.toInt())
            val ratio = size / this.size.toFloat()

            val x = X + g.xoffset * ratio
            val y = 0 + g.yoffset * ratio
            val xx = x + g.width * ratio
            val yy = y + g.height * ratio

            if(x < r.x) r.x = x
            if(y < r.y) r.y = y
            if(xx > r.width) r.width = xx
            if(yy > r.height) r.height = yy

            var kerning = 0
            if(i + 1 < text.length) {
                kerning = getKerning(ch.toInt(), text[i + 1].toInt())
            }
            X += (g.xadvance + kerning) * ratio
        }
        return r
    }
    override fun toString() : String {
        return String.format(
            "Font(%s size: %d width:%d height:%d chars:%d kernings:%d)",
            name, size, width, height, chars.size, kernings.size
        )
    }
}
