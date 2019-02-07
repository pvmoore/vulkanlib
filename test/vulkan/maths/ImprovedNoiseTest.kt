package vulkan.maths

import org.junit.jupiter.api.Test

internal class ImprovedNoiseTest {
    @Test
    fun `new instance`() {
        val noise = ImprovedNoise()

        val a = noise.noise(2.043)
        val b = noise.noise(0.5, 0.1)
        val c = noise.noise(0.5, 0.3, 2.3)
        println("a=$a b=$b c=$c")

        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        for(i in 0..10000) {
            val r = noise.noise(i.toDouble() / 100.0, 0.0, 0.0, 5, 0.5)
            if(r < min) min = r
            if(r > max) max = r
        }
        println(String.format("min = %.2f max = %.2f", min, max))

        min = Double.MAX_VALUE
        max = -Double.MAX_VALUE
        for(i in 0..10000) {
            val r = noise.noise(i.toDouble() / 100.0)
            if(r < min) min = r
            if(r > max) max = r
        }
        println(String.format("min = %.2f max = %.2f", min, max))
    }
}

