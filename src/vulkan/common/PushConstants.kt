package vulkan.common

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PushConstants(val count:Int) {
    val floatBuffer:FloatBuffer = ByteBuffer.allocateDirect(size)
                                            .order(ByteOrder.nativeOrder())
                                            .asFloatBuffer()

    fun set(index:Int, value:Float) : PushConstants {
        floatBuffer.put(index, value)
        return this
    }

    val size get() = count * 4
}
