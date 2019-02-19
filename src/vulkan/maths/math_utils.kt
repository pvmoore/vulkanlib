package vulkan.maths

import org.joml.*
import java.text.DecimalFormat

typealias float2 = Vector2f
typealias float3 = Vector3f
typealias float4 = Vector4f
typealias int2   = Vector2i
typealias int3   = Vector3i
typealias int4   = Vector4i

inline class Degrees(val value:Double) {
    fun toRadians() = (value * (Math.PI / 180.0))
}
inline class Radians(val value:Double) {
    fun toDegrees() = (value * (180.0 / Math.PI))
}

fun Float.string()  : String = String.format("%.4f", this)
fun Double.string() : String = String.format("%.6f", this)

fun Vector2i.toVector2f() = Vector2f(x.toFloat(), y.toFloat())
fun Vector3i.toVector2f() = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
fun Vector4i.toVector2f() = Vector4f(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

fun Vector2f.toVector2i() = Vector2i(x.toInt(), y.toInt())
fun Vector3f.toVector3i() = Vector3i(x.toInt(), y.toInt(), z.toInt())
fun Vector4f.toVector4i() = Vector4i(x.toInt(), y.toInt(), z.toInt(), w.toInt())

fun Vector2f.allGT(v:Vector2f)  = x>v.x && y>v.y
fun Vector2f.allGTE(v:Vector2f) = x>=v.x && y>=v.y
fun Vector2f.allLT(v:Vector2f)  = x<v.x && y<v.y
fun Vector2f.allLTE(v:Vector2f) = x<=v.x && y<=v.y

/** Add some operator overloads to Vectors */

/** Vector2f */
fun Vector2f.copy():Vector2f = Vector2f(x,y)
operator fun Vector2f.unaryMinus():Vector2f   = Vector2f(-x,-y)
operator fun Vector2f.plus(f:Float):Vector2f  = Vector2f(x+f, y+f)
operator fun Vector2f.minus(f:Float):Vector2f = Vector2f(x-f, y-f)
operator fun Vector2f.times(f:Float):Vector2f = Vector2f(x*f, y*f)
operator fun Vector2f.div(f:Float):Vector2f   = Vector2f(x,y).mul(1f/f)
operator fun Vector2f.rem(f:Float):Vector2f   = Vector2f(x%f, y%f)

operator fun Vector2f.plusAssign(f:Float)  { x+=f; y+=f }
operator fun Vector2f.minusAssign(f:Float) { x-=f; y-=f }
operator fun Vector2f.timesAssign(f:Float) { x*=f; y*=f }
operator fun Vector2f.divAssign(f:Float)   { x/=f; y/=f }
operator fun Vector2f.remAssign(f:Float)   { x%=f; y%=f }

operator fun Vector2f.plus(v:Vector2f):Vector2f  = Vector2f(x+v.x, y+v.y)
operator fun Vector2f.minus(v:Vector2f):Vector2f = Vector2f(x-v.x, y-v.y)
operator fun Vector2f.times(v:Vector2f):Vector2f = Vector2f(x*v.x, y*v.y)
operator fun Vector2f.div(v:Vector2f):Vector2f   = Vector2f(x/v.x, y/v.y)
operator fun Vector2f.rem(v:Vector2f):Vector2f   = Vector2f(x%v.x, y%v.y)

operator fun Vector2f.plusAssign(v:Vector2f)  { x+=v.x; y+=v.y }
operator fun Vector2f.minusAssign(v:Vector2f) { x-=v.x; y-=v.y }
operator fun Vector2f.timesAssign(v:Vector2f) { x*=v.x; y*=v.y }
operator fun Vector2f.divAssign(v:Vector2f)   { x/=v.x; y/=v.y }
operator fun Vector2f.remAssign(v:Vector2f)   { x%=v.x; y%=v.y }

/** Vector3f */
fun Vector3f.copy():Vector3f = Vector3f(x,y,z)
operator fun Vector3f.unaryMinus():Vector3f   = Vector3f(-x,-y,-z)
operator fun Vector3f.plus(f:Float):Vector3f  = Vector3f(x+f, y+f, z+f)
operator fun Vector3f.minus(f:Float):Vector3f = Vector3f(x-f, y-f, z-f)
operator fun Vector3f.times(f:Float):Vector3f = Vector3f(x*f, y*f, z*f)
operator fun Vector3f.div(f:Float):Vector3f   = Vector3f(x,y,z).mul(1f/f)
operator fun Vector3f.rem(f:Float):Vector3f   = Vector3f(x%f, y%f, z%f)

operator fun Vector3f.plusAssign(f:Float)  { x+=f; y+=f; z+=f }
operator fun Vector3f.minusAssign(f:Float) { x-=f; y-=f; z-=f }
operator fun Vector3f.timesAssign(f:Float) { x*=f; y*=f; z*=f }
operator fun Vector3f.divAssign(f:Float)   { x/=f; y/=f; z/=f }
operator fun Vector3f.remAssign(f:Float)   { x%=f; y%=f; z%=f }

operator fun Vector3f.plus(v:Vector3f):Vector3f  = Vector3f(x+v.x, y+v.y, z+v.z)
operator fun Vector3f.minus(v:Vector3f):Vector3f = Vector3f(x-v.x, y-v.y, z-v.z)
operator fun Vector3f.times(v:Vector3f):Vector3f = Vector3f(x*v.x, y*v.y, z*v.z)
operator fun Vector3f.div(v:Vector3f):Vector3f   = Vector3f(x/v.x, y/v.y, z/v.z)
operator fun Vector3f.rem(v:Vector3f):Vector3f   = Vector3f(x%v.x, y%v.y, z%v.z)

operator fun Vector3f.plusAssign(v:Vector3f)  { x+=v.x; y+=v.y; z+=v.z }
operator fun Vector3f.minusAssign(v:Vector3f) { x-=v.x; y-=v.y; z-=v.z }
operator fun Vector3f.timesAssign(v:Vector3f) { x*=v.x; y*=v.y; z*=v.z }
operator fun Vector3f.divAssign(v:Vector3f)   { x/=v.x; y/=v.y; z/=v.z }
operator fun Vector3f.remAssign(v:Vector3f)   { x%=v.x; y%=v.y; z%=v.z }

fun Vector3f.xy()  = Vector2f(x,y)

/** Vector4f */
fun Vector4f.copy():Vector4f = Vector4f(x,y,z,w)
operator fun Vector4f.unaryMinus():Vector4f   = Vector4f(-x,-y,-z,-w)
operator fun Vector4f.plus(f:Float):Vector4f  = Vector4f(x+f, y+f, z+f, w+f)
operator fun Vector4f.minus(f:Float):Vector4f = Vector4f(x-f, y-f, z-f, w-f)
operator fun Vector4f.times(f:Float):Vector4f = Vector4f(x*f, y*f, z*f, w*f)
operator fun Vector4f.div(f:Float):Vector4f   = Vector4f(x,y,z,w).mul(1f/f)
operator fun Vector4f.rem(f:Float):Vector4f   = Vector4f(x%f, y%f, z%f, w%f)

operator fun Vector4f.plusAssign(f:Float)  { x+=f; y+=f; z+=f; w+=f }
operator fun Vector4f.minusAssign(f:Float) { x-=f; y-=f; z-=f; w-=f }
operator fun Vector4f.timesAssign(f:Float) { x*=f; y*=f; z*=f; w*=f }
operator fun Vector4f.divAssign(f:Float)   { x/=f; y/=f; z/=f; w/=f }
operator fun Vector4f.remAssign(f:Float)   { x%=f; y%=f; z%=f; w%=f }

operator fun Vector4f.plus(v:Vector4f):Vector4f  = Vector4f(x+v.x, y+v.y, z+v.z, w+v.w)
operator fun Vector4f.minus(v:Vector4f):Vector4f = Vector4f(x-v.x, y-v.y, z-v.z, w-v.w)
operator fun Vector4f.times(v:Vector4f):Vector4f = Vector4f(x*v.x, y*v.y, z*v.z, w*v.w)
operator fun Vector4f.div(v:Vector4f):Vector4f   = Vector4f(x/v.x, y/v.y, z/v.z, w/v.w)
operator fun Vector4f.rem(v:Vector4f):Vector4f   = Vector4f(x%v.x, y%v.y, z%v.z, w%v.w)

operator fun Vector4f.plusAssign(v:Vector4f)  { x+=v.x; y+=v.y; z+=v.z; w+=v.w }
operator fun Vector4f.minusAssign(v:Vector4f) { x-=v.x; y-=v.y; z-=v.z; w-=v.w }
operator fun Vector4f.timesAssign(v:Vector4f) { x*=v.x; y*=v.y; z*=v.z; w*=v.w }
operator fun Vector4f.divAssign(v:Vector4f)   { x/=v.x; y/=v.y; z/=v.z; w/=v.w }
operator fun Vector4f.remAssign(v:Vector4f)   { x%=v.x; y%=v.y; z%=v.z; w%=v.w }

operator fun Vector4f.times(m:Matrix4f):Vector4f = this.mul(m)

fun Vector4f.xy()  = Vector2f(x,y)
fun Vector4f.xyz() = Vector3f(x,y,z)

/** Make Vector4f behave like a Rect */
var Vector4f.width
    get() = z
    set(value) { z = value}

var Vector4f.height
    get() = w
    set(value) { w = value}


fun Vector4f.dimension() = Vector2f(z,w)

fun Vector2i.string():String = "(${this.x}, ${this.y})"
fun Vector2f.string():String = String.format("(%.4f, %.4f)", this.x, this.y)
fun Vector3i.string():String = "(${this.x}, ${this.y},${this.z})"
fun Vector3f.string():String = String.format("(%.4f, %.4f, %.4f)", this.x, this.y, this.z)
fun Vector4f.string():String = String.format("(%.4f, %.4f, %.4f, %.4f)", this.x, this.y, this.z, this.w)

fun Matrix4f.string() : String = this.toString(DecimalFormat("###.####"))

