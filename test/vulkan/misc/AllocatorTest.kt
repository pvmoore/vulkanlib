package vulkan.misc

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.aggregator.ArgumentsAccessor
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import vulkan.misc.Allocator.Allocation
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class AllocatorTest {

    @Test
    fun `new instance`() {
        val alloc = Allocator(100)
        assertEquals(alloc.bytesFree, 100)
        assertEquals(alloc.bytesUsed, 0)
        assertEquals(alloc.numAllocs, 0)
        assertEquals(alloc.numFrees, 0)
        assertEquals(alloc.allocations, listOf())
    }
    @Test
    fun `align 1`() {
        val a     = Allocator(100)
        val align = 1

        a.testAlloc(size = 10, align = align, expectedOffset = 0,
            numAllocs       = 1,
            numFrees        = 0,
            bytesUsed       = 10,
            bytesFree       = 90,
            allocations     = listOf(Allocation(0, 10))
        )
        a.testAlloc(size = 10, align = align, expectedOffset = 10,
            numAllocs       = 2,
            numFrees        = 0,
            bytesUsed       = 20,
            bytesFree       = 80,
            allocations     = listOf(
                Allocation(0, 10),
                Allocation(10, 10)
            )
        )
        a.testFree(offset = 0, // size = 10
            numAllocs       = 2,
            numFrees        = 1,
            bytesUsed       = 10,
            bytesFree       = 90,
            allocations     = listOf(
                Allocation(10, 10)
            )
        )
        // Already freed
        assertThrows<Error> { a.free(0) }

        a.testAlloc(size = 20, align = align, expectedOffset = 20,
            numAllocs       = 3,
            numFrees        = 1,
            bytesUsed       = 30,
            bytesFree       = 70,
            allocations     = listOf(
                Allocation(10, 10),
                Allocation(20, 20))
        )
    }
    @Test
    fun `align 2`() {
        val a     = Allocator(100)
        val align = 2

        a.testAlloc(size = 11, align = align, expectedOffset = 0,
            numAllocs       = 1,
            numFrees        = 0,
            bytesUsed       = 11,
            bytesFree       = 100-11,
            allocations     = listOf(Allocation(0, 11))
        )
        a.testAlloc(size = 10, align = align, expectedOffset = 12,
            numAllocs       = 2,
            numFrees        = 0,
            bytesUsed       = 21,
            bytesFree       = 100-21,
            allocations     = listOf(
                Allocation(0, 11),
                Allocation(12, 10))
        )
        a.testAlloc(size = 11, align = align, expectedOffset = 22,
            numAllocs       = 3,
            numFrees        = 0,
            bytesUsed       = 32,
            bytesFree       = 100-32,
            allocations     = listOf(
                Allocation(0,  11),
                Allocation(12, 10),
                Allocation(22, 11))
        )
    }
    @Test
    fun `align 3`() {
        val a = Allocator(100)

        assertThrows<AssertionError> {
            a.alloc(10, 3)
        }
    }
    @Test
    fun `align 8`() {
        val a     = Allocator(100)
        val align = 8

        a.testAlloc(size = 11, align = align, expectedOffset = 0,
            numAllocs       = 1,
            numFrees        = 0,
            bytesUsed       = 11,
            bytesFree       = 100-11,
            allocations     = listOf(
                Allocation(0,  11))
        )
        a.testAlloc(size = 11, align = align, expectedOffset = 16,
            numAllocs       = 2,
            numFrees        = 0,
            bytesUsed       = 22,
            bytesFree       = 100-22,
            allocations     = listOf(
                Allocation(0,  11),
                Allocation(16,  11))
        )
        a.testAlloc(size = 11, align = align, expectedOffset = 32,
            numAllocs       = 3,
            numFrees        = 0,
            bytesUsed       = 33,
            bytesFree       = 100-33,
            allocations     = listOf(
                Allocation(0,  11),
                Allocation(16,  11),
                Allocation(32, 11)
            )
        )
    }
    @Test
    fun `align 256`() {
        val a     = Allocator(10_000)
        val align = 256

        a.testAlloc(size = 11, align = align, expectedOffset = 0,
            numAllocs       = 1,
            numFrees        = 0,
            bytesUsed       = 11,
            bytesFree       = 10000-11,
            allocations     = listOf(
                Allocation(0,  11))
        )
        a.testAlloc(size = 11, align = align, expectedOffset = 256,
            numAllocs       = 2,
            numFrees        = 0,
            bytesUsed       = 22,
            bytesFree       = 10000-22,
            allocations     = listOf(
                Allocation(0,   11),
                Allocation(256, 11))
        )
        a.testAlloc(size = 11, align = align, expectedOffset = 512,
            numAllocs       = 3,
            numFrees        = 0,
            bytesUsed       = 33,
            bytesFree       = 10000-33,
            allocations     = listOf(
                Allocation(0,   11),
                Allocation(256, 11),
                Allocation(512, 11))
        )
        a.testFree(offset = 256,
            numAllocs       = 3,
            numFrees        = 1,
            bytesUsed       = 22,
            bytesFree       = 10000-22,
            allocations     = listOf(
                Allocation(0,   11),
                Allocation(512, 11))
        )
        a.testFree(offset = 0,
            numAllocs       = 3,
            numFrees        = 2,
            bytesUsed       = 11,
            bytesFree       = 10000-11,
            allocations     = listOf(
                Allocation(512, 11))
        )
        a.testFree(offset = 512,
            numAllocs       = 3,
            numFrees        = 3,
            bytesUsed       = 0,
            bytesFree       = 10000,
            allocations     = listOf()
        )
    }
    @Test
    fun freeAll() {
        val a = Allocator(100)
        a.alloc(10, 1)
        a.freeAll()

        assertEquals(a.numAllocs, 1)
        assertEquals(a.numFrees, 0)
        assertEquals(a.bytesUsed, 0)
        assertEquals(a.bytesFree, 100)
        assertEquals(a.allocations, listOf())
    }
    @Test
    fun expand() {
        val a = Allocator(0)
        assertTrue { a.sizeBytes==0 && a.bytesFree==0 && a.bytesUsed==0 }

        a.expand(100)
        assertTrue { a.sizeBytes==100 && a.bytesFree==100 && a.bytesUsed==0 }

        a.alloc(50)

        a.expand(200)
        assertTrue { a.sizeBytes==200 && a.bytesFree==150 && a.bytesUsed==50 }

        assertThrows<Error> { a.expand(190) }
    }
    @Disabled
    @ParameterizedTest
    @ValueSource(strings = ["racecar", "radar", "able was I ere I saw elba"])
    fun free(value:String) {
        assertNotNull(value)
    }
    @Disabled
    @ParameterizedTest
    @CsvSource(value=[
        "one, two",
        "1,2"
    ])
    fun testing(args:ArgumentsAccessor) {
        println("testing '${args.getString(0)}', '${args.getString(1)}'")

    }
    @Test
    fun `random alloc and free`() {
        val SIZE     = 10000
        val MAXALLOC = 50

        val a        = Allocator(SIZE)
        val bytes    = IntArray(SIZE)
        val seed     = (Math.random()*1000000).toInt()
        val rand     = Random(seed)
        println("seed = $seed")

        fun spanIsFree(offset:Int, length:Int):Boolean {
            if(offset+length > bytes.size) return false
            for(i in 0 until length) {
                if(bytes[offset + i] == 1) return false
            }
            return true
        }
        fun writeSpan(offset:Int, length:Int, value:Int) {
            for(i in 0 until length) {
                bytes[offset + i] = value
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////
        val allocs     = HashMap<Int,Int>() // memoryOffset, size
        val allocsList = ArrayList<Int>() // memoryOffset

        fun dump() {
            println("allocs = (${allocs.size}) $allocs")
            for(i in 0 until bytes.size) {
                System.out.print(bytes[i])
            }
            println()
        }
        fun allocBytes(size:Int, align:Int) {
            bytes.forEachIndexed { i, byte ->
                if(byte == 0 && (i and (align-1))==0) {
                    if(spanIsFree(i, size)) {
                        assertFalse { allocs.containsKey(i) }
                        allocs[i] = size
                        allocsList.add(i)

                        writeSpan(i, size, 1)
                        return
                    }
                }
            }
        }
        fun freeBytes(offset:Int) {
            assertTrue { allocs.containsKey(offset) }
            val size = allocs.remove(offset)!!
            allocsList.remove(offset)

            writeSpan(offset, size, 0)
        }



        fun compare() {
            bytes.forEachIndexed { i, byte ->
                assertEquals(a.isAllocated(i), byte==1)
            }
            assertEquals(a.allocations.size, allocs.size)
            a.allocations.forEachIndexed { i,a ->
                assertTrue { allocs.containsKey(a.offset) }
                assertTrue { allocs[a.offset] == a.size }
            }
        }
        fun alloc(size:Int, align:Int):Int {
            //println("alloc($size,$align)")
            val offset = a.alloc(size, align)
            if(offset!=-1) {
                allocBytes(size, align)
            }
            return offset
        }
        fun free(offset:Int) {
            //println("free($memoryOffset)")
            a.free(offset)
            freeBytes(offset)
        }
        fun random(max:Int):Int = (rand.nextDouble()*max).toInt()


        // Alloc until full
        while(true) {
            val align  = 1;// shl random(8)
            val size   = 1+random(MAXALLOC)
            val offset = alloc(size, align)

            if(offset==-1) break
        }
        dump()
        compare()
        println("allocs = $allocs")

        var numAllocs = 0
        var numFrees  = 0

        // Randomly free and alloc
        for(i in 0..1000) {
            when(Math.random() < 0.45) {
                true  -> {
                    numAllocs++
                    val align  = 1 shl random(4)
                    val size   = 1 + random(MAXALLOC)
                    val offset = alloc(size, align)
                    //println("alloc($size) $memoryOffset")
                }
                false -> {
                    numFrees++
                    val index  = random(allocsList.size)
                    val offset = allocsList[index]
                    //println("free $memoryOffset")

                    free(offset)
                }
            }
        }
        dump()
        compare()
        println("numAllocs = $numAllocs numFrees = $numFrees")

        // Deallocate everything in random order
        while(allocs.size>0) {
            val index  = random(allocsList.size)
            val offset = allocsList[index]

            free(offset)
        }
        dump()
        compare()
    }
//=============================================================================================
    private fun Allocator.testAlloc(size:Int, align:Int, expectedOffset:Int,
                                    numAllocs:Int,
                                    numFrees:Int,
                                    bytesUsed:Int,
                                    bytesFree:Int,
                                    allocations:List<Allocation>)
    {
        assertEquals(this.alloc(size, align), expectedOffset)
        assertEquals(this.numAllocs, numAllocs)
        assertEquals(this.numFrees, numFrees)
        assertEquals(this.bytesUsed, bytesUsed)
        assertEquals(this.bytesFree, bytesFree)
        assertEquals(this.allocations, allocations)
    }
    private fun Allocator.testFree(offset:Int,
                                   numAllocs:Int,
                                   numFrees:Int,
                                   bytesUsed:Int,
                                   bytesFree:Int,
                                   allocations:List<Allocation>)
    {
        this.free(offset)
        assertEquals(this.numAllocs, numAllocs)
        assertEquals(this.numFrees, numFrees)
        assertEquals(this.bytesUsed, bytesUsed)
        assertEquals(this.bytesFree, bytesFree)
        assertEquals(this.allocations, allocations)
    }
}