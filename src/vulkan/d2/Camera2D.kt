package vulkan.d2

/**
 * https://matthewwellings.com/blog/the-new-vulkan-coordinate-system/
 *
 * Vulkan device coordinates (NDC) are:
 *
 *   -z (out of screen)
 *     \   -y
 *      \   |
 *       \  |
 *        \ |
 *         \|
 * -x ------|------> +x
 *          |\
 *          | \
 *          |  \
 *          v   +z (into the screen)
 *         +y
 *
 */
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector4f
import vulkan.maths.*

class Camera2D(val windowSize:Vector2i = Vector2i()) {
    private var _zoomFactor         = 1f
    private var rotationDegrees     = Degrees(0.0)
    private var recalculateView     = true
    private var recalculateProj     = true
    private var recalculateViewProj = true
    private var recalculateInvView  = true
    private var recalculateInvVP    = true
    private val view        = Matrix4f()
    private val proj        = Matrix4f()
    private val viewProj    = Matrix4f()
    private val invView     = Matrix4f()
    private val invViewProj = Matrix4f()

    val position = Vector2f(windowSize.x/2f, windowSize.y/2f)
    val up       = Vector2f(0f, 1f)

    val zoomFactor get() = 1f / _zoomFactor

    fun moveAbs(x:Float, y:Float) {
        position.x = x
        position.y = y
        recalculateView = true
    }
    fun moveAbs(v:Vector2f) {
        position.x = v.x
        position.y = v.y
        recalculateView = true
    }
    fun moveRel(x:Float, y:Float) {
        moveAbs(position.x+x, position.y+y)
    }
    fun moveRel(v:Vector2f) {
        moveAbs(position.x+v.x, position.y+v.y)
    }
    fun zoomOut(z:Float, maxZoom:Float = 2f) {
        if(_zoomFactor >= maxZoom) return

        _zoomFactor += z
        if(_zoomFactor > maxZoom) {
            _zoomFactor = maxZoom
        }
        recalculateProj = true
    }

    fun zoomIn(z:Float, minZoom:Float = 0.01f) {
        if(_zoomFactor <= minZoom) return

        _zoomFactor -= z
        if(_zoomFactor < minZoom) {
            _zoomFactor = minZoom
        }
        recalculateProj = true
    }
    /** 0.5 = zoomed out (50%), 1 = (100%) no zoom, 2 = (200%) zoomed in */
    fun setZoom(z:Float) {
        _zoomFactor     = 1f/z
        recalculateProj = true
    }
    fun rotateTo(degrees: Degrees) {
        rotationDegrees = degrees
        val rotated = Vector4f(0f,1f,0f,0f).rotateZ(degrees.toRadians().toFloat())
        up.apply {
            x = rotated.x
            y = rotated.y
        }
        recalculateView = true
    }
    fun rotateBy(degrees: Degrees) {
        rotateTo(Degrees(degrees.value + rotationDegrees.value))
    }
    fun resizeWindow(newWindowSize:Vector2i) {
        windowSize.set(newWindowSize)
        moveAbs(windowSize.x/2f, windowSize.y/2f)
        recalculateProj = true
    }
    fun P(dest:Matrix4f? = null) {
        if(recalculateProj) {
            assert(windowSize.x>0 && windowSize.y>0) { "windowSize has not been set" }
            val width  = windowSize.x*_zoomFactor
            val height = windowSize.y*_zoomFactor

            proj.setOrtho(
                -width/2f,   width/2f,
                -height/2f,  height/2f,
                0f,          100f,
                true)

            recalculateProj     = false
            recalculateViewProj = true
        }
        dest?.set(proj)
    }
    fun V(dest:Matrix4f? = null) {
        if(recalculateView) {

            view.setLookAt(
                position.x, position.y, 1f,  // camera _position in World Space
                position.x, position.y, 0f,  // look at the _position
                up.x,       up.y,       0f)  // head is up

            recalculateView     = false
            recalculateViewProj = true
            recalculateInvView  = true
        }
        dest?.set(view)
    }
    fun VP(dest:Matrix4f? = null) {
        if(recalculateView || recalculateProj || recalculateViewProj) {
            V()
            P()
            viewProj.set(proj)
            viewProj.mul(view)
            //viewProj = proj * view;
            recalculateViewProj = false
            recalculateInvVP    = true
        }
        dest?.set(viewProj)
    }
    fun invV(dest:Matrix4f) : Matrix4f {
        V()
        if(recalculateInvView) {
            view.invert(invView)
            recalculateInvView = false
        }
        dest.set(invView)
        return dest
    }
    fun invVP(dest:Matrix4f) : Matrix4f {
        VP()
        if(recalculateInvVP) {
            viewProj.invert(invViewProj)
            recalculateInvVP = false
        }
        dest.set(invViewProj)
        return dest
    }
    /**
     * @param pos screen pos (0,0) is top left
     * @return world pos
     */
    fun screenToWorld(pos:Vector2f) : Vector2f {

        val screenSize = windowSize.toVector2f()

        val ndc = (((pos + 0.5f) * 2f) / screenSize) - 1f

        val inv = invVP(Matrix4f())

        val worldPos = Vector4f(ndc, 0f, 1f) * inv

        return worldPos.xy()
    }
    override fun toString(): String {
        return "[Camera2D pos:${position.string()} up:${up.string()}"
    }
}
