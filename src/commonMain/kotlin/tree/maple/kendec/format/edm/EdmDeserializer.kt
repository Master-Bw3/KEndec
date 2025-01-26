package tree.maple.kendec.format.edm

import tree.maple.kendec.*
import tree.maple.kendec.util.*

open class EdmDeserializer(serialized: EdmElement<*>) : RecursiveDeserializer<EdmElement<*>>(serialized),
    SelfDescribedDeserializer<EdmElement<*>> {
    // ---
    override fun readByte(ctx: SerializationContext): Byte {
        return value!!.cast()
    }

    override fun readShort(ctx: SerializationContext): Short {
        return value!!.cast()
    }

    override fun readInt(ctx: SerializationContext): Int {
        return value!!.cast()
    }

    override fun readLong(ctx: SerializationContext): Long {
        return value!!.cast()
    }

    // ---
    override fun readFloat(ctx: SerializationContext): Float {
        return value!!.cast()
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return value!!.cast()
    }

    // ---
    override fun readVarInt(ctx: SerializationContext): Int {
        return this.readInt(ctx)
    }

    override fun readVarLong(ctx: SerializationContext): Long {
        return this.readLong(ctx)
    }

    // ---
    override fun readBoolean(ctx: SerializationContext): Boolean {
        return value!!.cast()
    }

    override fun readString(ctx: SerializationContext): String {
        return value!!.cast()
    }

    override fun readBytes(ctx: SerializationContext): ByteArray {
        return value!!.cast()
    }

    override fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V & Any> {
        val optional = value!!.cast<Optional<EdmElement<*>>>()
        return if (optional.isPresent()) {
            frame(
                { optional.get() },
                {
                    OptionalOfNullable(
                        endec.decode(
                            ctx,
                            this
                        )
                    )
                }
            )
        } else {
            OptionalOfEmpty()
        }
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): Deserializer.Sequence<E> {
        return Sequence<E>(ctx, elementEndec, value!!.cast())
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>): Deserializer.Map<V> {
        return Map<V>(ctx, valueEndec, value!!.cast())
    }

    override fun struct(): Deserializer.Struct {
        return Struct(value!!.cast())
    }

    // ---
    override fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>) {
        this.visit(ctx, visitor, this.value)
    }

    private fun <S> visit(ctx: SerializationContext, visitor: Serializer<S>, value: EdmElement<*>?) {
        when (value!!.type()) {
            EdmElement.Type.I8 -> visitor.writeByte(ctx, value.cast())
            EdmElement.Type.I16 -> visitor.writeShort(ctx, value.cast())
            EdmElement.Type.I32 -> visitor.writeInt(ctx, value.cast())
            EdmElement.Type.I64 -> visitor.writeLong(ctx, value.cast())
            EdmElement.Type.F32 -> visitor.writeFloat(ctx, value.cast())
            EdmElement.Type.F64 -> visitor.writeDouble(ctx, value.cast())
            EdmElement.Type.BOOLEAN -> visitor.writeBoolean(ctx, value.cast())
            EdmElement.Type.STRING -> visitor.writeString(ctx, value.cast())
            EdmElement.Type.BYTES -> visitor.writeBytes(ctx, value.cast())
            EdmElement.Type.OPTIONAL -> visitor.writeOptional(
                ctx,
                Endec.of<EdmElement<*>?>(
                    { ctx1, serializer1, value1 -> this.visit(ctx1, serializer1, value1) },
                    { _, _ -> null }),
                value.cast()
            )

            EdmElement.Type.SEQUENCE -> {
                visitor.sequence<EdmElement<*>?>(
                    ctx,
                    Endec.of({ ctx1, visitor1, value1 ->
                        this.visit(
                            ctx1,
                            visitor1,
                            value1
                        )
                    }, { _, _ -> null }), value.cast<MutableList<EdmElement<*>?>>().size
                ).use { sequence ->
                    value.cast<List<EdmElement<*>>>().forEach { element: EdmElement<*>? -> sequence.element(element) }
                }
            }

            EdmElement.Type.MAP -> {
                visitor.map<EdmElement<*>?>(
                    ctx,
                    Endec.of({ ctx1, visitor1, value1 ->
                        this.visit(
                            ctx1,
                            visitor1,
                            value1
                        )
                    }, { _, _ -> null }), value.cast<MutableMap<String, EdmElement<*>?>>().size
                ).use { map ->
                    value.cast<kotlin.collections.Map<String, EdmElement<*>>>()
                        .forEach { (key: String, value: EdmElement<*>?) -> map.entry(key, value) }
                }
            }

            else -> throw Exception("unimplemented")
        }
    }

    // ---
    protected inner class Sequence<V> internal constructor(
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        elements: List<EdmElement<*>>
    ) : Deserializer.Sequence<V> {
        private val elements = elements.iterator()
        private val size = elements.size

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return elements.hasNext()
        }

        override fun next(): V {
            val element = elements.next()
            return this@EdmDeserializer.frame(
                { element },
                { valueEndec.decode(this.ctx, this@EdmDeserializer) }
            )
        }
    }

    private inner class Map<V> constructor(
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        entries: kotlin.collections.Map<String, EdmElement<*>>
    ) : Deserializer.Map<V> {
        private val entries = entries.entries.iterator()
        private val size = entries.size

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return entries.hasNext()
        }

        override fun next(): kotlin.collections.Map.Entry<String, V> {
            val entry = entries.next()
            return this@EdmDeserializer.frame(
                { entry.value },
                {
                    object : kotlin.collections.Map.Entry<String, V> {
                        override val key = entry.key
                        override val value = valueEndec.decode(this@Map.ctx, this@EdmDeserializer)
                    }
                }
            )
        }
    }

    private inner class Struct constructor(private val map: kotlin.collections.Map<String, EdmElement<*>>) :
        Deserializer.Struct {
        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            defaultValueFactory: (() -> F)?
        ): F {
            val element = map[name]
            if (element == null) {
                checkNotNull(defaultValueFactory) { "Field '$name' was missing from serialized data, but no default value was provided" }

                return defaultValueFactory()
            }
            return this@EdmDeserializer.frame(
                { element },
                { endec.decode(ctx, this@EdmDeserializer) }
            )
        }
    }

    companion object {
        fun of(serialized: EdmElement<*>): EdmDeserializer {
            return EdmDeserializer(serialized)
        }
    }
}
