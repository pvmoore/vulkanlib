package vulkan.maths

import org.joml.*
import java.text.DecimalFormat

inline class Degrees(val value:Double) {
    fun toRadians() = (value * (Math.PI / 180.0))
}
inline class Radians(val value:Double) {
    fun toDegrees() = (value * (180.0 / Math.PI))
}

/**
 * Make Vector4f behave like a Rect
 */
var Vector4f.width
    get() = z
    set(value) { z = value}

var Vector4f.height
    get() = w
    set(value) { w = value}


fun Vector4f.dimension() = Vector2f(z,w)


fun Float.string()  : String = String.format("%.4f", this)
fun Double.string() : String = String.format("%.6f", this)

fun Vector2i.string():String = "(${this.x}, ${this.y})"
fun Vector2f.string():String = String.format("(%.4f, %.4f)", this.x, this.y)
fun Vector3i.string():String = "(${this.x}, ${this.y},${this.z})"
fun Vector3f.string():String = String.format("(%.4f, %.4f, %.4f)", this.x, this.y, this.z)
fun Vector4f.string():String = String.format("(%.4f, %.4f, %.4f, %.4f)", this.x, this.y, this.z, this.w)

fun Matrix4f.string() : String = this.toString(DecimalFormat("###.####"))

