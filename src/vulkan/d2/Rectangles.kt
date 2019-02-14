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
import vulkan.misc.orThrow
import vulkan.misc.set

class Rectangles {
    private lateinit var context:RenderContext
    private lateinit var buffers:BufferAllocs

    private val vertices        = Vertices()
    private val ubo             = UBO()
    private val pipeline        = GraphicsPipeline()
    private val descriptors     = Descriptors()

    private var colour:RGBA     = WHITE
    private var maxRects        = 0
    private var verticesChanged = true
    private var uboChanged      = true

    fun init(context:RenderContext, maxRects:Int) : Rectangles{
        assert(maxRects>0)
        this.context    = context
        this.maxRects   = maxRects

        initialise()
        return this
    }
    fun destroy() {
        buffers.free()
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera : Camera2D) : Rectangles {
        camera.VP(ubo.viewProj)
        uboChanged = true
        return this
    }
    fun setColour(c: RGBA) : Rectangles {
        this.colour = c
        return this
    }
    /**
     *  Vertices are assumed to be clockwise eg.
     *  1-2
     *  | |
     *  4-3
     */
    fun addRectangle(p1:Vector2f, p2:Vector2f, p3:Vector2f, p4:Vector2f) : Rectangles {
        vertices.add()
        vertices.update(vertices.numRectangles-1, p1, p2, p3, p4, colour, colour, colour, colour)
        verticesChanged = true
        return this
    }
    fun addRectangle(p1:Vector2f, p2:Vector2f, p3:Vector2f, p4:Vector2f,
                     c1:RGBA,     c2:RGBA,     c3:RGBA,     c4:RGBA) : Rectangles
    {
        vertices.add()
        vertices.update(vertices.numRectangles-1, p1, p2, p3, p4, c1, c2, c3, c4)
        verticesChanged = true
        return this
    }
    fun updateRectangle(index:Int,
                        p1:Vector2f, p2:Vector2f, p3:Vector2f, p4:Vector2f,
                        c1:RGBA,     c2:RGBA,     c3:RGBA,     c4:RGBA) : Rectangles
    {
        vertices.update(index, p1, p2, p3, p4, c1, c2, c3, c4)
        verticesChanged = true
        return this
    }
    fun clear() : Rectangles {
        verticesChanged = true
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(vertices.numRectangles==0) return

        if(verticesChanged) {
            updateVertices(res)
        }
        if(uboChanged) {
            updateUBO(res)
        }
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(vertices.numRectangles==0) return

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
                buffer  = buffers.vertexBuffer.rangeOf(0, vertices.size())
            )
            draw(
                vertexCount   = vertices.numVertices,
                instanceCount = 1,
                firstVertex   = 0,
                firstInstance = 0
            )
        }
    }
    //============================================================================================
    private fun initialise() {

        buffers = BufferAllocs(context.buffers)

        /**
         * Bindings:
         *    0     uniform buffer
         */
        descriptors
            .init(context)
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_VERTEX_BIT)
            .numSets(1)
            .build()

        descriptors
            .layout(0)
                .createSet()
                .add(buffers.uniformBuffer)
            .write()

        pipeline.init(context)
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "Rectangles/Rectangles.vert")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Rectangles/Rectangles.frag")
            .build()
    }
    private fun updateVertices(res:PerFrameResource) {
        vertices.transfer(
            res.cmd,
            buffers.stagingVertices.rangeOf(0, vertices.size()),
            buffers.vertexBuffer.rangeOf(0,vertices.size()))
        verticesChanged = false

        log.info("Updated ${vertices.numRectangles} rectangles")
    }
    private fun updateUBO(res : PerFrameResource) {
        ubo.transfer(res.cmd, buffers.stagingUniform, buffers.uniformBuffer)
        uboChanged = false
    }

    //=======================================================================================================
    private inner class BufferAllocs(b: VulkanBuffers) {
        var stagingVertices: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(vertices.vertexSize * 6 * maxRects).orThrow()
        val stagingUniform: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(ubo.size()).orThrow()

        val vertexBuffer: BufferAlloc  = b.get(VulkanBuffers.VERTEX).allocate(vertices.vertexSize * 6 * maxRects).orThrow()
        val uniformBuffer: BufferAlloc = b.get(VulkanBuffers.UNIFORM).allocate(ubo.size()).orThrow()

        fun free() {
            stagingVertices.free()
            stagingUniform.free()
            vertexBuffer.free()
            uniformBuffer.free()
        }
    }
    private class Vertices : AbsTransferableArray() {
        private class Vertex(val pos : Vector2f, val colour : Vector4f) : AbsTransferable()

        private val array = ArrayList<Vertex>()

        override fun getArray() = array.toTypedArray()
        override fun elementInstance() = Vertex(Vector2f(), Vector4f())

        val numVertices get()   = array.size
        val numRectangles get() = array.size / 6
        val vertexSize          = 6*4

        fun removeAll() {
            array.clear()
        }
        fun add() {
            (0 until 6).forEach { array.add(Vertex(Vector2f(), Vector4f())) }
        }
        fun update(index:Int,
                   p1:Vector2f, p2:Vector2f, p3:Vector2f, p4:Vector2f,
                   c1:RGBA,     c2:RGBA,     c3:RGBA,     c4:RGBA)
        {
            assert(index<numRectangles)
            val i = index*6

            // 1-2  (124), (234)
            // |/|
            // 4-3
            array[i+0].pos.set(p1); array[i+0].colour.set(c1)
            array[i+1].pos.set(p2); array[i+1].colour.set(c2)
            array[i+2].pos.set(p4); array[i+2].colour.set(c4)

            array[i+3].pos.set(p2); array[i+3].colour.set(c2)
            array[i+4].pos.set(p3); array[i+4].colour.set(c3)
            array[i+5].pos.set(p4); array[i+5].colour.set(c4)
        }
    }
    private class UBO(val viewProj: Matrix4f = Matrix4f()) : AbsTransferable()
}
