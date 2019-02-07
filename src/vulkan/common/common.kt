package vulkan.common

import org.apache.log4j.Logger
import vulkan.misc.VkColorSpaceKHR
import vulkan.misc.VkFormat

val DEBUG:Boolean = "true" == System.getProperty("DEBUG_MODE", "false")

const val KILOBYTE:Int = 1024
const val MEGABYTE:Int = 1024*1024

const val THOUSAND:Long = 1_000L
const val MILLION:Long  = THOUSAND*THOUSAND
const val BILLION:Long  = MILLION*1000

internal val log:Logger = Logger.getLogger("Global")

data class SurfaceFormat(var colorFormat:VkFormat = 0,var colorSpace:VkColorSpaceKHR = 0)

