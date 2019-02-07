package vulkan.maths

import kotlin.random.Random

/**
 * Adapted from http://mrl.nyu.edu/~perlin/noise/
 */
class ImprovedNoise {

    /** @return a value in the range -0.5 -> +0.5 */
    fun noise(x1:Double) : Double {
        return noise(x1, 0.0, 0.0)
    }
    /** @return a value in the range -0.5 -> +0.5 */
    fun noise(x1:Double, y1:Double) : Double {
        return noise(x1, y1, 0.0)
    }
    /** @return a value in the range -0.5 -> +0.5 */
    fun noise(x1:Double, y1:Double, z1:Double) : Double {
        val fx = x1.toInt()
        val fy = y1.toInt()
        val fz = z1.toInt()

        val X = fx and 255 // FIND UNIT CUBE THAT
        val Y = fy and 255 // CONTAINS POINT
        val Z = fz and 255

        val x  = x1 - fx    // FIND RELATIVE X,Y,Z
        val y  = y1 - fy    // OF POINT IN CUBE
        val z  = z1 - fz

        val u  = fade(x)        // COMPUTE FADE CURVES
        val v  = fade(y)        // FOR EACH OF X,Y,Z
        val w  = fade(z)

        val A  = p[X]+Y         // HASH COORDINATES OF
        val AA = p[A]+Z         // THE 8 CUBE CORNERS
        val AB = p[A+1]+Z
        val B  = p[X+1]+Y
        val BA = p[B]+Z
        val BB = p[B+1]+Z

        return lerp(w,  lerp(v, lerp(u, grad(p[AA  ], x  , y  , z   ),  // AND ADD
                                        grad(p[BA  ], x-1, y  , z   )), // BLENDED
                                lerp(u, grad(p[AB  ], x  , y-1, z   ),  // RESULTS
                                        grad(p[BB  ], x-1, y-1, z   ))),// FROM  8
                        lerp(v, lerp(u, grad(p[AA+1], x  , y  , z-1 ),  // CORNERS
                                        grad(p[BA+1], x-1, y  , z-1 )), // OF CUBE
                                lerp(u, grad(p[AB+1], x  , y-1, z-1 ),
                                        grad(p[BB+1], x-1, y-1, z-1 ))))
    }
    fun noise(x:Double, y:Double, z:Double, octaves:Int, persistence:Double) : Double {
        var total     = 0.0
        var frequency = 1
        var amplitude = 1.0
        var maxValue  = 0.0  // Used for normalizing result to 0.0 - 1.0

        for(i in 0 until octaves) {
            total += noise(x * frequency, y * frequency, z * frequency) * amplitude

            maxValue += amplitude

            amplitude *= persistence
            frequency *= 2
        }

        return total/maxValue
    }

    /**
     * Generate a 2D array (size*size) of noise bytes in the range 0..255
     */
    fun noiseArray2D(size:Int, wavelength:Double, octaves:Int, persistence:Double) : ByteArray {

        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE

        val bytes   = ByteArray(size*size)
        val doubles = DoubleArray(size*size)

        // Generate random values
        for(y in 0 until size) {
            for(x in 0 until size) {
                val v = (noise(x.toDouble()/wavelength, y.toDouble()/wavelength, 0.0,
                               octaves, persistence))

                if(v>max) max = v
                if(v<min) min = v

                doubles[x + y * size] = v
            }
        }

        // Normalise the values to the 0 to 255 range
        val mul = 255.0 / (max-min)

        doubles.forEachIndexed { i, v ->

            val b = (v - min) * mul

            bytes[i] = b.toByte()
        }

        return bytes
    }
    companion object {
        private fun fade(t:Double) : Double {
            return t * t * t * (t * (t * 6 - 15) + 10)
        }
        private fun lerp(t:Double, a:Double, b:Double) : Double {
            return a + t * (b - a)
        }
        private fun grad(hash:Int, x:Double, y:Double, z:Double) : Double {
            return when(hash and 0xF) {
                0x0  ->  x + y
                0x1  -> -x + y
                0x2  ->  x - y
                0x3  -> -x - y
                0x4  ->  x + z
                0x5  -> -x + z
                0x6  ->  x - z
                0x7  -> -x - z
                0x8  ->  y + z
                0x9  -> -y + z
                0xA  ->  y - z
                0xB  -> -y - z
                0xC  ->  y + x
                0xD  -> -y + z
                0xE  ->  y - x
                0xF  -> -y - z
                else -> 0.0      // never happens
            }
        }
        private val p = IntArray(512)
        private val rand = Random(System.nanoTime().toInt())
        private val permutations = IntArray(size = 256, init = { rand.nextInt(0, 256) })
        init{
            permutations.copyInto(p, 0)
            permutations.copyInto(p, 256)
        }
    }
}
/*
/**
 *  Adapted from Ken Perlin's improved noise:
 *  http://mrl.nyu.edu/~perlin/noise/
 */
