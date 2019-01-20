package vulkan.misc

import org.joml.Vector2f
import org.joml.Vector4f

inline class Degrees(val value:Float) {
    fun toRadians() = (value * (Math.PI / 180.0f)).toFloat()
}
inline class Radians(val value:Float) {
    fun toDegrees() = (value * (180.0f / Math.PI)).toFloat()
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


fun Vector4f.dimension() = Vector2f(x,y)
