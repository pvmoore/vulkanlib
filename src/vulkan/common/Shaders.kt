package vulkan.common

import org.apache.log4j.Logger
import org.lwjgl.vulkan.VkDevice
import vulkan.api.pipeline.VkShaderModule
import vulkan.api.pipeline.createShaderModule
import vulkan.misc.normalisePath
import vulkan.misc.readBinaryFile
import vulkan.misc.readBinaryResource
import vulkan.misc.then
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

object Shaders {
    private const val resourceTargetDirectory:String = "shaders/"
    private val log: Logger                          = Logger.getLogger("Shaders")
    private val srcDirectory:String                  = "./src/shaders/".normalisePath(true)
    private val fileTargetDirectory:String           = "./resources/shaders/".normalisePath(true)

    private val modules = ArrayList<VkShaderModule>()

    private lateinit var device:VkDevice

    fun init(device:VkDevice) {
        Shaders.device = device
    }
    fun destroy() {
        modules.forEach { it.destroy() }
    }

    fun get(name:String, props:Map<String,String>, includes:List<String>): VkShaderModule {
        assertNotNull(device)

        log.info("=============================================================")
        log.info("Shader file : '$name'")
        log.info("=============================================================")

        val file     = File(name)
        val baseName = file.nameWithoutExtension
        val ext      = File(name).extension
        assert(ext in listOf("comp","vert","frag","geom"))

        val code = if(DEBUG) {
            /** Compile the shader and write the spirv to the resources/shaders folder */
            val targetName = "$fileTargetDirectory${baseName}_$ext.spv"
            compileShader(name, targetName, props, includes)
            readBinaryFile(File(targetName))
        } else {
            /** In RELEASE mode, just assume the shader is already in the resources/shaders folder */
            val targetName = "$resourceTargetDirectory${baseName}_$ext.spv"
            log.info("Reading shader resource $targetName")
            readBinaryResource(targetName)
        }

        log.info("Creating shader module")
        val mod = device.createShaderModule(code)

        modules.add(mod)

        return mod
    }
    private fun compileShader(srcName:String, targetName:String, props:Map<String,String>, includes:List<String>) {

        log.info("Compiling shader $srcName --> $targetName")

        val commands = mutableListOf(
            "glslangValidator.exe",
            "-V",
            "-Os",  // minimise size
            //"-g", // debug info
            "-q",   // reflection
            "-t",   // multi-threaded compilation
            "-o", targetName
        )
        commands.addAll(includes.map{"-I$it"})
        commands.addAll(props.map{"-D${it.key}=${it.value}"})
        commands.add(srcName)

        log.debug("Command: ${commands.joinToString()}")

        val proc = ProcessBuilder(commands)
            .directory(File(srcDirectory))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(10, TimeUnit.SECONDS)

        log.info(proc.inputStream.bufferedReader().readText())

        (proc.exitValue()!=0).then {
            log.error(proc.inputStream.bufferedReader().readText())
            log.error(proc.errorStream.bufferedReader().readText())
            throw Error("Compile shader error exit code=${proc.exitValue()}")
        }
    }
}


