package vulkan.misc

import org.lwjgl.BufferUtils.createByteBuffer
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.ByteBuffer

fun <T : Any> T?.orThrow(msg:String="") : T {
    if(this==null) throw NullPointerException(msg)
    return this
}

fun readBinaryFile(file: File) : ByteBuffer {
    val array = file.readBytes()
    val buffer = createByteBuffer(array.size)
    return buffer.put(array).flip()
}
fun readBinaryResource(filename: String) : ByteBuffer {
    try{
        BufferedInputStream(Thread.currentThread().contextClassLoader.getResourceAsStream(filename)).use { stream ->

            val array  = stream.readAllBytes()
            val buffer = createByteBuffer(array.size)
            buffer.put(array)

            return buffer.flip()
        }
    }catch(e: Exception) {
        throw Error("Can't read resource '$filename': ", e)
    }

}
fun readStringResource(filename: String) : String {
    BufferedReader(InputStreamReader(
        Thread.currentThread().contextClassLoader.getResourceAsStream(filename)
    )).use { reader->
        return reader.readText()
    }
}
