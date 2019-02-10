package vulkan.common

import vulkan.app.KeyMods

class Mouse(var x:Float,
            var y:Float,
            val buttons:Array<KeyMods?>)
{
    fun isPressed(button:Int)     = buttons[button] != null
    fun mods(button:Int):KeyMods? = buttons[button]
}
