package vulkan.d2

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector4f
import org.lwjgl.vulkan.VK10.*
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
    private lateinit var context: RenderContext
    private lateinit var bufferAllocs: BufferAllocs

    private var isInitialised    = false
    private var verticesUploaded = false
    private val vertices         = Vertices()
    private val indices          = Indices()
    private val ubo              = UBO()
    private val pipeline         = GraphicsPipeline()
    private val descriptors      = Descriptors()

    fun init(context: RenderContext,
             imageView: VkImageView,
             sampler:VkSampler)
        : Quad
    {
        this.context      = context
        this.bufferAllocs = BufferAllocs(context.buffers)

        ubo.init(context)

        createDescriptors(imageView, sampler)
        createPipeline(context)

        isInitialised = true
        return this
    }
    fun destroy() {
        if(isInitialised) {
            ubo.destroy()
            bufferAllocs.free()
            descriptors.destroy()
            pipeline.destroy()
        }
    }
    fun camera(camera: Camera2D) : Quad {
        camera.V(ubo.view)
        camera.P(ubo.proj)
        ubo.setStale()
        return this
    }
    fun model(model:Matrix4f) : Quad {
        ubo.model.set(model)
        ubo.setStale()
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
        res.cmd.run {
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
        ubo.transfer(res.cmd)
    }
    private fun uploadVertices(res: PerFrameResource) {
        if(verticesUploaded) return

        vertices.transfer(res.cmd, bufferAllocs.stagingVertices, bufferAllocs.vertexBuffer)
        indices.transfer(res.cmd, bufferAllocs.stagingIndices, bufferAllocs.indexBuffer)

        verticesUploaded = true
    }
    private fun createDescriptors(imageView:VkImageView, sampler:VkSampler) {
        /**
         * Bindings:
         *    0     uniform buffer
         *    1     image sampler
         */
        this.descriptors
            .init(context)
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_VERTEX_BIT)
            .combinedImageSampler(VK_SHADER_STAGE_FRAGMENT_BIT)
            .numSets(1)
            .build()

        descriptors.layout(0).createSet()
            .add(ubo.deviceBuffer)
            .add(imageView, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, sampler)
            .write()
    }
    private fun createPipeline(context:RenderContext) {
        pipeline.init(context)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShaderProperties(mapOf(), listOf())
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "Quad/Quad.vert")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "Quad/Quad.frag")
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            .build()
    }

    //=================================================================================================

    private class UBO(val model:Matrix4f = Matrix4f(),
                      val view:Matrix4f  = Matrix4f(),
                      val proj:Matrix4f  = Matrix4f()) : AbsUBO()

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
        b: VulkanBuffers,

        val stagingVertices: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(vertices.size()).orThrow(),
        val stagingIndices: BufferAlloc = b.get(VulkanBuffers.STAGING_UPLOAD).allocate(indices.size()).orThrow(),

        val vertexBuffer: BufferAlloc  = b.get(VulkanBuffers.VERTEX).allocate(vertices.size()).orThrow(),
        val indexBuffer: BufferAlloc   = b.get(VulkanBuffers.INDEX).allocate(indices.size()).orThrow())
    {
        fun free() {
            stagingVertices.free()
            stagingIndices.free()
            vertexBuffer.free()
            indexBuffer.free()
        }
    }
}
