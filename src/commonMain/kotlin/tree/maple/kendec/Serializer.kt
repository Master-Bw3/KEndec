package tree.maple.kendec

import tree.maple.kendec.util.Endable
import tree.maple.kendec.util.Optional

interface Serializer<T> {
    fun setupContext(ctx: SerializationContext): SerializationContext {
        return ctx
    }

    fun writeByte(ctx: SerializationContext, value: Byte)
    fun writeShort(ctx: SerializationContext, value: Short)
    fun writeInt(ctx: SerializationContext, value: Int)
    fun writeLong(ctx: SerializationContext, value: Long)
    fun writeFloat(ctx: SerializationContext, value: Float)
    fun writeDouble(ctx: SerializationContext, value: Double)

    fun writeVarInt(ctx: SerializationContext, value: Int)
    fun writeVarLong(ctx: SerializationContext, value: Long)

    fun writeBoolean(ctx: SerializationContext, value: Boolean)
    fun writeString(ctx: SerializationContext, value: String)
    fun writeBytes(ctx: SerializationContext, bytes: ByteArray)

    fun <V> writeOptional(ctx: SerializationContext, endec: Endec<V>, optional: Optional<V>)

    fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>, size: Int): Sequence<E>
    fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>, size: Int): Map<V>
    fun struct(): Struct

    fun result(): T

    interface Sequence<E> : Endable {
        fun element(element: E)
    }

    interface Map<V> : Endable {
        fun entry(key: String, value: V)
    }

    interface Struct : Endable {
        fun <F> field(name: String, ctx: SerializationContext, endec: Endec<F>, value: F): Struct {
            return field(name, ctx, endec, value, false)
        }

        fun <F> field(name: String, ctx: SerializationContext, endec: Endec<F>, value: F, mayOmit: Boolean): Struct
    }
}
