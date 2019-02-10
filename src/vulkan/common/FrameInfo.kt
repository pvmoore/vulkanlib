package vulkan.common

/**
 * @param number    The number of times <render> has been called
 * @param seconds   Total number of elapsed seconds.
 * @param perSecond 1.0 / frames per second.
 *                  Multiply by this to keep calculations relative to frame speed.
 */
data class FrameInfo(
    var number:Long,
    var seconds:Double,
    var perSecond:Double)
