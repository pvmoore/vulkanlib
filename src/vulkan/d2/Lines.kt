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
import vulkan.misc.WHITE
import vulkan.misc.toVector4f
import java.util.*

class Lines {
    private lateinit var context:RenderContext
    private var maxLines = 0

    private val descriptors     = Descriptors()
    private val pipeline        = GraphicsPipeline()
    private val ubo             = UBO()
    private val points          = Points()

    private var uboStale        = true
    private var pointsStale     = true
    private var colour: RGBA    = WHITE
    private var thickness       = 1.5f

    private var stagingUniform  : BufferAlloc? = null
    private var uniformBuffer   : BufferAlloc? = null
    private var stagingVertices : BufferAlloc? = null
    private var verticesBuffer  : BufferAlloc? = null

    fun init(context:RenderContext, maxLines:Int) : Lines {
        this.maxLines = maxLines
        this.context  = context

        assert(maxLines > 0)

        stagingUniform = context.buffers.get(VulkanBuffers.STAGING_UPLOAD)
            .allocate(ubo.size())
        uniformBuffer = context.buffers.get(VulkanBuffers.UNIFORM)
            .allocate(ubo.size())

        val verticesSize = points.elementInstance().size() * maxLines

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
            .withShader(VK_SHADER_STAGE_VERTEX_BIT, "Lines.vert")
            .withShader(VK_SHADER_STAGE_GEOMETRY_BIT, "Lines.geom")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Lines.frag")
            .withStandardColorBlend()
            .build()

        return this
    }
    fun destroy() {
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera: Camera2D) : Lines {
        camera.VP(ubo.viewProj)
        uboStale = true
        return this
    }
    fun colour(c:RGBA) : Lines {
        this.colour = c
        return this
    }
    fun thickness(t:Float) : Lines {
        this.thickness = t
        return this
    }
    fun add(from:Vector2f, to:Vector2f) : Lines {
        return add(from, to, colour, colour)
    }
    fun add(from:Vector2f, to:Vector2f, fromCol:RGBA, toCol:RGBA) : Lines {

        points.add(Vertex(
            Vector4f(from.x, from.y, to.x, to.y),
            fromCol.toVector4f(),
            toCol.toVector4f(),
            thickness))

        pointsStale = true
        return this
    }
    fun removeAt(index:Int) : Lines {
        points.removeAt(index)
        pointsStale = true
        return this
    }
    fun clear() : Lines {
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

        res.adhocCB.run {
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
    private fun updateUniform(frame: FrameInfo, res: PerFrameResource) {
        ubo.transfer(res.adhocCB, stagingUniform, uniformBuffer!!)
        uboStale = false
    }
    private fun updatePoints(frame: FrameInfo, res: PerFrameResource) {
        points.transfer(res.adhocCB, stagingVertices!!.rangeOf(0, points.size()),
                                     verticesBuffer!!.rangeOf(0, points.size()))
        pointsStale = false
    }
    //==========================================================================================
    private class Vertex(val fromTo: Vector4f,
                         val fromCol:Vector4f,
                         val toCol:Vector4f,
                         val thickness:Float) : AbsTransferable()

    private class Points :AbsTransferableArray() {
        private val array = ArrayList<Vertex>()
        override fun getArray(): Array<out AbsTransferable> {
            return array.toTypedArray()
        }
        override fun elementInstance(): AbsTransferable {
            return Vertex(Vector4f(), Vector4f(), Vector4f(), 0f)
        }
        fun numPoints() = array.size
        fun add(p: Vertex) {
            array.add(p)
        }
        fun removeAt(i:Int) {
            array.removeAt(i)
        }
        fun clear() {
            array.clear()
        }
    }
    private class UBO(val viewProj:Matrix4f = Matrix4f()) : AbsTransferable()
}