final class ImprovedPerlin : NoiseFunction1D,
                             NoiseFunction2D,
                             NoiseFunction3D
{
private:
    Mt19937 rng;
    int[512] p;
public:
    this(uint seed=unpredictableSeed()) {
        rng.seed(seed);
        for(int i=0; i<256 ;i++) {
            p[256+i] = p[i] = uniform(0,256, rng);
        }
    }
    /// returns a value in the range -0.5 -> +0.5
    Double get(Double x) {
        return get(x,0,0);
    }
    /// returns a value in the range -0.5 -> +0.5
    Double get(Vector2 v) {
        return get(v.x,v.y,0);
    }
    /// returns a value in the range -0.5 -> +0.5
    Double get(Double x, Double y) {
        return get(x,y,0);
    }
    /// returns a value in the range -0.5 -> +0.5
    Double get(Vector3 v) {
        return get(v.x, v.y, v.z);
    }
    /// returns a value in the range -0.5 -> +0.5
    Double get(Double x, Double y, Double z) {
        int fx = cast(int)x;
        int fy = cast(int)y;
        int fz = cast(int)z;
        int X = fx & 255;   // FIND UNIT CUBE THAT
        int Y = fy & 255;   // CONTAINS POINT.
        int Z = fz & 255;
        x -= fx;            // FIND RELATIVE X,Y,Z
        y -= fy;            // OF POINT IN CUBE.
        z -= fz;
        Double u = fade(x);  // COMPUTE FADE CURVES
        Double v = fade(y);  // FOR EACH OF X,Y,Z.
        Double w = fade(z);
        int A  = p[X]+Y;    // HASH COORDINATES OF
        int AA = p[A]+Z;    // THE 8 CUBE CORNERS,
        int AB = p[A+1]+Z;
        int B  = p[X+1]+Y;
        int BA = p[B]+Z;
        int BB = p[B+1]+Z;

        Double f = lerp(w, lerp(v, lerp(u,  grad(p[AA  ], x  , y  , z   ),  // AND ADD
                                           grad(p[BA  ], x-1, y  , z   )), // BLENDED
                                   lerp(u, grad(p[AB  ], x  , y-1, z   ),  // RESULTS
                                           grad(p[BB  ], x-1, y-1, z   ))),// FROM  8
                           lerp(v, lerp(u, grad(p[AA+1], x  , y  , z-1 ),  // CORNERS
                                           grad(p[BA+1], x-1, y  , z-1 )), // OF CUBE
                                   lerp(u, grad(p[AB+1], x  , y-1, z-1 ),
                                           grad(p[BB+1], x-1, y-1, z-1 ))));
        return f;
   }
   Double get(Double x, Double y, Double z, int octaves, Double persistence) {
       Double total = 0;
       Double frequency = 1;
       Double amplitude = 1;
       Double maxValue = 0;  // Used for normalizing result to 0.0 - 1.0
       for(int i=0; i<octaves; i++) {
           total += get(x * frequency, y * frequency, z * frequency) * amplitude;

           maxValue += amplitude;

           amplitude *= persistence;
           frequency *= 2;
       }

       return total/maxValue;
   }
private:
    static Double fade(Double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    static Double lerp(Double t, Double a, Double b) {
        return a + t * (b - a);
    }
    static Double grad(int hash, Double x, Double y, Double z)
    {
//        int h   = hash & 15;    // CONVERT LO 4 BITS OF HASH CODE
//        Double u = h<8 ? x : y;  // INTO 12 GRADIENT DIRECTIONS.
//        Double v = h<4 ? y : h==12||h==14 ? x : z;
//        return ((h&1) == 0 ? u : -u) + ((h&2) == 0 ? v : -v);
        switch(hash & 0xF) {
            case 0x0: return  x + y;
            case 0x1: return -x + y;
            case 0x2: return  x - y;
            case 0x3: return -x - y;
            case 0x4: return  x + z;
            case 0x5: return -x + z;
            case 0x6: return  x - z;
            case 0x7: return -x - z;
            case 0x8: return  y + z;
            case 0x9: return -y + z;
            case 0xA: return  y - z;
            case 0xB: return -y - z;
            case 0xC: return  y + x;
            case 0xD: return -y + z;
            case 0xE: return  y - x;
            case 0xF: return -y - z;
            default: return 0; // never happens
        }
    }
}
 */