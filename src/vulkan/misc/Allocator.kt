package vulkan.misc

/**
 * Memory allocator. Keeps track of used regions of memory
 */
private typealias Offset = Int

class Allocator(var sizeBytes:Int) {
    data class Allocation(var offset: Int, var size: Int) {
        val end get() = offset + size
        override fun toString(): String = "[$offset - $end]"
    }

    private val allocsList = ArrayList<Allocation>()
    private val allocsMap  = HashMap<Offset, Allocation>()

    var numAllocs: Int    = 0
    var numFrees: Int     = 0
    var bytesFree: Int    = sizeBytes
    val bytesUsed get()   = sizeBytes - bytesFree
    val allocations get() = allocsList.toList()

    fun isAllocated(offset:Int) = allocations.any { a->
        offset >= a.offset && offset < a.offset+a.size
    }

    fun alloc(size:Int, align:Int = 1):Int {
        assert(Integer.bitCount(align)==1)
        assert(size >= 0)

        val alignMask    = align-1
        val invAlignMask = alignMask.inv()
        var offset       = 0
        numAllocs++

        fun isBigEnough(end:Int):Boolean {
            val freeSize = end-offset
            return freeSize >= size
        }

        allocsList.forEachIndexed { i, allocation ->

            if(isBigEnough(allocation.offset)) {
                val a = Allocation(offset, size)
                allocsList.add(i, a)
                allocsMap[offset] = a
                bytesFree -= size
                return offset
            }

            /** Calculate next memoryOffset */
            offset = (allocation.end+alignMask) and invAlignMask
        }
        if(isBigEnough(sizeBytes)) {
            val a = Allocation(offset, size)
            allocsList.add(a)
            allocsMap[offset] = a
            bytesFree -= size
            return offset
        }

        return -1
    }
    /** @return new memoryOffset (may be the same os old memoryOffset) */
    fun realloc(offset:Int, newSize:Int):Int {
        val allocation = allocsMap.remove(offset) ?: throw Error("Attempt to free unallocated memoryOffset $offset")
        if(newSize==allocation.size) return allocation.offset

        if(newSize > allocation.size) {
            /** Increase size */
        } else {
            /** Decrease size */
        }

        throw NotImplementedError()
    }
    fun free(offset:Int) {
        val allocation = allocsMap.remove(offset) ?: throw Error("Attempt to free unallocated memoryOffset $offset")
        allocsList.remove(allocation)
        bytesFree += allocation.size
        numFrees++
    }
    fun freeAll() {
        allocsList.clear()
        allocsMap.clear()
        bytesFree = sizeBytes
    }
    fun expand(newSize:Int) {
        assert(newSize > sizeBytes)

        val difference = newSize-sizeBytes
        sizeBytes = newSize
        bytesFree += difference
    }
    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("[Allocator used: ${sizeBytes-bytesFree} of $sizeBytes bytes " +
                "(${((sizeBytes-bytesFree)*100.0)/sizeBytes}%) "+
                "in ${allocsList.size} allocations]\n")

        allocsList.forEachIndexed { i, it->
            buf.append("\t[$i]  ${it.offset} - ${it.offset+it.size-1} (${it.size} bytes)\n")
        }

        return buf.toString()
    }
}
