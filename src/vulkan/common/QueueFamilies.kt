package vulkan.common

data class QueueFamilies(
    var graphics:Int          = -1,
    var compute:Int           = -1,
    var transfer:Int          = -1,
    var numGraphicsQueues:Int = 0,
    var numComputeQueues:Int  = 0,
    var numTransferQueues:Int = 0)