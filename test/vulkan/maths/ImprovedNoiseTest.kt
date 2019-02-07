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
    }
}

