package vulkan.d3

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2i
import org.joml.Vector3f
import vulkan.maths.*

/**
 * https://github.com/JOML-CI/JOML
 */
class Camera3D(val windowSize: Vector2i = Vector2i()) {
    private var recalculateView     = true
    private var recalculateProj     = true
    private var recalculateViewProj = true
    private var recalculateInvView  = true
    private var recalculateInvVP    = true
    private var focalLength         = 0f
    private val view                = Matrix4f()
    private val proj                = Matrix4f()
    private val viewProj            = Matrix4f()
    private val invView             = Matrix4f()
    private val invViewProj         = Matrix4f()

    val position = Vector3f(0f, 0f, 0f)
    val forward  = Vector3f(0f, 0f, -1f)
    val up       = Vector3f(0f, 1f, 0f)

    val right:Vector3f      get() = forward.copy().cross(up)
    val focalPoint:Vector3f get() = position+forward*focalLength

    var fov  = Math.toRadians(60.0).toFloat()   ; private set
    var near = 0.1f                             ; private set
    var far  = 1000f                            ; private set

    /**
     * Set view assuming the up vector is 0,1,0
     */
    fun init(position:Vector3f, focalPoint:Vector3f) {
        this.position.set(position)
        this.forward.set(focalPoint-position).normalize()
        this.focalLength = (focalPoint-position).length()

        if(forward.isParallelTo(Vector3f(0f,1f,0f))) {
            // Up is anywhere around the y axis
            this.up.set(0f, 0f, -1f)
        } else {
            this.up.set(forward.rotatedTo(Vector3f(0f,1f,0f), Math.toRadians(90.0)))
        }
    }
    fun setOrientation(forward:Vector3f, up:Vector3f) {
        this.forward.set(forward).normalize()
        this.up.set(up).normalize()
        recalculateView = true
    }
    fun fovNearFar(fov:Float, near:Float, far:Float) {
        this.fov  = fov
        this.near = near
        this.far  = far
        recalculateProj = true
    }
    fun resizeWindow(newWindowSize:Vector2i) {
        windowSize.set(newWindowSize)
        recalculateProj = true
    }
    fun lookAt(point:Vector3f) {
        val q = Quaternionf().rotationTo(forward, point)

        forward.rotate(q)
        //val r = right

        up.rotate(q)
        recalculateView = true
    }
    fun moveForward(f:Float) {
        val dist  = forward * f
        position += dist
        recalculateView = true
    }
    fun movePositionRelative(newpos:Vector3f) {
        position += newpos
        recalculateView = true
    }
    fun movePositionAbsolute(newpos:Vector3f) {
        position.set(newpos)
        recalculateView = true
    }
    fun strafeRight(f:Float) {
        val right   = forward.right(up)
        val dist    = right * f
        position   += dist
        recalculateView = true
    }
    /**
     * move focal point left/right (around y plane)
     */
    fun yaw(f:Float) {
        val right   = forward.right(up)
        val dist    = right * f
        forward += dist
        forward.normalize()
        recalculateView = true
    }
    fun yawByDegrees(deg:Double) {
        forward.rotateY(Math.toRadians(deg).toFloat())
        recalculateView = true
    }
    /**
     * move focal point up/down (around x plane)
     */
    fun pitch(f:Float) {
        val right   = forward.right(up)
        val dist    = up * f
        forward += dist
        forward.normalize()
        up.set(right.right(forward))
        recalculateView = true
    }
    fun pitchByDegrees(deg:Double) {
        up.rotateX(Math.toRadians(deg).toFloat())
        recalculateView = true
    }
    /**
     * tip (around z plane)
     */
    fun roll(f:Float) {
        val dist = right * f
        up += dist
        up.normalize()
        recalculateView = true
    }
    fun rollByDegrees(deg:Double) {
        up.rotateZ(Math.toRadians(deg).toFloat())
        recalculateView = true
    }
    fun P(dest:Matrix4f?=null) {
        if(recalculateProj) {
            assert(windowSize.x > 0 && windowSize.y > 0) { "windowSize has not been set" }

            /**
             * Near and far planes reversed to allow for more accurate depth buffer
             * using near plane of 1.0 and a far plane of 0.0
             */
            proj.setPerspective(fov, windowSize.x.toFloat() / windowSize.y, far, near, true)

            recalculateProj     = false
            recalculateViewProj = true
        }
        dest?.set(proj)
    }
    fun V(dest:Matrix4f?=null) {
        if(recalculateView) {

            view.setLookAt(position, focalPoint, up)

            recalculateView     = false
            recalculateViewProj = true
            recalculateInvView  = true
        }
        dest?.set(view)
    }
    fun VP(dest:Matrix4f?=null) {
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
    fun invV(dest:Matrix4f) {
        V()
        if(recalculateInvView) {
            view.invert(invView)
            recalculateInvView = false
        }
        dest.set(invView)
    }
    fun invVP(dest:Matrix4f) {
        VP()
        if(recalculateInvVP) {
            viewProj.invert(invViewProj)
            recalculateInvVP = false
        }
        dest.set(invViewProj)
    }
//    fun screenToWorld(screenX:Float, screenY:Float, depthZ:Float) : Vector3f {
//        val invScreenY = (windowSize.y - screenY)
//        return Vector3.unProject(
//            Vector3(screenX, invScreenY, depthZ),
//            invVP(),
//            Vector4f(Point(0,0), _windowSize)
//        );
//    }
//    fun worldToScreen(Vector3 v, bool topLeftOrigin=true) : Vector3f {
//        val p = Vector3.project(v, V, P, Rect(Point(0,0), _windowSize))
//        if(topLeftOrigin) {
//            return Vector3(p.x, (windowSize.y)-p.y, p.z)
//        }
//        return p
//    }
    override fun toString(): String {
        return "[Camera3D pos:${position.string()} up:${up.string()}, forward:${forward.string()}"
    }
}
