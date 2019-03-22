package vulkan.d3

import org.joml.Matrix4f
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
    private var fov                 = Math.toRadians(60.0).toFloat()
    private var near                = 0.1f
    private var far                 = 1000f
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
            this.up.set(forward.rotatedTo(Vector3f(0f,-1f,0f), Math.toRadians(90.0)))
        }
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
    /**
     * tip (around z plane)
     */
    fun roll(f:Float) {
        val dist = right * f
        up += dist
        up.normalize()
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

/*


final class Camera3D {
    enum Mode { GL, VULKAN }
    Mode mode = Mode.GL;
	Vector3 _position;
	Vector3 _forward;
	Vector3 _up;
	float _focalLength;
	Angle!float _fov = 60.degrees;
	float _near = 0.1f, _far = 100f;
	Dimension _windowSize;
	Matrix4 view	 = Matrix4.identity;
	Matrix4 proj	 = Matrix4.identity;
	Matrix4 viewProj = Matrix4.identity;
	Matrix4 invViewProj = Matrix4.identity;
	bool recalculateView = true;
	bool recalculateProj = true;
	bool recalculateViewProj = true;
	bool recalculateInvViewProj = true;
private:

public:
	Vector3 position() { return _position; }
	Vector3 up() { return _up; }
	Vector3 forward() { return _forward; }
	Vector3 right() { return _forward.right(_up); }
	Vector3 focalPoint() { return _position+_forward*_focalLength; }
	Dimension windowSize() { return _windowSize; }
    float aspectRatio() { return _windowSize.width/_windowSize.height; }
    Angle!float fov() { return _fov; }
    float near() { return _near; }
    float far() { return _far; }

	override string toString() {
		return "[Camera pos:%s forward:%s up:%s"
			.format(_position.toString(2), _forward.toString(2), _up.toString(2));
	}

	this(Dimension windowSize, Vector3 pos, Vector3 up, Vector3 focalPoint) {
		this._windowSize  = windowSize;
		this._position	  = pos;
		this._up		  = up.normalised();
		this._forward     = (focalPoint-pos).normalised();
		this._focalLength = (focalPoint-pos).magnitude();
	}
	/**
	 * Assumption 1:
	 *  Resize will be called to set the windowSize before use.
	 * Assumption 2:
	 *  Up vector is _forward rotated by the X-axis by
	 *  90 degrees. Use pitch to change the _up vector later.
	 */
	this(Vector3 pos, Vector3 focalPoint) {
        this._windowSize  = Dimension(0,0);
        this._position	  = pos;
        this._forward     = (focalPoint-pos).normalised();
        this._focalLength = (focalPoint-pos).magnitude();
        // Calculate up
        if(_forward.isParallelTo(vec3(0,1,0))) {
            // Up is anywhere around the y axis
            this._up = vec3(0,0,-1);
        } else {
            this._up = _forward.rotatedTo(vec3(0,1,0), 90.degrees);
        }
    }
	ref Matrix4 P() {
		if(recalculateProj) {
		    if(mode==Mode.VULKAN)
		        proj = vkPerspective(
                        _fov,
                        _windowSize.width / _windowSize.height,
                        _near,
                        _far);
            else
                proj = Matrix4.perspective(
                        _fov,
                        _windowSize.width / _windowSize.height,
                        _near,
                        _far);
			recalculateProj = false;
			recalculateViewProj = true;
			recalculateInvViewProj = true;
		}
		return proj;
	}
	ref Matrix4 V() {
		if(recalculateView) {
			view = Matrix4.lookAt(
				_position,		// camera _position in World Space
				focalPoint(),	// position we are looking at
				_up				// head is up
			);
			recalculateView = false;
			recalculateViewProj = true;
			recalculateInvViewProj = true;
		}
		return view;
	}
	ref Matrix4 VP() {
		if(recalculateView || recalculateProj || recalculateViewProj) {
			V();
			P();
			viewProj = proj * view;
			recalculateViewProj = false;
			recalculateInvViewProj = true;
		}
		return viewProj;
	}
	ref Matrix4 inverseVP() {
		// ensure we have a valid viewProj
		VP();
		if(recalculateInvViewProj) {
			invViewProj = VP().inversed();
			recalculateInvViewProj = false;
		}
		return invViewProj;
	}
	/// origin is top-left
    Vector3 screenToWorld(float screenX, float screenY, float depthZ) {
        float invScreenY = (_windowSize.height - screenY);
        return Vector3.unProject(
            Vector3(screenX, invScreenY, depthZ),
            inverseVP(),
            Rect(Point(0,0), _windowSize)
        );
    }
    Vector3 worldToScreen(Vector3 v, bool topLeftOrigin=true) {
        auto p = Vector3.project(v, V, P, Rect(Point(0,0), _windowSize));
        if(topLeftOrigin) {
            return Vector3(p.x, cast(float)(_windowSize.height)-p.y, p.z);
        }
        return p;
    }
}




 */
