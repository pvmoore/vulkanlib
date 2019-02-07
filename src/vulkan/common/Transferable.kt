package vulkan.common

import org.joml.*
import org.lwjgl.vulkan.VkCommandBuffer
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.copyBuffer
import vulkan.misc.put
import java.nio.ByteBuffer
import kotlin.test.assertNotNull

interface Transferable {
    fun size():Int
    fun writeTo(dest: ByteBuffer)
}

abstract class AbsTransferable : Transferable {

    private fun lazySize() = lazy {
        var size = 0
        this.javaClass.declaredFields.forEachIndexed { i, field ->
            size += when(field.type.name) {
                "int"               -> 4
                "float"             -> 4
                "org.joml.Vector2f" -> 2*4
                "org.joml.Vector3f" -> 3*4
                "org.joml.Vector4f" -> 4*4
                "org.joml.Vector2i" -> 2*4
                "org.joml.Vector3i" -> 3*4
                "org.joml.Vector4i" -> 4*4
                "org.joml.Matrix4f" -> 16*4
                else -> throw Error("Unknown format for field ${field.type.name}")
            }
        }
        size
    }

    override fun size():Int = lazySize().value

    override fun writeTo(dest: ByteBuffer) {
        this.javaClass.declaredFields.forEachIndexed { i, field ->

            field.isAccessible = true

            when(field.type.name) {
                "int"               -> dest.putInt(field.getInt(this))
                "float"             -> dest.putFloat(field.getFloat(this))
                "org.joml.Vector2f" -> dest.put(field.get(this) as Vector2f)
                "org.joml.Vector3f" -> dest.put(field.get(this) as Vector3f)
                "org.joml.Vector4f" -> dest.put(field.get(this) as Vector4f)
                "org.joml.Vector2i" -> dest.put(field.get(this) as Vector2i)
                "org.joml.Vector3i" -> dest.put(field.get(this) as Vector3i)
                "org.joml.Vector4i" -> dest.put(field.get(this) as Vector4i)
                "org.joml.Matrix4f" -> dest.put(field.get(this) as Matrix4f)
                else -> throw Error("Unknown format for field ${field.type.name}")
            }
        }
    }
    fun transfer(cmd: VkCommandBuffer, staging: BufferAlloc?, dest:BufferAlloc) {

        /** On AMD, we can use shared memory so we can write directly to dest */
        if(dest.type.isHostVisible) {

            dest.mapForWriting { bb->
                this.writeTo(bb)
            }

        } else {
            assertNotNull(staging) {
                it.mapForWriting { bb->
                    this.writeTo(bb)
                }
                cmd.run {
                    copyBuffer(it, dest)
                }
            }

        }
    }
}

abstract class AbsTransferableArray : AbsTransferable() {

    protected abstract fun getArray():Array<out AbsTransferable>

    open fun elementInstance():AbsTransferable = getArray()[0]

    override fun size(): Int = elementInstance().size() * getArray().size

    override fun writeTo(dest: ByteBuffer) {
        getArray().forEach { dest.put(it) }
    }
}

abstract class AbsTransferableShortArray : AbsTransferable() {
    protected abstract fun getArray():ShortArray

    override fun size():Int = 2 * getArray().size

    override fun writeTo(dest: ByteBuffer) {
        getArray().forEach { dest.putShort(it) }
    }
}

abstract class AbsTransferableIntArray : AbsTransferable() {
    protected abstract fun getArray():IntArray

    override fun size():Int = 4 * getArray().size

    override fun writeTo(dest: ByteBuffer) {
        getArray().forEach { dest.putInt(it) }
    }
}

abstract class AbsTransferableFloatArray : AbsTransferable() {
    protected abstract fun getArray():FloatArray

    override fun size():Int = 4 * getArray().size

    override fun writeTo(dest: ByteBuffer) {
        getArray().forEach { dest.putFloat(it) }
    }
}