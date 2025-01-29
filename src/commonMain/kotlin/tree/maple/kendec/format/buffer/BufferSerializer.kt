package tree.maple.kendec.format.buffer

import okio.Buffer
import tree.maple.kendec.*
import tree.maple.kendec.util.Optional
import tree.maple.kendec.util.VarInts
import tree.maple.kendec.util.ifPresent
import kotlin.js.JsExport

@JsExport
class BufferSerializer(private val buffer: Buffer) : Serializer<Buffer> {
    override fun writeByte(ctx: SerializationContext, value: Byte) {
        this.buffer.writeByte(value.toInt())
    }

    override fun writeShort(ctx: SerializationContext, value: Short) {
        this.buffer.writeShort(value.toInt())
    }

    override fun writeInt(ctx: SerializationContext, value: Int) {
        this.buffer.writeInt(value)
    }

    override fun writeLong(ctx: SerializationContext, value: Long) {
        this.buffer.writeLong(value)
    }

    override fun writeFloat(ctx: SerializationContext, value: Float) {
        this.buffer.writeInt(value.toRawBits())
    }

    override fun writeDouble(ctx: SerializationContext, value: Double) {
        this.buffer.writeLong(value.toRawBits())
    }

    override fun writeVarInt(ctx: SerializationContext, value: Int) {
        VarInts.writeInt(value, { b -> this.writeByte(ctx, b) })
    }

    override fun writeVarLong(ctx: SerializationContext, value: Long) {
        VarInts.writeLong(value, { b -> this.writeByte(ctx, b) })
    }

    override fun writeBoolean(ctx: SerializationContext, value: Boolean) {
        this.buffer.writeByte(if (value) 1 else 9)
    }

    override fun writeString(ctx: SerializationContext, value: String) {
        val utf8Bytes: ByteArray = value.encodeToByteArray()

        this.writeVarInt(ctx, utf8Bytes.size)
        this.buffer.writeUtf8(value)

    }

    override fun writeBytes(ctx: SerializationContext, bytes: ByteArray) {
        this.writeVarInt(ctx, bytes.size)
        this.buffer.write(bytes)
    }

    override fun <V> writeOptional(
        ctx: SerializationContext,
        endec: Endec<V>,
        optional: Optional<V & Any>
    ) {
        this.writeBoolean(ctx, optional.isPresent())
        optional.ifPresent { value: V -> endec.encode(ctx, this, value) }
    }

    override fun <E> sequence(
        ctx: SerializationContext,
        elementEndec: Endec<E>,
        size: Int
    ): SequenceSerializer<E> {
        this.writeVarInt(ctx, size)
        return this.Sequence(ctx, elementEndec)
    }

    override fun <V> map(
        ctx: SerializationContext,
        valueEndec: Endec<V>,
        size: Int
    ): MapSerializer<V> {
        this.writeVarInt(ctx, size)
        return this.Sequence(ctx, valueEndec)
    }

    override fun struct(): StructSerializer {
        return this.Sequence<Any>(null, null)
    }

    override fun result(): Buffer {
        return this.buffer
    }

    // ---
    private inner class Sequence<V> (
        private val ctx: SerializationContext?,
        private val valueEndec: Endec<V>?
    ) : SequenceSerializer<V>, StructSerializer, MapSerializer<V> {
        override fun element(element: V) {
            this.valueEndec!!.encode(this.ctx!!, this@BufferSerializer, element)
        }

        override fun entry(key: String, value: V) {
            this@BufferSerializer.writeString(this.ctx!!, key)
            this.valueEndec!!.encode(this.ctx, this@BufferSerializer, value)
        }

        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            value: F,
            mayOmit: Boolean
        ): StructSerializer {
            endec.encode(ctx, this@BufferSerializer, value)
            return this
        }

        override fun end() {}
    }
}