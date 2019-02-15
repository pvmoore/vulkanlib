package vulkan.app

import org.lwjgl.glfw.GLFW.*

class InputState {
    private val keysDown         = HashSet<Int>()
    private val mouseButtonsDown = HashSet<Int>()

    var mouseX:Float      = 0f
    var mouseY:Float      = 0f
    var mouseWheelY:Float = 0f

    fun isKeyDown(code:Int) = keysDown.contains(code)
    fun isShiftPressed()    = isKeyDown(GLFW_KEY_LEFT_SHIFT) or isKeyDown(GLFW_KEY_RIGHT_SHIFT)
    fun isCtrlPressed()     = isKeyDown(GLFW_KEY_LEFT_CONTROL) or isKeyDown(GLFW_KEY_RIGHT_CONTROL)
    fun isAltPressed()      = isKeyDown(GLFW_KEY_LEFT_ALT) or isKeyDown(GLFW_KEY_RIGHT_ALT)

    fun isMouseButtonDown(button:Int) = mouseButtonsDown.contains(button)
    fun isAnyMouseButtonDown()   = mouseButtonsDown.isNotEmpty()
    fun whichMouseButtonIsDown() = if(mouseButtonsDown.isNotEmpty()) mouseButtonsDown.first() else -1

    /**
     * Update properties with this event.
     */
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
}
