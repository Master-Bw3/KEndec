package tree.maple.kendec.format.forwarding

import tree.maple.kendec.Endec
import tree.maple.kendec.SelfDescribedSerializer
import tree.maple.kendec.SerializationContext
import tree.maple.kendec.Serializer
import tree.maple.kendec.util.Optional

open class ForwardingSerializer<T> protected constructor(private val delegate: Serializer<T>) : Serializer<T> {
    fun delegate(): Serializer<T> {
        return this.delegate
    }

    //--
    override fun writeByte(ctx: SerializationContext, value: Byte) {
        delegate.writeByte(ctx, value)
    }

    override fun writeShort(ctx: SerializationContext, value: Short) {
        delegate.writeShort(ctx, value)
    }

    override fun writeInt(ctx: SerializationContext, value: Int) {
        delegate.writeInt(ctx, value)
    }

    override fun writeLong(ctx: SerializationContext, value: Long) {
        delegate.writeLong(ctx, value)
    }

    override fun writeFloat(ctx: SerializationContext, value: Float) {
        delegate.writeFloat(ctx, value)
    }

    override fun writeDouble(ctx: SerializationContext, value: Double) {
        delegate.writeDouble(ctx, value)
    }

    override fun writeVarInt(ctx: SerializationContext, value: Int) {
        delegate.writeVarInt(ctx, value)
    }

    override fun writeVarLong(ctx: SerializationContext, value: Long) {
        delegate.writeVarLong(ctx, value)
    }

    override fun writeBoolean(ctx: SerializationContext, value: Boolean) {
        delegate.writeBoolean(ctx, value)
    }

    override fun writeString(ctx: SerializationContext, value: String) {
        delegate.writeString(ctx, value)
    }

    override fun writeBytes(ctx: SerializationContext, bytes: ByteArray) {
        delegate.writeBytes(ctx, bytes)
    }

    override fun <V> writeOptional(ctx: SerializationContext, endec: Endec<V>, optional: Optional<V & Any>) {
        delegate.writeOptional(ctx, endec, optional)
    }

    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>, size: Int): Serializer.Sequence<E> {
        return delegate.sequence(ctx, elementEndec, size)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>, size: Int): Serializer.Map<V> {
        return delegate.map(ctx, valueEndec, size)
    }

    override fun struct(): Serializer.Struct {
        return delegate.struct()
    }

    override fun result(): T {
        return delegate.result()
    }

    private class ForwardingSelfDescribedSerializer<T> private constructor(delegate: Serializer<T>) :
        ForwardingSerializer<T>(delegate), SelfDescribedSerializer<T>
}
