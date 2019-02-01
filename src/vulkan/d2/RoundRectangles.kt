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



class RoundRectangles {
    fun init(context: RenderContext, buffers: VulkanBuffers, maxRects:Int) : RoundRectangles {
        assert(maxRects>0)
        this.context    = context
        this.maxRects   = maxRects
        initialise(buffers)
        return this
    }
    fun destroy() {
        buffers.free()
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera : Camera2D) : RoundRectangles {
        camera.VP(ubo.viewProj)
        uboChanged = true
        return this
    }
    fun setColour(c: RGBA) : RoundRectangles {
        this.colour = c
        return this
    }
    fun addRectangle(pos:Vector2f, size:Vector2f, cornerRadius:Float) : RoundRectangles {
        return addRectangle(pos, size, colour, colour, colour, colour, cornerRadius)
    }
    fun addRectangle(pos:Vector2f, size:Vector2f,
                     c1:RGBA, c2:RGBA, c3:RGBA, c4:RGBA,
                     cornerRadius:Float) : RoundRectangles
    {
        vertices.add()
        return updateRectangle(vertices.numRectangles-1, pos, size, c1,c2,c3,c4, cornerRadius)
    }
    fun updateRectangle(index:Int, pos:Vector2f, size:Vector2f,
                        c1:RGBA, c2:RGBA, c3:RGBA, c4:RGBA,
                        cornerRadius:Float) : RoundRectangles
    {
        vertices.update(index, pos, size, c1,c2,c3,c4, cornerRadius)
        verticesChanged = true
        return this
    }
    fun clear() : RoundRectangles {
        vertices.removeAll()
        verticesChanged = true
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res:PerFrameResource) {
        if(vertices.numRectangles==0) return

        if(verticesChanged) {
            updateVertices(res)
        }
        if(uboChanged) {
            updateUBO(res)
        }
    }
    fun insideRenderPass(frame: FrameInfo, res:PerFrameResource) {
        if(vertices.numRectangles==0) return

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
    //=======================================================================================================
    //     _____        _____        _____      __      __                   _______      ______
    //    |  __ \      |  __ \      |_   _|     \ \    / /        /\        |__   __|    |  ____|
    //    | |__) |     | |__) |       | |        \ \  / /        /  \          | |       | |__
    //    |  ___/      |  _  /        | |         \ \/ /        / /\ \         | |       |  __|
    //    | |          | | \ \       _| |_         \  /        / ____ \        | |       | |____
    //    |_|          |_|  \_\     |_____|         \/        /_/    \_\       |_|       |______|
    //
    //=======================================================================================================
    private inner class BufferAllocs(b: VulkanBuffers) {
        var stagingVertices: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(vertices.vertexSize * maxRects).orThrow()
        val stagingUniform: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(ubo.size()).orThrow()

        val vertexBuffer: BufferAlloc = b.get(VulkanBuffers.VERTEX).allocate(vertices.vertexSize * maxRects).orThrow()
        val uniformBuffer: BufferAlloc = b.get(VulkanBuffers.UNIFORM).allocate(ubo.size()).orThrow()

        fun free() {
            stagingVertices.free()
            stagingUniform.free()
            vertexBuffer.free()
            uniformBuffer.free()
        }
    }
    /** Each vertex is a single POINT */
    private class Vertices : AbsTransferableArray() {
        private class Vertex(val pos    : Vector2f,
                             val size   : Vector2f,
                             val c1     : Vector4f,
                             val c2     : Vector4f,
                             val c3     : Vector4f,
                             val c4     : Vector4f,
                             var radius : Float) : AbsTransferable()

        private val array = ArrayList<Vertex>()

        override fun getArray() = array.toTypedArray()
        override fun elementInstance() =
            Vertex(Vector2f(), Vector2f(), Vector4f(), Vector4f(), Vector4f(), Vector4f(), 0f)

        val numVertices get()   = array.size
        val numRectangles get() = array.size
        val vertexSize          = 21*4

        fun removeAll() {
            array.clear()
        }
        fun add() {
            array.add(Vertex(Vector2f(), Vector2f(), Vector4f(), Vector4f(), Vector4f(), Vector4f(), 0f))
        }
        fun update(index:Int,
                   pos: Vector2f,
                   size: Vector2f,
                   c1: RGBA, c2: RGBA, c3: RGBA, c4: RGBA,
                   cornerRadius:Float)
        {
            assert(index<numRectangles)

            array[index].let {
                it.pos.set(pos)
                it.size.set(size)
                it.c1.set(c1)
                it.c2.set(c2)
                it.c3.set(c3)
                it.c4.set(c4)
                it.radius = cornerRadius
            }
        }
    }
    private class UBO(val viewProj: Matrix4f = Matrix4f()) : AbsTransferable()
    //=====================================================================================
    private lateinit var context:RenderContext
    private lateinit var buffers: BufferAllocs
    private lateinit var descriptors: Descriptors
    private lateinit var pipeline: GraphicsPipeline
    private var maxRects = 0

    private val vertices        = Vertices()
    private val ubo             = UBO()
    private var colour:RGBA     = WHITE
    private var uboChanged      = true
    private var verticesChanged = true


    private fun initialise(vBuffers : VulkanBuffers) {

        buffers = BufferAllocs(vBuffers)

        /**
         * Bindings:
         *    0     uniform buffer
         */
        descriptors = Descriptors()
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_GEOMETRY_BIT)
            .numSets(1)
            .build(context.device)

        descriptors
            .layout(0)
            .createSet()
            .add(buffers.uniformBuffer)
            .write()

        pipeline = GraphicsPipeline(context)
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_POINT_LIST)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "RoundRectangles.vert")
            .withShader(VK_SHADER_STAGE_GEOMETRY_BIT, "RoundRectangles.geom")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "RoundRectangles.frag")
            .withStandardColorBlend()
            .build()
    }
    private fun updateVertices(res:PerFrameResource) {
        vertices.transfer(
            res.adhocCB,
            buffers.stagingVertices.rangeOf(0, vertices.size()),
            buffers.vertexBuffer.rangeOf(0,vertices.size()))

        verticesChanged = false
        log.info("Updated ${vertices.numRectangles} rectangles")
    }
    private fun updateUBO(res : PerFrameResource) {
        ubo.transfer(res.adhocCB, buffers.stagingUniform, buffers.uniformBuffer)
        uboChanged = false
    }
}
