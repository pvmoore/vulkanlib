package vulkan.common

import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*
import vulkan.app.*

class InputState {
    private val keysDown         = HashSet<Int>()
    private val mouseButtonsDown = HashSet<Int>()

    var mouseX:Float      = 0f
    var mouseY:Float      = 0f
    var mouseWheelY:Float = 0f
    val drag              = Drag()

    fun isKeyDown(code:Int) = keysDown.contains(code)
    fun isShiftPressed()    = isKeyDown(GLFW_KEY_LEFT_SHIFT) or isKeyDown(GLFW_KEY_RIGHT_SHIFT)
    fun isCtrlPressed()     = isKeyDown(GLFW_KEY_LEFT_CONTROL) or isKeyDown(GLFW_KEY_RIGHT_CONTROL)
    fun isAltPressed()      = isKeyDown(GLFW_KEY_LEFT_ALT) or isKeyDown(GLFW_KEY_RIGHT_ALT)

    fun isMouseButtonDown(button:Int) = mouseButtonsDown.contains(button)

    fun reset() {
        keysDown.clear()
        mouseButtonsDown.clear()
        mouseX = 0f
        mouseY = 0f
        mouseWheelY = 0f
    }
    fun resetMouseWheel() {
        mouseWheelY = 0f
    }

    /**
     * Update properties given the list of events.
     */
    fun update(events:List<WindowEvent>) {
        events.forEach { update(it) }
    }
    fun update(e:WindowEvent) {
        when(e) {
            is KeyEvent         -> {
                if(e.action.press || e.action.repeat) {
                    keysDown.add(e.key)
                } else {
                    keysDown.remove(e.key)
                }
            }
            is MouseWheelEvent  -> { mouseWheelY += e.yDelta }
            is MouseMoveEvent   -> { mouseX = e.x; mouseY = e.y }
            is MouseButtonEvent -> {
                when(e.action.press) {
                    true  -> mouseButtonsDown.add(e.button)
                    false -> mouseButtonsDown.remove(e.button)
                }
            }
        }
    }

    override fun toString() : String {
        return "Keys:$keysDown mouse:$mouseX,$mouseY - $mouseButtonsDown wheel:$mouseWheelY"
    }
    //=========================================================================================
    class Drag {
        var start:Vector2f? = null
        var delta:Vector2f? = null

        fun reset() {
            start = null
            delta = null
        }
    }
}
