package tree.maple.kendec.util


internal object VarInts {
    const val SEGMENT_BITS: Int = 127
    const val CONTINUE_BIT: Int = 128

    fun getSizeInBytesFromInt(i: Int): Int {
        for (j in 1..4) {
            if ((i and (-1 shl j * 7)) == 0) {
                return j
            }
        }

        return 5
    }

    fun getSizeInBytesFromLong(l: Long): Int {
        for (i in 1..9) {
            if ((l and (-1L shl i * 7)) == 0L) {
                return i
            }
        }

        return 10
    }

    fun readInt(readByteSup: ByteSupplier): Int {
        var value = 0
        var position = 0
        var currentByte: Byte

        while (true) {
            currentByte = readByteSup.get()
            value = value or ((currentByte.toInt() and SEGMENT_BITS) shl position)

            if ((currentByte.toInt() and CONTINUE_BIT) == 0) break

            position += 7

            if (position >= 32) throw RuntimeException("VarInt is too big")
        }

        return value
    }

    fun writeInt(value: Int, writeByteFunc: (Byte) -> Unit) {
        var value = value
        while (true) {
            if ((value and SEGMENT_BITS.inv()) == 0) {
                writeByteFunc(value.toByte())
                return
            }

            writeByteFunc(((value and SEGMENT_BITS) or CONTINUE_BIT).toByte())

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
        }
    }

    fun readLong(readByteSup: ByteSupplier): Long {
        var value: Long = 0
        var position = 0
        var currentByte: Byte

        while (true) {
            currentByte = readByteSup.get()
            value = value or ((currentByte.toInt() and SEGMENT_BITS).toLong() shl position)

            if ((currentByte.toInt() and CONTINUE_BIT) == 0) break

            position += 7

            if (position >= 64) throw RuntimeException("VarLong is too big")
        }

        return value
    }

    fun writeLong(value: Long, writeByteFunc: (Byte) -> Unit) {
        var value = value
        while (true) {
            if ((value and SEGMENT_BITS.toLong().inv()) == 0L) {
                writeByteFunc(value.toByte())
                return
            }

            writeByteFunc(((value and SEGMENT_BITS.toLong()) or CONTINUE_BIT.toLong()).toByte())

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value = value ushr 7
        }
    }

    interface ByteSupplier {
        fun get(): Byte
    }
}
