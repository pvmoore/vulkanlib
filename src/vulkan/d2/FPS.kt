package vulkan.d2

import vulkan.common.FrameInfo
import vulkan.common.PerFrameResource
import vulkan.common.RenderBuffers
import vulkan.common.RenderContext
import vulkan.misc.RGBA

class FPS(private val suffix:String = " fps",
          private val colour: RGBA = RGBA(1f,1f,0.7f,1f),
          private var x:Int = -1,
          private var y:Int = -1)
{
    private lateinit var context:RenderContext
    private val text = Text()

    fun init(context:RenderContext, buffers:RenderBuffers) : FPS {
        if(x==-1 || y==-1) {
            x = context.vk.windowSize.x-135
            y = 5
        }

        this.context = context
        this.text.init(context, buffers, context.vk.fonts.get("segoe-ui"), 100, true)
                 .setColour(colour)
                 .setSize(24f)
                 .appendText(".....", x,y)

        return this
    }
    fun destroy() {
        text.destroy()
    }
    fun camera(camera:Camera2D) : FPS {
        text.camera(camera)
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        text.updateText(0, String.format("%.2f%s", context.vk.fps, suffix))
        text.beforeRenderPass(frame, res)
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        text.insideRenderPass(frame, res)
    }
}
