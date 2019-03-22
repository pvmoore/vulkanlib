package vulkan.d3

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.vulkan.VK10.*
import vulkan.api.VkSampler
import vulkan.api.buffer.BufferAlloc
import vulkan.api.buffer.bindVertexBuffer
import vulkan.api.createSampler
import vulkan.api.descriptor.bindDescriptorSets
import vulkan.api.draw
import vulkan.api.pipeline.GraphicsPipeline
import vulkan.api.pipeline.bindPipeline
import vulkan.common.*
import vulkan.misc.megabytes
import vulkan.texture.Texture
import vulkan.texture.Textures

class Model3D {
    private val descriptors     = Descriptors()
    private val pipeline        = GraphicsPipeline()
    private val textures        = Textures()
    private val uboVert         = UBOVert()
    private val uboFrag         = UBOFrag()
    private val vertices        = Vertices()
    private var vertUboChanged  = true
    private var fragUboChanged  = true
    private var vertsChanged    = true
    private var modelChanged    = true
    private var sampler         = null as VkSampler?
    private var vertsStagingBuf = null as BufferAlloc?
    private var vertsDeviceBuf  = null as BufferAlloc?

    private var texture         = null as Texture?
    private var scale    = 1f
    private val rotation = Vector3f()

    fun init(context:RenderContext, filename:String) {

        textures.init(context.vk, 1.megabytes())

        OBJLoader.load(filename, object : OBJLoader.Callback {
            override fun vertex(pos : Vector3f, normal : Vector3f?, uv : Vector2f?, colour : Vector3f?) {
                vertices.add(pos, normal, uv, colour)
            }
            override fun material(name : String) {
                texture = texture ?: textures.get(name)
            }
        })
        /**
         * Set texture to default if it has not been set
         */
        texture = texture ?: textures.get("brick.dds")

        log.info("num vertices = ${vertices.numVertices}")

        sampler = context.vk.device.createSampler { info-> }

        uboVert.init(context)
        uboFrag.init(context)

        vertsStagingBuf = context.buffers.get(VulkanBuffers.STAGING_UPLOAD).allocate(vertices.size())
        vertsDeviceBuf  = context.buffers.get(VulkanBuffers.VERTEX).allocate(vertices.size())

        /**
         * Bindings:
         *    0     vert uniform buffer
         *    1     frag uniform buffer
         *    2     sampler
         */
        descriptors
            .init(context)
            .createLayout()
            .uniformBuffer(VK_SHADER_STAGE_VERTEX_BIT)
            .uniformBuffer(VK_SHADER_STAGE_FRAGMENT_BIT)
            .combinedImageSampler(VK_SHADER_STAGE_FRAGMENT_BIT)
            .numSets(1)
            .build()

        descriptors.layout(0).createSet()
            .add(uboVert.deviceBuffer)
            .add(uboFrag.deviceBuffer)
            .add(texture!!.image.getView(), VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, sampler!!)
            .write()

        pipeline
            .init(context)
            .withDSLayouts(arrayOf(descriptors.layout(0).dsLayout))
            .withShaderProperties(mapOf(), listOf())
            .withShader(VK_SHADER_STAGE_VERTEX_BIT,   "OBJModel/OBJModel.vert")
            .withShader(VK_SHADER_STAGE_FRAGMENT_BIT, "OBJModel/OBJModel.frag")
            .withVertexInputState(vertices.elementInstance(), VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            .withRasterisationState { info->
                info.cullMode(VK_CULL_MODE_BACK_BIT)
                info.frontFace(VK_FRONT_FACE_CLOCKWISE)
            }
            .withDepthStencilState { info->
                info.depthTestEnable(true)
                info.depthWriteEnable(true)
                info.depthCompareOp(VK_COMPARE_OP_GREATER_OR_EQUAL)

                info.stencilTestEnable(false)
            }
            .build()
    }
    fun destroy() {
        uboVert.destroy()
        uboFrag.destroy()
        sampler?.destroy()
        textures.destroy()
        descriptors.destroy()
        pipeline.destroy()
    }
    fun camera(camera : Camera3D) {
        camera.VP(uboVert.mvp)
        camera.V(uboVert.v)
        camera.invV(uboVert.invV)
        vertUboChanged = true
    }
    fun rotation(r:Vector3f) {
        this.rotation.set(r)
        modelChanged = true
    }
    fun scale(s:Float) {
        this.scale = s
        modelChanged = true
    }
    fun lightPos(p:Vector3f) {
        uboVert.lightPos.set(p)
        vertUboChanged = true
    }
    fun beforeRenderPass(frame: FrameInfo, res: PerFrameResource) {
        if(vertsChanged) {
            vertices.transfer(res.cmd, vertsStagingBuf, vertsDeviceBuf!!)
            vertsChanged = false
        }
        if(modelChanged) {
            uboVert.m.identity()
            uboVert.m.scale(scale)
            uboVert.m.rotateAffineXYZ(rotation.x, rotation.y, rotation.z)

            vertUboChanged = true
            modelChanged   = false
        }
        if(vertUboChanged) {
            uboVert.transfer(res.cmd)
            vertUboChanged = false
        }
        if(fragUboChanged) {
            uboFrag.transfer(res.cmd)
            fragUboChanged = false
        }
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
                buffer  = vertsDeviceBuf!!
            )
            draw(
                vertexCount   = vertices.numVertices,
                instanceCount = 1,
                firstVertex   = 0,
                firstInstance = 0
            )
        }
    }

    // ##########################################################################################
    // ##########################################################################################
    private class UBOVert(val mvp      : Matrix4f = Matrix4f(),
                          val v        : Matrix4f = Matrix4f(),
                          val invV     : Matrix4f = Matrix4f(),
                          val m        : Matrix4f = Matrix4f(),
                          val lightPos : Vector3f = Vector3f(),
                          val _pad1    : Float    = 0f) : AbsUBO()

    private class UBOFrag(var shineDamping : Float    = 10f,
                          var reflectivity : Float    = 1f,
                          val _pad1        : Vector2f = Vector2f()) : AbsUBO()

    private class Vertices : AbsTransferableArray() {
        private class Vertex(val pos    : Vector3f,
                             val normal : Vector3f,
                             val uv     : Vector2f,
                             val colour : Vector3f) : AbsTransferable()

        val vertices   = ArrayList<Vertex>()
        var hasUVs     = true
        var hasNormals = true
        var hasColours = true

        override fun elementInstance() = Vertex(Vector3f(), Vector3f(), Vector2f(), Vector3f())
        override fun getArray()        = vertices.toTypedArray()

        val numVertices get() = vertices.size

        fun add(pos:Vector3f, normal:Vector3f?, uv:Vector2f?, rgb:Vector3f?) {
            hasUVs     = hasUVs && uv!=null
            hasNormals = hasNormals && normal!=null
            hasColours = hasColours && rgb!=null

            vertices.add(Vertex(pos,
                                normal  ?: Vector3f(),
                                uv      ?: Vector2f(),
                                rgb     ?: Vector3f(1f,1f,1f)))
        }
    }
}
