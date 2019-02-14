package vulkan.d2

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.vulkan.VK10.*
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.bindVertexBuffer
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.draw
import vulkan.api.pipeline.GraphicsPipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.common.*
import vulkan.misc.RGBA
import vulkan.misc.YELLOW
import vulkan.misc.toVector4f

class Circles {
    private lateinit var context: RenderContext

    private val descriptors     = Descriptors()
    private val pipeline        = GraphicsPipeline()
    private val ubo             = UBO(Matrix4f())
    private val points          = Points()

    private var uboStale        = true
    private var pointsStale     = true
    private var fillColour:RGBA = YELLOW
    private var edgeColour:RGBA = RGBA(0f,0f,0f,0f) //
    private var edgeThickness   = 0f                // no edge
    private var maxCircles:Int  = 0

    private var stagingUniform  : BufferAlloc? = null
    private var uniformBuffer   : BufferAlloc? = null
    private var stagingVertices : BufferAlloc? = null
    private var verticesBuffer  : BufferAlloc? = null

    fun init(context: RenderContext, maxCircles:Int) : Circles {
        this.context    = context
        this.maxCircles = maxCircles

        assert(maxCircles > 0)

        stagingUniform = context.buffers.get(VulkanBuffers.STAGING_UPLOAD)
                                        .allocate(ubo.size())
        uniformBuffer = context.buffers.get(VulkanBuffers.UNIFORM)
                                       .allocate(ubo.size())

        val verticesSize = points.elementInstance().size() * maxCircles

        stagingVertices = context.buffers.get(VulkanBuffers.STAGING_UPLOAD)
                                         .allocate(verticesSize)
        verticesBuffer = context.buffers.get(VulkanBuffers.VERTEX)
                                        .allocate(verticesSize)

        /**
         * Bindings:
         *    0     uniform buffer
         */
        descriptors
            .init(context)
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_GEOMETRY_BIT)
            .numSets(1)
            .build()

        descriptors
            .layout(0)
            .createSet()
            .add(uniformBuffer!!)
            .write()

        pipeline.init(context)
            .withVertexInputState(points.elementInstance(), VK_PRIMITIVE_TOPOLOGY_POINT_LIST)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "Circles/Circles.vert")
            .withShader(VK_SHADER_STAGE_GEOMETRY_BIT, "Circles/Circles.geom")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Circles/Circles.frag")
            .withStandardColorBlend()
            .build()

        return this
    }
    fun destroy() {
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera: Camera2D) : Circles {
        camera.VP(ubo.viewProj)
        uboStale = true
        return this
    }
    fun fillColour(c:RGBA) : Circles {
        this.fillColour = c
        return this
    }
    fun edgeColour(c:RGBA) : Circles  {
        this.edgeColour = c
        return this
    }
    fun edgeThickness(t:Float) : Circles {
        this.edgeThickness = t
        return this
    }
    fun add(centre:Vector2f, radius:Float) : Circles {

        points.add(Vertex(
            posRadiusThickness  = Vector4f(centre.x, centre.y, radius, edgeThickness),
            edgeColour          = edgeColour.toVector4f(),
            fillColour          = fillColour.toVector4f()
        ))

        pointsStale = true
        return this
    }
    fun removeAt(index:Int) : Circles {
        points.removeAt(index)
        pointsStale = true
        return this
    }
    fun clear() : Circles {
        points.clear()
        pointsStale = true
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(points.numPoints()==0) return

        if(uboStale) {
            updateUniform(frame, res)
        }
        if(pointsStale) {
            updatePoints(frame, res)
        }
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(points.numPoints()==0) return

        res.cmd.run {
            bindPipeline(pipeline)
            bindDescriptorSets(
                pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipelineLayout    = pipeline.layout,
                firstSet          = 0,
                descriptorSets    = arrayOf(descriptors.layout(0).set(0)),
                dynamicOffsets    = intArrayOf()
            )
            bindVertexBuffer(
                binding = 0,
                buffer  = verticesBuffer!!.rangeOf(0, points.size())
            )
            draw(
                vertexCount   = points.numPoints(),
                instanceCount = 1,
                firstVertex   = 0,
                firstInstance = 0
            )
        }
    }
    //==========================================================================================
    private fun updatePoints(frame: FrameInfo, res: PerFrameResource) {
        points.transfer(res.cmd, stagingVertices!!.rangeOf(0, points.size()),
                        verticesBuffer!!.rangeOf(0, points.size()))
        pointsStale = false
    }
    private fun updateUniform(frame: FrameInfo, res: PerFrameResource) {
        ubo.transfer(res.cmd, stagingUniform, uniformBuffer!!)
        uboStale = false
    }
    //=========================================================================================
    /**
     * if alpha channel == 0 then there is no fill
     * if edgeThickness == 0 then there is no edge
     */
    private class Vertex(val posRadiusThickness: Vector4f,
                         val fillColour: Vector4f,
                         val edgeColour: Vector4f) : AbsTransferable()

    private class Points : AbsTransferableArray() {
        private val array = ArrayList<Vertex>()
        override fun getArray(): Array<out AbsTransferable> {
            return array.toTypedArray()
        }
        override fun elementInstance(): AbsTransferable {
            return Vertex(Vector4f(), Vector4f(), Vector4f())
        }
        fun numPoints() = array.size
        fun add(p:Vertex) {
            array.add(p)
        }
        fun removeAt(i:Int) {
            array.removeAt(i)
        }
        fun clear() {
            array.clear()
        }
    }
    private class UBO(val viewProj: Matrix4f) : AbsTransferable()
}

