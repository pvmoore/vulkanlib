package vulkan.app


import org.joml.Vector2f
import org.lwjgl.glfw.GLFW.*

enum class WindowEventKind {
    KEY,
    MOUSE_BUTTON,
    MOUSE_MOVE,
    MOUSE_WHEEL,
    MOUSE_DRAG,
    MOUSE_DRAG_START,
    MOUSE_DRAG_END
}

abstract class WindowEvent {
    abstract fun kind() : WindowEventKind
}

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
{
    override fun kind() = WindowEventKind.KEY
}

class MouseButtonEvent(val button:Int,
                       val pos:Vector2f,
                       val action:KeyAction,
                       val mods:KeyMods) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_BUTTON
}
class MouseMoveEvent(val x:Float,
                     val y:Float) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_MOVE
}
class MouseWheelEvent(val xDelta:Float,
                      val yDelta:Float) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_WHEEL
}
class MouseDragStart(val pos:Vector2f, val button:Int) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_DRAG_START
}
class MouseDrag(val delta: Vector2f, val button:Int) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_DRAG
}
class MouseDragEnd(val delta:Vector2f, val button:Int) : WindowEvent()
{
    override fun kind() = WindowEventKind.MOUSE_DRAG_END
}
