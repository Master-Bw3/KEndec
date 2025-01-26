package tree.maple.kendec.util

import tree.maple.kendec.Endec
import tree.maple.kendec.SerializationContext

interface EndecBuffer {
    fun <T> write(endec: Endec<T>?, value: T) {
        write(SerializationContext.empty(), endec, value)
    }

    fun <T> write(ctx: SerializationContext?, endec: Endec<T>?, value: T) {
        throw UnsupportedOperationException()
    }

    fun <T> read(endec: Endec<T>?): T {
        return read(SerializationContext.empty(), endec)
    }

    fun <T> read(ctx: SerializationContext?, endec: Endec<T>?): T {
        throw UnsupportedOperationException()
    }
}
