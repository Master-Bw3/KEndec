@file:JsExport
package tree.maple.kendec.format.forwarding

import tree.maple.kendec.*
import tree.maple.kendec.util.Optional
import kotlin.js.JsExport

open class ForwardingDeserializer<T> protected constructor(private val delegate: Deserializer<T>) : Deserializer<T> {
    fun delegate(): Deserializer<T> {
        return this.delegate
    }

    //--
    override fun readByte(ctx: SerializationContext): Byte {
        return delegate.readByte(ctx)
    }

    override fun readShort(ctx: SerializationContext): Short {
        return delegate.readShort(ctx)
    }

    override fun readInt(ctx: SerializationContext): Int {
        return delegate.readInt(ctx)
    }

    override fun readLong(ctx: SerializationContext): Long {
        return delegate.readLong(ctx)
    }

    override fun readFloat(ctx: SerializationContext): Float {
        return delegate.readFloat(ctx)
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return delegate.readDouble(ctx)
    }

    override fun readVarInt(ctx: SerializationContext): Int {
        return delegate.readVarInt(ctx)
    }

    override fun readVarLong(ctx: SerializationContext): Long {
        return delegate.readVarLong(ctx)
    }

    override fun readBoolean(ctx: SerializationContext): Boolean {
        return delegate.readBoolean(ctx)
    }

    override fun readString(ctx: SerializationContext): String {
        return delegate.readString(ctx)
    }

    override fun readBytes(ctx: SerializationContext): ByteArray {
        return delegate.readBytes(ctx)
    }

    override fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V & Any> {
        return delegate.readOptional(ctx, endec)
    }

    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): SequenceDeserializer<E> {
        return delegate.sequence(ctx, elementEndec)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>): MapDeserializer<V> {
        return delegate.map(ctx, valueEndec)
    }

    override fun struct(): StructDeserializer {
        return delegate.struct()
    }

    override fun <V> tryRead(reader: (Deserializer<T>) -> V): V {
        return delegate.tryRead(reader)
    }

    private class ForwardingSelfDescribedDeserializer<T> private constructor(delegate: Deserializer<T>) :
        ForwardingDeserializer<T>(delegate), SelfDescribedDeserializer<T> {

        override fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>) {
            (delegate() as SelfDescribedDeserializer<T>).readAny(ctx, visitor)
        }
    }
}
