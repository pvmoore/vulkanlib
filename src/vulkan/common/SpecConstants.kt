package vulkan.common

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class SpecConstants(val numConstants:Int) {
    val byteBuffer:ByteBuffer   = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
    val floatBuffer:FloatBuffer = byteBuffer.asFloatBuffer()

    fun set(index:Int, value:Float) : SpecConstants {
        floatBuffer.put(index, value)
        return this
    }

    val size get() = numConstants * 4
}
