package io.github.master_bw3.kendec.format.json


import io.github.master_bw3.kendec.*
import io.github.master_bw3.kendec.util.Optional
import io.github.master_bw3.kendec.util.OptionalOfEmpty
import io.github.master_bw3.kendec.util.OptionalOfNullable
import io.github.master_bw3.kendec.util.RecursiveDeserializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.gciatto.kt.math.BigDecimal
import kotlin.collections.get
import kotlin.text.get
import kotlin.text.iterator

class JsonDeserializer protected constructor(serialized: JsonElement?) :
    RecursiveDeserializer<JsonElement?>(serialized), SelfDescribedDeserializer<JsonElement?> {
    override fun setupContext(ctx: SerializationContext): SerializationContext {
        return super<RecursiveDeserializer>.setupContext(ctx).withAttributes(SerializationAttributes.HUMAN_READABLE)
    }

    // ---
    override fun readByte(ctx: SerializationContext): Byte {
        return value!!.jsonPrimitive.content.toByte()
    }

    override fun readShort(ctx: SerializationContext): Short {
        return value!!.jsonPrimitive.content.toShort()
    }

    override fun readInt(ctx: SerializationContext): Int {
        return value!!.jsonPrimitive.content.toInt()
    }

    override fun readLong(ctx: SerializationContext): Long {
        return value!!.jsonPrimitive.content.toLong()
    }

    override fun readFloat(ctx: SerializationContext): Float {
        return value!!.jsonPrimitive.content.toFloat()
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return value!!.jsonPrimitive.content.toDouble()
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
        return value!!.jsonPrimitive.content.toBooleanStrict()
    }

    override fun readString(ctx: SerializationContext): String {
        return value!!.jsonPrimitive.content
    }

    override fun readBytes(ctx: SerializationContext): ByteArray {
        val array = value!!.jsonArray

        val result = ByteArray(array.size)
        for (i in array.indices) {
            result[i] = array[i].jsonPrimitive.content.toByte()
        }

        return result
    }

    override fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V & Any> {
        val value = this.value
        return if (value !is JsonNull) OptionalOfNullable(endec.decode(ctx, this))
        else OptionalOfEmpty()
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): io.github.master_bw3.kendec.SequenceDeserializer<E> {
        return SequenceDeserializer<E>(ctx, elementEndec, value as JsonArray)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>): io.github.master_bw3.kendec.MapDeserializer<V> {
        return MapDeserializer<V>(ctx, valueEndec, (value as JsonObject))
    }

    override fun struct(): io.github.master_bw3.kendec.StructDeserializer {
        return StructDeserializer(value as JsonObject)
    }

    // ---
    override fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>) {
        this.decodeValue(ctx, visitor, this.value!!)
    }

    private fun <S> decodeValue(ctx: SerializationContext, visitor: Serializer<S>, element: JsonElement?) {
        if (element == null || (element is JsonNull)) {
            visitor.writeOptional(ctx, JsonEndec.INSTANCE, Optional.empty())
        } else if (element is JsonPrimitive) {
            if (element.isString) {
                visitor.writeString(ctx, element.content)
            } else if (element.content.toBooleanStrictOrNull() != null) {
                visitor.writeBoolean(ctx, element.content.toBooleanStrict())
            } else {
                val value =  BigDecimal.of(element.content)


                try {
                    val asLong = value.toLongExact()

                    if (asLong.toByte().toLong() == asLong) {
                        visitor.writeByte(ctx, element.content.toByte())
                    } else if (asLong.toShort().toLong() == asLong) {
                        visitor.writeShort(ctx, element.content.toShort())
                    } else if (asLong.toInt().toLong() == asLong) {
                        visitor.writeInt(ctx, element.content.toInt())
                    } else {
                        visitor.writeLong(ctx, asLong)
                    }
                } catch (bruh: ArithmeticException) {
                    val asDouble = value.toDouble()

                    if (asDouble.toFloat().toDouble() == asDouble) {
                        visitor.writeFloat(ctx, element.content.toFloat())
                    } else {
                        visitor.writeDouble(ctx, asDouble)
                    }
                }
            }
        } else if (element is JsonArray) {
            visitor.sequence(
                ctx,
                endecOf<JsonElement?>({ ctx, visitor, element ->
                    this.decodeValue(
                        ctx,
                        visitor,
                        element
                    )
                },
                { ctx1, deserializer -> null }), element.size()).use { sequence ->
                element.forEach(
                    { element: JsonElement? -> sequence.element(element) })
            }
        } else if (element is JsonObject) {
            visitor.map(
                ctx,
                endecOf<JsonElement?>( { ctx, visitor, element ->
                    this.decodeValue(
                        ctx,
                        visitor,
                        element
                    )
                },
                { ctx1, deserializer -> null }), element.size()).use { map ->
                element.asMap().forEach { (key: String, value: JsonElement?) -> map.entry(key, value) }
            }
        } else {
            throw IllegalArgumentException("Non-standard, unrecognized JsonElement implementation cannot be decoded")
        }
    }

    // ---
    private inner class SequenceDeserializer<V> (
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        elements: JsonArray
    ) : io.github.master_bw3.kendec.SequenceDeserializer<V> {
        private val elements: Iterator<JsonElement> = elements.iterator()
        private val size = elements.count()

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return elements.hasNext()
        }

        override fun next(): V {
            val element = elements.next()
            return this@JsonDeserializer.frame(
                { element },
                { valueEndec.decode(this.ctx, this@JsonDeserializer) }
            )
        }
    }

    private inner class MapDeserializer<V> (
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        entries: JsonObject
    ) : io.github.master_bw3.kendec.MapDeserializer<V> {
        private val entries: Iterator<Map.Entry<String, JsonElement>> =
            entries.entries.iterator()
        private val size = entries.count()

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return entries.hasNext()
        }

        override fun next(): Map.Entry<String, V> {
            val entry = entries.next()
            return this@JsonDeserializer.frame(
                { entry.value },
                { entry.key to valueEndec.decode(this.ctx, this@JsonDeserializer) }
            )
        }
    }

    private inner class StructDeserializer (private val `object`: JsonObject) : io.github.master_bw3.kendec.StructDeserializer {
        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            defaultValueFactory: (() -> F)?
        ): F {
            val element = `object`[name]
            if (element == null) {
                checkNotNull(defaultValueFactory) { "Field '$name' was missing from serialized data, but no default value was provided" }

                return defaultValueFactory()
            }
            return this@JsonDeserializer.frame(
                { element },
                { endec.decode(ctx, this@JsonDeserializer) }
            )
        }
    }

    companion object {
        fun of(serialized: JsonElement?): JsonDeserializer {
            return JsonDeserializer(serialized)
        }
    }
}
