package vulkan.app


import org.lwjgl.glfw.GLFW.*

abstract class WindowEvent

class KeyMods(mods:Int) {
    val shift = (mods and GLFW_MOD_SHIFT) != 0
    val ctrl  = (mods and GLFW_MOD_CONTROL) != 0
    val alt   = (mods and GLFW_MOD_ALT) != 0
}
class KeyAction(action:Int) {
    val press  = action == GLFW_PRESS
    val repeat = action == GLFW_REPEAT
}

class KeyEvent(val key:Int,
               val scancode:Int,
               val action:KeyAction,
               val mods:KeyMods) : WindowEvent()

class MouseButtonEvent(val button:Int,
                       val action:KeyAction,
                       val mods:KeyMods) : WindowEvent()

class MouseMoveEvent(val x:Float,
                     val y:Float) : WindowEvent()

class MouseWheelEvent(val xDelta:Float,
                      val yDelta:Float) : WindowEvent()
