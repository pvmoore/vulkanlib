package vulkan.d2

import org.joml.Vector2f
import vulkan.common.FrameInfo
import vulkan.common.PerFrameResource
import vulkan.common.RenderContext
import vulkan.misc.RGBA

class FPS(private val suffix:String = " fps",
          private val colour:RGBA   = RGBA(1f,1f,0.7f,1f),
          private var pos:Vector2f? = null)
{
    private lateinit var context:RenderContext

    private val text = Text()

    fun init(context:RenderContext) : FPS {
        if(pos==null) {
            pos = Vector2f(context.vk.graphics.windowSize.x-135f, 5f)
        }

        this.context = context
        this.text.init(context, context.vk.graphics.fonts.get("segoe-ui"), 100, true)
                 .setColour(colour)
                 .setSize(24f)
                 .appendText(".....", pos!!)

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
        text.updateText(0, String.format("%.2f%s", context.vk.graphics.fps, suffix))
        text.beforeRenderPass(frame, res)
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        text.insideRenderPass(frame, res)
    }
}
