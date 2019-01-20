package vulkan.misc

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
