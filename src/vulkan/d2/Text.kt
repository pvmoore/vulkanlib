package vulkan.d2

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.vulkan.VK10.*
import vulkan.api.VkSampler
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.bindVertexBuffer
import vulkan.api.createSampler
import vulkan.api.descriptor.bindDescriptorSet
import vulkan.api.draw
import vulkan.api.pipeline.GraphicsPipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.api.pushConstants
import vulkan.common.*
import vulkan.font.Font
import vulkan.misc.*

class Text {

    fun init(context: RenderContext, font: Font, maxCharacters:Int, dropShadow:Boolean)
        : Text
    {
        assert(maxCharacters>0)
        this.maxCharacters = maxCharacters
        this.dropShadow    = dropShadow
        this.context       = context
        this.font          = font
        this.size          = font.size.toFloat()
        initialise(context.buffers)
        return this
    }
    fun destroy() {
        sampler.destroy()
        buffers.free()
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera : Camera2D) : Text {
        camera.VP(ubo.viewProj)
        uboChanged = true
        return this
    }
    fun setColour(c: RGBA) : Text {
        this.colour = c
        return this
    }
    fun setDropShadowColour(c:RGBA) : Text {
        ubo.dsColour.set(c)
        uboChanged = true
        return this
    }
    fun setDropShadowOffset(o:Vector2f) : Text {
        ubo.dsOffset.set(o)
        uboChanged = true
        return this
    }
    fun setSize(size:Float) : Text {
        this.size = size
        return this
    }
    fun appendText(text:String, x:Int = 0, y:Int = 0) : Text {
        textChunks.add(TextChunk(
            text,
            colour,
            size,
            x,
            y)
        )
        verticesChanged = true
        return this
    }
    fun updateText(chunk:Int, text:String, x:Int = -1, y:Int = -1) : Text {
        /** Ignore the update if nothing has changed */
        val prev = textChunks[chunk]
        if((x==-1 || x==prev.x) && (y==-1 || y==prev.y) && prev.text == text) return this

        prev.text = text
        if(x!=-1) prev.x = x
        if(y!=-1) prev.y = y

        verticesChanged = true
        return this
    }
    fun updateColour(chunk:Int, colour:RGBA) : Text {
        val prev = textChunks[chunk]
        if(prev.colour != colour) {
            prev.colour = colour
            verticesChanged = true
        }
        return this
    }
    fun updateSize(chunk:Int, size:Float) : Text {
        val prev = textChunks[chunk]
        if(prev.size!=size) {
            prev.size = size
            verticesChanged = true
        }
        return this
    }
    fun clear() : Text {
        textChunks.clear()
        verticesChanged = true
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res:PerFrameResource) {
        if(countCharacters()==0) return

        if(verticesChanged) {
            updateVertices(res)
        }
        if(uboChanged) {
            updateUBO(res)
        }
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(vertices.numCharacters==0) return

        res.adhocCB.run {
            bindPipeline(pipeline)
            bindDescriptorSet(
                pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS,
                descriptorSet     = descriptors.layout(0).set(0),
                pipelineLayout    = pipeline.layout
            )
            bindVertexBuffer(
                binding = 0,
                buffer  = buffers.vertexBuffer.rangeOf(0, vertices.size())
            )
            if(dropShadow) {
                pushConstants.set(0, 1f)
                pushConstants(
                    pipelineLayout = pipeline.layout,
                    stageFlags     = VK_SHADER_STAGE_FRAGMENT_BIT,
                    offset         = 0,
                    values         = pushConstants.floatBuffer
                )
                draw(vertices.numCharacters, 1, 0, 0) // numCharacters points
            }
            pushConstants.set(0, 0f)
            pushConstants(
                pipelineLayout = pipeline.layout,
                stageFlags     = VK_SHADER_STAGE_FRAGMENT_BIT,
                offset         = 0,
                values         = pushConstants.floatBuffer
            )
            draw(vertices.numCharacters, 1, 0, 0) // numCharacters points
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
        var stagingVertices: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(vertices.vertexSize * maxCharacters).orThrow()
        val stagingUniform: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(ubo.size()).orThrow()

        val vertexBuffer: BufferAlloc = b.get(VulkanBuffers.VERTEX).allocate(vertices.vertexSize * maxCharacters).orThrow()
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
        private class Vertex(
            val pos    : Vector4f,
            val uvs    : Vector4f,
            val colour : Vector4f,
            var size   : Float
        ):AbsTransferable()

        private val array = ArrayList<Vertex>()

        override fun getArray()        = array.toTypedArray()
        override fun elementInstance() = Vertex(Vector4f(), Vector4f(), Vector4f(), 0f)

        val numCharacters get() = array.size
        val vertexSize          = 13 * 4

        fun clear() {
            array.clear()
        }
        fun add(pos:Vector4f, uvs:Vector4f, colour:RGBA, size:Float) {
            array.add(Vertex(pos, uvs, colour.toVector4f(), size))
        }
    }
    private class UBO(val viewProj: Matrix4f = Matrix4f(),
                      val dsColour:Vector4f  = Vector4f(0f,0f,0f, 0.75f),
                      val dsOffset:Vector2f  = Vector2f(-0.0025f, 0.0025f),
                      val _pad:Vector2f      = Vector2f()) : AbsTransferable()

    private class TextChunk(
        var text:String,
        var colour:RGBA,
        var size:Float,
        var x:Int,
        var y:Int
    )
    //=====================================================================================
    private lateinit var context:RenderContext
    private lateinit var buffers: BufferAllocs
    private lateinit var descriptors: Descriptors
    private lateinit var pipeline: GraphicsPipeline
    private lateinit var sampler: VkSampler
    private lateinit var font:Font

    private var maxCharacters   = 0
    private var dropShadow      = true
    private var size            = 0f
    private val pushConstants   = PushConstants(1).set(0, 0f)
    private val vertices        = Vertices()
    private val ubo             = UBO()
    private val textChunks      = ArrayList<TextChunk>()
    private var colour: RGBA    = WHITE
    private var uboChanged      = true
    private var verticesChanged = true

    private fun initialise(vBuffers : VulkanBuffers) {

        buffers = BufferAllocs(vBuffers)

        sampler = context.device.createSampler { info-> }

        /**
         * Bindings:
         *    0     uniform buffer
         */
        descriptors = Descriptors()
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_GEOMETRY_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)
            .combinedImageSampler(VK_SHADER_STAGE_FRAGMENT_BIT)
            .numSets(1)
            .build(context.device)

        descriptors
            .layout(0)
            .createSet()
            .add(buffers.uniformBuffer)
            .add(font.texture.image.getView(),
                 VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                 sampler)
            .write()

        pipeline = GraphicsPipeline(context)
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_POINT_LIST)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShader(VK_SHADER_STAGE_VERTEX_BIT, "Text.vert")
            .withShader(VK_SHADER_STAGE_GEOMETRY_BIT, "Text.geom")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Text.frag")
            .withStandardColorBlend()
            .withPushConstants(firstIndex = 0, count = 1)
            .build()
    }
    private fun updateVertices(res:PerFrameResource) {

        assert(countCharacters() <= maxCharacters)

        generateVertices()

        vertices.transfer(
            res.adhocCB,
            buffers.stagingVertices.rangeOf(0, vertices.size()),
            buffers.vertexBuffer.rangeOf(0,vertices.size()))

        verticesChanged = false
    }
    private fun updateUBO(res : PerFrameResource) {
        ubo.transfer(res.adhocCB, buffers.stagingUniform, buffers.uniformBuffer)
        uboChanged = false
    }
    private fun countCharacters():Int {
        return textChunks.sumBy { it.text.length }
    }
    private fun generateVertices() {

        vertices.clear()

        textChunks.forEach { c ->
            var X  = c.x.toFloat()
            val Y  = c.y.toFloat()

            fun generateVertex(i:Int, ch:Int) {
                val g     = font.getChar(ch)
                val ratio = c.size/font.size

                val x = X + g.xoffset * ratio
                val y = Y + g.yoffset * ratio
                val w = g.width * ratio
                val h = g.height * ratio

                vertices.add(
                    pos     = Vector4f(x, y, w, h),
                    uvs     = Vector4f(g.u, g.v, g.u2, g.v2),
                    colour  = c.colour,
                    size    = c.size
                )

                val kerning = if(i<c.text.length-1) font.getKerning(ch, c.text[i+1].toInt()) else 0

                X += (g.xadvance + kerning) * ratio
            }

            c.text.toCharArray().forEachIndexed { i,ch->
                generateVertex(i, ch.toInt())
            }
        }

        /** Write vertices to the staging buffer */
        buffers.stagingVertices.rangeOf(0, vertices.size()).mapForWriting { bb->
            vertices.writeTo(bb)
        }
    }
}
