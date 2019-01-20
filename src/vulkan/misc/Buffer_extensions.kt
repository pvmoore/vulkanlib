package vulkan.misc

import org.joml.*
import org.lwjgl.PointerBuffer
import vulkan.common.Transferable
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer
import kotlin.contracts.contract

fun ByteBuffer.put(v: Vector2f) {
    this.putFloat(v.x)
    this.putFloat(v.y)
}
fun ByteBuffer.put(v: Vector3f) {
    this.putFloat(v.x)
    this.putFloat(v.y)
    this.putFloat(v.z)
}
fun ByteBuffer.put(v: Vector4f) {
    this.putFloat(v.x)
    this.putFloat(v.y)
    this.putFloat(v.z)
    this.putFloat(v.w)
}
fun ByteBuffer.put(v: Vector2i) {
    this.putInt(v.x)
    this.putInt(v.y)
}
fun ByteBuffer.put(v: Vector3i) {
    this.putInt(v.x)
    this.putInt(v.y)
    this.putInt(v.z)
}
fun ByteBuffer.put(v: Vector4i) {
    this.putInt(v.x)
    this.putInt(v.y)
    this.putInt(v.z)
    this.putInt(v.w)
}

fun ByteBuffer.put(m: Matrix4f) {
    m.get(this)
    position(position()+64)
}

fun ByteBuffer.put(t: Transferable) : ByteBuffer {
    t.writeTo(this)
    return this
}

fun IntBuffer.skip(count:Int) : IntBuffer {
    this.position(this.position()+count)
    return this
}
fun IntBuffer.getAndSkip(count:Int) : Int {
    val v = this.get()
    this.position(this.position()+count)
    return v
}

fun ByteBuffer.string():String {
    val b = StringBuilder()
    for(i in 0 until this.limit()) {
        if(i>0) b.append(", ")
        b.append(this.get(i))
    }
    return b.toString()
}
fun FloatBuffer.string():String {
    val b = StringBuilder()
    for(i in 0 until this.limit()) {
        if(i>0) b.append(", ")
        b.append(this.get(i))
    }
    return b.toString()
}

inline fun PointerBuffer.forEach(action: (Long) -> Unit) {
    for(i in 0 until this.limit()) {
        action(this.get(i))
    }
}
inline fun PointerBuffer.forEachIndexed(action: (Int, Long) -> Unit) {
    for(i in 0 until this.limit()) {
        action(i, this.get(i))

    }
}
inline fun LongBuffer.forEach(action: (Long) -> Unit) {
    for(i in 0 until this.limit()) {
        action(this.get(i))
    }
}

inline fun <T> PointerBuffer.toList(action: (Long) -> T):List<T> {
    contract {
        returnsNotNull()
    }
    val list = ArrayList<T>()
    for(i in 0 until this.limit()) {
        list.add(action(this.get(i)))
    }
    return list
}
fun LongBuffer.writeTo(c:MutableCollection<Long>) {
    for(i in 0 until this.limit()) {
        c.add(get(i))
    }
}