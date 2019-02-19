package vulkan.app


import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*

abstract class WindowEvent

class KeyMods(mods:Int) {
    val shift = (mods and GLFW_MOD_SHIFT) != 0
    val ctrl  = (mods and GLFW_MOD_CONTROL) != 0
    val alt   = (mods and GLFW_MOD_ALT) != 0
}
class KeyAction(action:Int) {
    val press   = action == GLFW_PRESS
    val release = action == GLFW_RELEASE
    val repeat  = action == GLFW_REPEAT
}

class KeyEvent(val key:Int,
               val scancode:Int,
               val action:KeyAction,
               val mods:KeyMods) : WindowEvent()

class MouseButtonEvent(val button:Int,
                       val pos:Vector2f,
                       val action:KeyAction,
                       val mods:KeyMods) : WindowEvent()

class MouseMoveEvent(val x:Float,
                     val y:Float) : WindowEvent()

class MouseWheelEvent(val xDelta:Float,
                      val yDelta:Float) : WindowEvent()

class MouseDragStart(val pos:Vector2f, val button:Int) : WindowEvent()

class MouseDrag(val delta: Vector2f, val button:Int) : WindowEvent()

class MouseDragEnd(val delta:Vector2f, val button:Int) : WindowEvent()
