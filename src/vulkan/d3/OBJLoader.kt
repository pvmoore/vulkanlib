package vulkan.d3

import org.joml.Vector2f
import org.joml.Vector3f
import vulkan.misc.RGBA
import vulkan.misc.readStringResource
import vulkan.misc.toVector3f

/**
 * Wavefront OBJ file loader.
 *
 * https://en.wikipedia.org/wiki/Wavefront_.obj_file
 */
object OBJLoader {
    interface Callback {
        fun vertex(pos: Vector3f, normal:Vector3f?, uv: Vector2f?, colour:Vector3f?)
        fun material(name:String)
    }

    fun load(filename:String, callback:Callback) {

        val data     = readStringResource(filename)
        val vertices = ArrayList<Vector3f>()
        val normals  = ArrayList<Vector3f>()
        val uvs      = ArrayList<Vector2f>()

        fun addVertex(tokens:List<String>) {
            vertices.add(Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
        }
        fun addUV(tokens:List<String>) {
            uvs.add(Vector2f(tokens[1].toFloat(), tokens[2].toFloat()))
        }
        fun addNormal(tokens:List<String>) {
            normals.add(Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
        }
        /**
         * f  v1/uv1/n1  v2/uv2/n2  v3/uv3/n3   -> supported
         * f  v1/uv1     v2/uv2     v3/uv3      -> todo
         * f  v1//n1     v2//n2     v3//n3      -> todo
         * f  v1         v2         v3          -> todo
         */
        fun addFace(tokens:List<String>) {

            val numSlashes = tokens[1].count { it=='/' }

            when(numSlashes) {
                2 -> for(i in arrayOf(0,1,2)) {
                        val ele = tokens[i + 1].split("/")
                        callback.vertex(
                            vertices[ele[0].toInt()-1],
                            if(ele[2].isNotEmpty()) normals[ele[2].toInt() - 1] else null,
                            if(ele[1].isNotEmpty()) uvs[ele[1].toInt() - 1] else null,
                            RGBA(1f, 0.7f, 0.2f).toVector3f()
                        )
                    }
                1 -> TODO()
                0 -> TODO()
            }
        }

        val whitespace = Regex("\\s+")

        for(it in data.lines()) {
            val line = it.trim()
            if(line.isEmpty() || line.startsWith('#')) continue

            val tokens = line.split(whitespace)

            when(tokens[0]) {
                "v"      -> addVertex(tokens)
                "vt"     -> addUV(tokens)
                "vn"     -> addNormal(tokens)
                "f"      -> addFace(tokens)
                "s"      -> {}  // smooth shading
                "o"      -> {}  // object name
                "g"      -> {}  // group name
                "usemtl" -> callback.material(tokens[1])
            }
        }
    }
}