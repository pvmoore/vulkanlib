package vulkan.texture

import org.lwjgl.vulkan.VK10.*
import vulkan.common.log
import vulkan.misc.*
import java.nio.ByteBuffer
import java.nio.IntBuffer

/**
 * DXT
 *
 * https://en.wikipedia.org/wiki/DirectDraw_Surface
 * https://docs.microsoft.com/en-gb/windows/desktop/direct3ddds/dx-graphics-dds-pguide
 *
 * DXT1 / BC1 -
 *      RGB
 *      VK_FORMAT_BC1_RGB_UNORM_BLOCK
 *      Use this for images without alpha
 *
 * DXT3 / BC2 -
 *      DOn't use this one
 *
 * DXT5 / BC3 -
 *      RGBA
 *      VK_FORMAT_BC3_UNORM_BLOCK
 *      Use this for images with alpha
 */
object DDSLoader {

    fun load(name:String, bytes:ByteBuffer) : RawImageData {
        log.info("Loading DDS image '$name'")

        fun expect(b:Boolean, msg:String) {
            if(!b) throw Error(msg)
        }

        log.info("Read ${bytes.limit()}")

        val magic = bytes.getInt(0)
        if(magic!=0x20534444) {
            expect(false, "Not a valid DDS file")
        }
        bytes.position(4)

        val header = readHeader(bytes.asIntBuffer())
        log.info("header = $header")

        expect(header.size==124, "DDSHeader.size should be 124")
        expect(header.format.size==32, "DDSPixelFormat.size should be 32")

        if(header.format.flags.isUnset(DDPF.FOURCC.value)) {
            expect(false, "Expecting a fourcc")
        }



        val fourCC = String(charArrayOf(
            (header.format.fourCC and 0xff).toChar(),
            ((header.format.fourCC shr 8) and 0xff).toChar(),
            ((header.format.fourCC shr 16) and 0xff).toChar(),
            ((header.format.fourCC shr 24) and 0xff).toChar()
        ))
        log.info("fourCC = $fourCC ${header.format.fourCC.toString(16)}")

        if(header.format.fourCC==FOURCC.DX10.value) {
            expect(false, "DX10 fourcc not yet supported")
        }

        if(header.format.flags.isSet(DDPF.ALPHAPIXELS.value)) {
            expect(false, "Unsupported ALPHAPIXELS")
        }

        bytes.position(4+124)

        return when(header.format.fourCC) {
            FOURCC.DXT1.value -> compressed(name, header, 8, bytes, VK_FORMAT_BC1_RGB_UNORM_BLOCK)
            FOURCC.DXT3.value -> {
                log.warn("Prefer DXT5 over DXT3 for RGBA images")
                compressed(name, header, 16, bytes, VK_FORMAT_BC2_UNORM_BLOCK)
            }
            FOURCC.DXT5.value -> compressed(name, header, 16, bytes, VK_FORMAT_BC3_UNORM_BLOCK)
            else -> throw Error("Format unsupported: $fourCC")
        }
    }
    //===================================================================================================
    private fun readHeader(b: IntBuffer) : DDSHeader {
        return DDSHeader(
            size         = b.get(),
            flags        = b.get(),
            height       = b.get(),
            width        = b.get(),
            pitch        = b.get(),
            depth        = b.get(),
            mipMapLevels = b.get(),
            format       = readPixelFormat(b.skip(11)),
            caps         = b.get(),
            caps2        = b.getAndSkip(3)
        )
    }
    private fun readPixelFormat(b:IntBuffer) : DDSPixelFormat {
        return DDSPixelFormat(
            size        = b.get(),
            flags       = b.get(),
            fourCC      = b.get(),
            RGBBitCount = b.get(),
            RBitMask    = b.get(),
            GBitMask    = b.get(),
            BBitMask    = b.get(),
            ABitMask    = b.get()
        )
    }
    private fun compressed(name:String,
                           header:DDSHeader,
                           blockBytes:Int,
                           bytes: ByteBuffer,
                           compressedFormat:VkFormat)
        : RawImageData
    {

        log.info("block bytes = $blockBytes")

        val width  = header.width
        val height = header.height

        val size = Math.max(4, width)/4 * Math.max(4, height)/4 * blockBytes

        log.info("size = $size")

        val mipMapCount = if(header.flags.isSet(DDSD.MIPMAPCOUNT.value)) header.mipMapLevels else 1
        log.info("mipMapCount = $mipMapCount")

        if(mipMapCount==1) {
            /** Everything */

            log.info("remaining = ${bytes.remaining()}")

            assert(bytes.remaining()==size)



            return RawImageData(name, bytes, width, height, 3, 1, size, compressedFormat)

        } else throw Error("Mip maps - implement me")

    }
    //====================================================================================================
    private data class DDSHeader(
        val size:Int,
        val flags:Int,          // DDSD
        val height:Int,
        val width:Int,
        val pitch:Int,
        val depth:Int,
        val mipMapLevels:Int,
        // reserved 11 ints
        val format:DDSPixelFormat,
        val caps:Int,                   // DDSCaps
        val caps2:Int                   // DDSCaps2
        //caps3:Int = 0,
        //caps4:Int = 0,
        //reserved int
    )
    private data class DDSPixelFormat(
        val size:Int,
        val flags:Int,          // DDPF
        val fourCC:Int,
        val RGBBitCount:Int,
        val RBitMask:Int,
        val GBitMask:Int,
        val BBitMask:Int,
        val ABitMask:Int
    )
    private class DDS_HEADER_DXT10(
        val dxgiFormat:Int,             // DXGI_FORMAT
        val resourceDImension:Int,      // D3D10_RESOURCE_DIMENSION
        val miscFlag:Int,
        val arraySize:Int,
        val miscFlags2:Int
    )
    private enum class FOURCC(val value:Int) {
        DXT1(0x31545844), // 0x44=D
        DXT2(0x32545844),
        DXT3(0x33545844),
        DXT4(0x34545844),
        DXT5(0x35545844),
        DX10(0x30315844)
    }
    private enum class DDSD(val value:Int) {
        CAPS(0x1),
        HEIGHT(0x2),
        WIDTH(0x4),
        PITCH(0x8),
        PIXELFORMAT(0x1000),
        MIPMAPCOUNT(0x20000),
        LINEARSIZE(0x80000),
        DEPTH(0x800000)
    }
    private enum class DDPF(val value:Int) {
        ALPHAPIXELS(0x1),   // Texture contains alpha data; ABitMask contains valid data.
        ALPHA(0x2),
        FOURCC(0x4),        // Texture contains compressed RGB data; dwFourCC contains valid data.
        RGB(0x40),
        YUV(0x200),
        LUMINANCE(0x20000)
    }
    private enum class DDSCaps(val value:Int) {
        COMPLEX(0x8),
        MIPMAP(0x400000),
        TEXTURE(0x1000)
    }
    private enum class DDSCaps2(val value:Int) {
        CUBEMAP(0x200), 	        // Required for a cube map.
        CUBEMAP_POSITIVEX(0x400), 	// Required when these surfaces are stored in a cube map.
        CUBEMAP_NEGATIVEX(0x800), 	// Required when these surfaces are stored in a cube map.
        CUBEMAP_POSITIVEY(0x1000), 	// Required when these surfaces are stored in a cube map.
        CUBEMAP_NEGATIVEY(0x2000), 	// Required when these surfaces are stored in a cube map.
        CUBEMAP_POSITIVEZ(0x4000), 	// Required when these surfaces are stored in a cube map.
        CUBEMAP_NEGATIVEZ(0x8000), 	// Required when these surfaces are stored in a cube map.
        VOLUME(0x200000) 	        // Required for a volume texture.
    }
}
