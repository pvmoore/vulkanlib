package vulkan.misc

import vulkan.common.MEGABYTE
import java.nio.file.Path
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun Boolean.toInt() = if(this) 1 else 0

fun Int.megabytes() = this* MEGABYTE

fun Int.clamp(min:Int, max:Int):Int {
    return Math.max(Math.min(this, max), min)
}

fun Int.isSet(bit:Int):Boolean {
    return (this and bit) != 0
}
fun Int.isUnset(bit:Int):Boolean {
    return (this and bit) == 0
}

fun Int.Companion.sizeof() = 4
fun Float.Companion.sizeof() = 4


fun String.normalisePath(absolute:Boolean=false):String {
    val p = Path.of(this).normalize()
    val s = when(absolute) {
        true  -> p.toAbsolutePath().toString().replace('\\', '/')
        false -> p.toString().replace('\\', '/')
    }
    return when {
        s.endsWith("/") -> s
        else            -> "$s/"
    }
}

inline fun Boolean.then(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if(this) {
        block()
    }
}
inline fun Boolean.ifTrue(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    if(this) {
        block()
    }
}