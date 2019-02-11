package vulkan.common

import vulkan.misc.VkColorSpaceKHR
import vulkan.misc.VkFormat

data class SurfaceFormat(var colorFormat: VkFormat = 0, var colorSpace: VkColorSpaceKHR = 0)