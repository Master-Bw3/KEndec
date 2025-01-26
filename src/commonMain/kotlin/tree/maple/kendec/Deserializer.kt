package tree.maple.kendec

import tree.maple.kendec.util.Optional

interface Deserializer<T> {
    fun setupContext(ctx: SerializationContext): SerializationContext {
        return ctx
    }

    fun readByte(ctx: SerializationContext): Byte
    fun readShort(ctx: SerializationContext): Short
    fun readInt(ctx: SerializationContext): Int
    fun readLong(ctx: SerializationContext): Long
    fun readFloat(ctx: SerializationContext): Float
    fun readDouble(ctx: SerializationContext): Double

    fun readVarInt(ctx: SerializationContext): Int
    fun readVarLong(ctx: SerializationContext): Long

    fun readBoolean(ctx: SerializationContext): Boolean
    fun readString(ctx: SerializationContext): String
    fun readBytes(ctx: SerializationContext): ByteArray
    fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V>

    fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): Sequence<E>
    fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>): Map<V>
    fun struct(): Struct

    fun <V> tryRead(reader: (Deserializer<T>) -> V): V

    interface Sequence<E> : Iterator<E> {
        fun estimatedSize(): Int

        override fun hasNext(): Boolean

        override fun next(): E

    }

    interface Map<E> : Iterator<kotlin.collections.Map.Entry<String, E>> {
        fun estimatedSize(): Int

        override fun hasNext(): Boolean

        override fun next(): kotlin.collections.Map.Entry<String, E>

    }

    interface Struct {
        /**
         * Decode the value of field `name` using `endec`. If no
         * such field exists in the serialized data, then `defaultValue`
         * supplier result is used as the returned value
         */
        fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            defaultValueFactory: (() -> F)?
        ): F?
    }
}
