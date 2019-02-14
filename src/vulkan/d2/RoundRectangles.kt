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
import vulkan.maths.string
import vulkan.misc.RGBA
import vulkan.misc.WHITE
import vulkan.misc.orThrow
import vulkan.misc.set

class RoundRectangles {
    private lateinit var context:RenderContext
    private lateinit var buffers: BufferAllocs

    private var maxRects = 0

    private val vertices        = Vertices()
    private val ubo             = UBO()
    private val pipeline        = GraphicsPipeline()
    private val descriptors     = Descriptors()
    private var colour:RGBA     = WHITE
    private var uboChanged      = true
    private var verticesChanged = true

    fun init(context: RenderContext, maxRects:Int) : RoundRectangles {
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
    fun camera(camera : Camera2D) : RoundRectangles {
        camera.VP(ubo.viewProj)
        uboChanged = true
        return this
    }
    fun setColour(c: RGBA) : RoundRectangles {
        this.colour = c
        return this
    }
    fun addRectangle(pos:Vector2f, size:Vector2f, cornerRadius:Vector4f) : RoundRectangles {
        return addRectangle(pos, size, colour, colour, colour, colour, cornerRadius)
    }
    fun addRectangle(pos:Vector2f, size:Vector2f,
                     c1:RGBA, c2:RGBA, c3:RGBA, c4:RGBA,
                     cornerRadius:Vector4f) : RoundRectangles
    {
        vertices.addRectangle()
        return updateRectangle(vertices.numRectangles-1, pos, size, c1,c2,c3,c4, cornerRadius)
    }
    fun updateRectangle(index:Int, pos:Vector2f, size:Vector2f,
                        c1:RGBA, c2:RGBA, c3:RGBA, c4:RGBA,
                        cornerRadius:Vector4f) : RoundRectangles
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
    //=====================================================================================
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
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "RoundRectangles/RoundRectangles.vert")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "RoundRectangles/RoundRectangles.frag")
            .withStandardColorBlend()
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
    private class Vertices : AbsTransferableArray() {
        private class Vertex(val pos      : Vector2f,
                             val rectPos  : Vector2f,
                             val rectSize : Vector2f,
                             val colour   : Vector4f,
                             var radius   : Float) : AbsTransferable()

        private val array = ArrayList<Vertex>()

        override fun getArray() = array.toTypedArray()
        override fun elementInstance() =
            Vertex(Vector2f(), Vector2f(), Vector2f(), Vector4f(), 0f)

        val numVertices get()   = array.size
        val numRectangles get() = array.size / 6
        val vertexSize          = 11*4

        fun removeAll() {
            array.clear()
        }
        fun addRectangle() {
            (0 until 6).forEach {
                array.add(Vertex(Vector2f(), Vector2f(), Vector2f(), Vector4f(), 0f))
            }
        }
        fun update(index:Int,
                   pos: Vector2f,
                   size: Vector2f,
                   c1: RGBA, c2: RGBA, c3: RGBA, c4: RGBA,
                   cornerRadius:Vector4f)
        {
            assert(index<numRectangles)

            println("cornerRadius = ${cornerRadius.string()}")

            val i = index*6

            //  1--2    1,2,4  2,3,4
            //  | /|
            //  |/ |
            //  4--3
            for(j in 0 until 6) {
                array[i+j].let { it.rectPos.set(pos); it.rectSize.set(size) }
            }

            //1
            array[i].let { it.pos.set(pos); it.colour.set(c1); it.radius = cornerRadius.x }
            //2
            array[i+1].let { it.pos.set(pos).add(size.x, 0f); it.colour.set(c2); it.radius = cornerRadius.y }
            //4
            array[i+2].let { it.pos.set(pos).add(0f, size.y); it.colour.set(c4); it.radius = cornerRadius.w }

            //2
            array[i+3].let { it.pos.set(pos).add(size.x, 0f); it.colour.set(c2); it.radius = cornerRadius.y }
            //3
            array[i+4].let { it.pos.set(pos).add(size); it.colour.set(c3); it.radius = cornerRadius.z }
            //4
            array[i+5].let { it.pos.set(pos).add(0f, size.y); it.colour.set(c4); it.radius = cornerRadius.w }
        }
    }
    private class UBO(val viewProj: Matrix4f = Matrix4f()) : AbsTransferable()
}
