package vulkan.misc

import java.nio.file.Path

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
