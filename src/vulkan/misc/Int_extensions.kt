package vulkan.misc

import vulkan.common.MEGABYTE

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
