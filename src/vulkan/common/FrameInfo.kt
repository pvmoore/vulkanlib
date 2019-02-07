package vulkan.common

/**
 * @param number    The number of times <render> has been called
 * @param relNumber Frame number relative to <targetFPS> eg. if speed is
 *                  <targetFPS> fps then relativeNumber==number.
 *                  If we are rendering at 120 fps and <targetFPS>=60
 *                  then relativeNumber will be number/2.
 * @param delta     Speed delta relative to <targetFPS> fps.
 *                  If <targetFPS>=60 and actual FPS=120 then delta will be 0.5
 *                  to slow down animations by half.
 * @param seconds   Total number of elapsed seconds.
 */
data class FrameInfo(
    var number:Long,
    var relNumber:Double,
    var delta:Double,
    var seconds:Double)
