package tree.maple.kendec.format.edm

import tree.maple.kendec.*
import tree.maple.kendec.util.Optional
import tree.maple.kendec.util.OptionalOfNullable
import tree.maple.kendec.util.RecursiveSerializer
import tree.maple.kendec.util.ifPresent

class EdmSerializer : RecursiveSerializer<EdmElement<*>?>(null), SelfDescribedSerializer<EdmElement<*>?> {
    // ---
    override fun writeByte(ctx: SerializationContext, value: Byte) {
        this.consume(EdmElement.i8(value))
    }

    override fun writeShort(ctx: SerializationContext, value: Short) {
        this.consume(EdmElement.i16(value))
    }

    override fun writeInt(ctx: SerializationContext, value: Int) {
        this.consume(EdmElement.i32(value))
    }

    override fun writeLong(ctx: SerializationContext, value: Long) {
        this.consume(EdmElement.i64(value))
    }

    // ---
    override fun writeFloat(ctx: SerializationContext, value: Float) {
        this.consume(EdmElement.f32(value))
    }

    override fun writeDouble(ctx: SerializationContext, value: Double) {
        this.consume(EdmElement.f64(value))
    }

    // ---
    override fun writeVarInt(ctx: SerializationContext, value: Int) {
        this.consume(EdmElement.i32(value))
    }

    override fun writeVarLong(ctx: SerializationContext, value: Long) {
        this.consume(EdmElement.i64(value))
    }

    // ---
    override fun writeBoolean(ctx: SerializationContext, value: Boolean) {
        this.consume(EdmElement.bool(value))
    }

    override fun writeString(ctx: SerializationContext, value: String) {
        this.consume(EdmElement.string(value))
    }

    override fun writeBytes(ctx: SerializationContext, bytes: ByteArray) {
        this.consume(EdmElement.bytes(bytes))
    }

    override fun <V> writeOptional(ctx: SerializationContext, endec: Endec<V>, optional: Optional<V & Any>) {
        val result: Array<EdmElement<*>?> = arrayOfNulls(1)
        this.frame { encoded ->
            optional.ifPresent { v: V -> endec.encode(ctx, this, v) }
            result[0] = encoded.value()
        }

        this.consume(EdmElement.optional(OptionalOfNullable(result[0]) as Optional<EdmElement<*>>))
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>, size: Int): tree.maple.kendec.SequenceSerializer<E> {
        return SequenceSerializer<E>(elementEndec, ctx)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>, size: Int): tree.maple.kendec.MapSerializer<V> {
        return MapSerializer<V>(valueEndec, ctx)
    }

    override fun struct(): tree.maple.kendec.StructSerializer {
        return StructSerializer()
    }

    // ---
    private inner class SequenceSerializer<V> (
        private val elementEndec: Endec<V>,
        private val ctx: SerializationContext
    ) : tree.maple.kendec.SequenceSerializer<V> {
        private val result: MutableList<EdmElement<*>> = ArrayList()

        override fun element(element: V) {
            this@EdmSerializer.frame { encoded ->
                elementEndec.encode(ctx, this@EdmSerializer, element)
                this.result.add(encoded!!.require("sequence element")!!)
            }
        }

        override fun end() {
            this@EdmSerializer.consume(EdmElement.sequence(this.result))
        }
    }

    private inner class MapSerializer<V> (
        private val valueEndec: Endec<V>,
        private val ctx: SerializationContext
    ) : tree.maple.kendec.MapSerializer<V> {
        private val result: MutableMap<String, EdmElement<*>> = HashMap()

        override fun entry(key: String, value: V) {
            this@EdmSerializer.frame { encoded ->
                valueEndec.encode(ctx, this@EdmSerializer, value)
                this.result[key] = encoded!!.require("map value")!!
            }
        }

        override fun end() {
            this@EdmSerializer.consume(EdmElement.consumeMap(this.result))
        }
    }

    private inner class StructSerializer : tree.maple.kendec.StructSerializer{
        private val result: MutableMap<String, EdmElement<*>> = HashMap()

        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            value: F,
            mayOmit: Boolean
        ): tree.maple.kendec.StructSerializer {
            this@EdmSerializer.frame{ encoded ->
                endec.encode(ctx, this@EdmSerializer, value)
                val element = encoded!!.require("struct field")

                if (mayOmit && element == EdmElement.EMPTY) return@frame
                this.result[name] = element!!
            }

            return this
        }

        override fun end() {
            this@EdmSerializer.consume(EdmElement.consumeMap(this.result))
        }
    }

    companion object {
        fun of(): EdmSerializer {
            return EdmSerializer()
        }
    }
}
