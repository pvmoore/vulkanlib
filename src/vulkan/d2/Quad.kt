package vulkan.d2

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.vulkan.VK10.*
import org.lwjgl.vulkan.VkDevice
import vulkan.api.VkRenderPass
import vulkan.api.VkSampler
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.bindIndexBuffer
import vulkan.api.buffer.bindVertexBuffer
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.drawIndexed
import vulkan.api.image.VkImageView
import vulkan.api.pipeline.GraphicsPipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.common.*
import vulkan.misc.RGBA
import vulkan.misc.orThrow
import vulkan.misc.set

class Quad {
    private class UBO(val model:Matrix4f = Matrix4f(),
                      val view:Matrix4f  = Matrix4f(),
                      val proj:Matrix4f  = Matrix4f()) : AbsTransferable()

    private class Vertices : AbsTransferableArray()
    {
        private class Vertex(val pos: Vector2f, val colour: Vector4f, val uv:Vector2f) : AbsTransferable()
        private val array = arrayOf(
            // pos                   colour        uv
            Vertex(Vector2f(0f, 0f), Vector4f(1f), Vector2f(0f, 0f)),
            Vertex(Vector2f(1f, 0f), Vector4f(1f), Vector2f(1f, 0f)),
            Vertex(Vector2f(1f, 1f), Vector4f(1f), Vector2f(1f, 1f)),
            Vertex(Vector2f(0f, 1f), Vector4f(1f), Vector2f(0f, 1f))
        )
        override fun getArray() = array

        fun setColour(c:RGBA) {
            array.forEach { it.colour.set(c) }
        }
        fun setUV(topLeft:Vector2f, bottomRight:Vector2f) {
            array[0].uv.set(topLeft)
            array[1].uv.set(bottomRight.x, topLeft.y)
            array[2].uv.set(bottomRight)
            array[3].uv.set(topLeft.x, bottomRight.y)
        }
    }
    private class Indices : AbsTransferableShortArray() {
        private val array = shortArrayOf(
            0,1,2,
            2,3,0
        )
        override fun getArray() = array
    }
    private inner class BufferAllocs(
        b: RenderBuffers,
        val stagingVertices: BufferAlloc = b.staging.allocate(vertices.size()).orThrow(),
        val stagingIndices: BufferAlloc = b.staging.allocate(indices.size()).orThrow(),
        val stagingUniform: BufferAlloc = b.staging.allocate(ubo.size()).orThrow(),

        val vertexBuffer: BufferAlloc  = b.vertex.allocate(vertices.size()).orThrow(),
        val indexBuffer: BufferAlloc   = b.index.allocate(indices.size()).orThrow(),
        val uniformBuffer: BufferAlloc = b.uniform.allocate(ubo.size()).orThrow())
    {
        fun free() {
            stagingVertices.free()
            stagingIndices.free()
            stagingUniform.free()
            vertexBuffer.free()
            indexBuffer.free()
            uniformBuffer.free()
        }
    }
    //=================================================================================================

    private var isInitialised    = false
    private var verticesUploaded = false
    private var uboUploaded      = false
    private val vertices         = Vertices()
    private val indices          = Indices()
    private val ubo              = UBO()

    private lateinit var device: VkDevice
    private lateinit var renderPass: VkRenderPass

    private lateinit var bufferAllocs: BufferAllocs
    private lateinit var descriptors: Descriptors
    private lateinit var pipeline: GraphicsPipeline

    fun init(context: RenderContext,
             renderBuffers: RenderBuffers,
             imageView: VkImageView,
             sampler:VkSampler)
        : Quad
    {
        this.device       = context.device
        this.renderPass   = context.renderPass
        this.bufferAllocs = BufferAllocs(renderBuffers)

        createDescriptors(imageView, sampler)
        createPipeline(context)

        isInitialised = true
        return this
    }
    fun destroy() {
        if(isInitialised) {
            bufferAllocs.free()
            descriptors.destroy()
            pipeline.destroy()
        }
    }
    fun camera(camera: Camera2D) : Quad {
        camera.V(ubo.view)
        camera.P(ubo.proj)

        uboUploaded = false
        return this
    }
    fun model(model:Matrix4f) : Quad {
        ubo.model.set(model)

        uboUploaded = false
        return this
    }
    fun setColour(c: RGBA) : Quad {
        vertices.setColour(c)
        verticesUploaded = false
        return this
    }
    fun setUV(topLeft:Vector2f, bottomRight:Vector2f) : Quad {
        vertices.setUV(topLeft, bottomRight)
        verticesUploaded = false
        return this
    }
    fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        assert(isInitialised)

        uploadVertices(res)
        uploadUBO(res)
    }
    fun insideRenderPass(frame: FrameInfo, res: PerFrameResource) {
        res.adhocCB.run {
            bindPipeline(pipeline)
            bindDescriptorSets(
                pipelineBindPoint = VK_PIPELINE_BIND_POINT_GRAPHICS,
                pipelineLayout    = pipeline.layout,
                firstSet          = 0,
                descriptorSets    = arrayOf(descriptors.layout(0).set(0)),
                dynamicOffsets    = intArrayOf())
            bindVertexBuffer(
                binding = 0,
                buffer  = bufferAllocs.vertexBuffer
            )
            bindIndexBuffer(
                buffer    = bufferAllocs.indexBuffer,
                useShorts = true)
            drawIndexed(
                indexCount    = 6,
                instanceCount = 1,
                firstIndex    = 0,
                vertexOffset  = 0,
                firstInstance = 0)
        }
    }
    //==========================================================================================
    // PRIVATE
    //==========================================================================================
    private fun uploadUBO(res: PerFrameResource) {
        if(uboUploaded) return

        ubo.transfer(res.adhocCB, bufferAllocs.stagingUniform, bufferAllocs.uniformBuffer)

        uboUploaded = true
    }
    private fun uploadVertices(res: PerFrameResource) {
        if(verticesUploaded) return

        vertices.transfer(res.adhocCB, bufferAllocs.stagingVertices, bufferAllocs.vertexBuffer)
        indices.transfer(res.adhocCB, bufferAllocs.stagingIndices, bufferAllocs.indexBuffer)

        verticesUploaded = true
    }
    private fun createDescriptors(imageView:VkImageView, sampler:VkSampler) {
        /**
         * Bindings:
         *    0     uniform buffer
         *    1     image sampler
         */
        this.descriptors = Descriptors()
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_VERTEX_BIT)
            .combinedImageSampler(VK_SHADER_STAGE_FRAGMENT_BIT)
            .numSets(1)
            .build(device)

        descriptors.layout(0).createSet()
            .add(bufferAllocs.uniformBuffer)
            .add(imageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, sampler)
            .write()
    }
    private fun createPipeline(context:RenderContext) {
        pipeline = GraphicsPipeline(context)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShaderProperties(mapOf(), listOf())
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "Quad.vert")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Quad.frag")
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            .build()
    }
}
