package tree.maple.kendec.format.buffer

import okio.Buffer
import tree.maple.kendec.*
import tree.maple.kendec.util.Optional
import tree.maple.kendec.util.OptionalOfEmpty
import tree.maple.kendec.util.OptionalOfNullable
import tree.maple.kendec.util.VarInts
import kotlin.js.JsExport

@JsExport
class BufferDeserializer(private var buffer: Buffer) : Deserializer<Buffer> {

    override fun readByte(ctx: SerializationContext): Byte {
        return buffer.readByte()
    }

    override fun readShort(ctx: SerializationContext): Short {
        return buffer.readShort()
    }

    override fun readInt(ctx: SerializationContext): Int {
        return buffer.readInt()
    }

    override fun readLong(ctx: SerializationContext): Long {
        return buffer.readLong()
    }

    override fun readFloat(ctx: SerializationContext): Float {
        return Float.fromBits(buffer.readInt())
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return Double.fromBits(buffer.readLong())
    }

    override fun readVarInt(ctx: SerializationContext): Int {
        return VarInts.readInt({ this.readByte(ctx) })
    }

    override fun readVarLong(ctx: SerializationContext): Long {
        return VarInts.readInt({ this.readByte(ctx) }).toLong()
    }

    override fun readBoolean(ctx: SerializationContext): Boolean {
        return buffer.readByte() == 1.toByte()
    }

    override fun readString(ctx: SerializationContext): String {
        val sequenceLength = this.readVarInt(ctx).toLong()

        return buffer.readUtf8(sequenceLength)
    }

    override fun readBytes(ctx: SerializationContext): ByteArray {
        val array = ByteArray(this.readVarInt(ctx))
        this.buffer.read(array)

        return array
    }

    override fun <V> readOptional(
        ctx: SerializationContext,
        endec: Endec<V>
    ): Optional<V & Any> {
        return if (this.readBoolean(ctx))
            OptionalOfNullable(endec.decode(ctx, this))
        else
            OptionalOfEmpty()
    }

    override fun <V> tryRead(reader: (Deserializer<Buffer>) -> V): V {
        val copy = this.buffer.copy()

        try {
            return reader(this)
        } catch (e: Exception) {
            this.buffer = copy
            throw e
        }
    }

    override fun <E> sequence(
        ctx: SerializationContext,
        elementEndec: Endec<E>
    ): SequenceDeserializer<E> {
        return this.Sequence(
            ctx,
            elementEndec,
            this.readVarInt(ctx)
        )
    }

    override fun <V> map(
        ctx: SerializationContext,
        valueEndec: Endec<V>
    ): MapDeserializer<V> {
        return this.Map(ctx, valueEndec, this.readVarInt(ctx))
    }

    override fun struct(): StructDeserializer {
        return this.Sequence<Any>(null, null, 0)
    }

    // ---
    private inner class Sequence<V> internal constructor(
        private val ctx: SerializationContext?,
        private val valueEndec: Endec<V>?,
        private val size: Int
    ) : SequenceDeserializer<V>, StructDeserializer {
        private var index = 0

        public override fun estimatedSize(): Int {
            return this.size
        }

        public override fun hasNext(): Boolean {
            return this.index < this.size
        }

        public override fun next(): V {
            this.index++
            return this.valueEndec!!.decode(this.ctx!!, this@BufferDeserializer)
        }

        public override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            defaultValueFactory: (() -> F)?
        ): F {
            return endec.decode(ctx, this@BufferDeserializer)
        }
    }

    private inner class Map<V> internal constructor(
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        private val size: Int
    ) : MapDeserializer<V> {
        private var index = 0

        public override fun estimatedSize(): Int {
            return this.size
        }

        public override fun hasNext(): Boolean {
            return this.index < this.size
        }

        public override fun next(): kotlin.collections.Map.Entry<String, V> {
            this.index++
            return object : Map.Entry<String, V> {
                override val key: String = this@BufferDeserializer.readString(this@Map.ctx)

                override val value: V = this@Map.valueEndec.decode(this@Map.ctx, this@BufferDeserializer)!!
            }
        }
    }

}