package tree.maple.kendec.format.gson

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import tree.maple.kendec.*
import tree.maple.kendec.util.RecursiveDeserializer
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.jvm.functions.Function0

class GsonDeserializer protected constructor(serialized: JsonElement?) :
    RecursiveDeserializer<JsonElement?>(serialized), SelfDescribedDeserializer<JsonElement?> {
    override fun setupContext(ctx: SerializationContext): SerializationContext {
        return super<RecursiveDeserializer>.setupContext(ctx).withAttributes(SerializationAttributes.HUMAN_READABLE)
    }

    // ---
    override fun readByte(ctx: SerializationContext): Byte {
        return value!!.asByte
    }

    override fun readShort(ctx: SerializationContext): Short {
        return value!!.asShort
    }

    override fun readInt(ctx: SerializationContext): Int {
        return value!!.asInt
    }

    override fun readLong(ctx: SerializationContext): Long {
        return value!!.asLong
    }

    override fun readFloat(ctx: SerializationContext): Float {
        return value!!.asFloat
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return value!!.asDouble
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
        return value!!.asBoolean
    }

    override fun readString(ctx: SerializationContext): String {
        return value!!.asString
    }

    override fun readBytes(ctx: SerializationContext): ByteArray {
        val array = value!!.asJsonArray.asList()

        val result = ByteArray(array.size)
        for (i in array.indices) {
            result[i] = array[i].asByte
        }

        return result
    }

    override fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V & Any> {
        val value = this.value
        return if (!value!!.isJsonNull
        ) Optional.ofNullable(endec.decode(ctx, this)) as Optional<V & Any>
        else Optional.empty<V>()
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): Deserializer.Sequence<E> {
        return Sequence<E>(ctx, elementEndec, value as JsonArray)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>): Deserializer.Map<V> {
        return Map<V>(ctx, valueEndec, (value as JsonObject))
    }

    override fun struct(): Deserializer.Struct {
        return Struct(value as JsonObject)
    }

    // ---
    override fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>) {
        this.decodeValue(ctx, visitor, this.value!!)
    }

    private fun <S> decodeValue(ctx: SerializationContext, visitor: Serializer<S>, element: JsonElement?) {
        if (element == null || element.isJsonNull) {
            visitor.writeOptional(ctx, GsonEndec.INSTANCE, Optional.empty())
        } else if (element is JsonPrimitive) {
            if (element.isString) {
                visitor.writeString(ctx, element.getAsString())
            } else if (element.isBoolean) {
                visitor.writeBoolean(ctx, element.getAsBoolean())
            } else {
                val value = element.getAsBigDecimal()

                try {
                    val asLong = value.longValueExact()

                    if (asLong.toByte().toLong() == asLong) {
                        visitor.writeByte(ctx, element.getAsByte())
                    } else if (asLong.toShort().toLong() == asLong) {
                        visitor.writeShort(ctx, element.getAsShort())
                    } else if (asLong.toInt().toLong() == asLong) {
                        visitor.writeInt(ctx, element.getAsInt())
                    } else {
                        visitor.writeLong(ctx, asLong)
                    }
                } catch (bruh: ArithmeticException) {
                    val asDouble = value.toDouble()

                    if (asDouble.toFloat().toDouble() == asDouble) {
                        visitor.writeFloat(ctx, element.getAsFloat())
                    } else {
                        visitor.writeDouble(ctx, asDouble)
                    }
                }
            }
        } else if (element is JsonArray) {
            visitor.sequence(
                ctx,
                Endec.of<JsonElement?>({ ctx, visitor, element ->
                    this.decodeValue(
                        ctx,
                        visitor,
                        element
                    )
                },
                { ctx1, deserializer -> null }), element.size()).use { sequence ->
                element.forEach(
                    Consumer { element: JsonElement? -> sequence.element(element) })
            }
        } else if (element is JsonObject) {
            visitor.map(
                ctx,
                Endec.of<JsonElement?>( { ctx, visitor, element ->
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
    private inner class Sequence<V> (
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        elements: JsonArray
    ) : Deserializer.Sequence<V> {
        private val elements: Iterator<JsonElement> = elements.iterator()
        private val size = elements.size()

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return elements.hasNext()
        }

        override fun next(): V {
            val element = elements.next()
            return this@GsonDeserializer.frame(
                { element },
                { valueEndec.decode(this.ctx, this@GsonDeserializer) }
            )
        }
    }

    private inner class Map<V> (
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        entries: JsonObject
    ) : Deserializer.Map<V> {
        private val entries: Iterator<kotlin.collections.Map.Entry<String, JsonElement>> =
            entries.entrySet().iterator()
        private val size = entries.size()

        override fun estimatedSize(): Int {
            return this.size
        }

        override fun hasNext(): Boolean {
            return entries.hasNext()
        }

        override fun next(): kotlin.collections.Map.Entry<String, V> {
            val entry = entries.next()
            return this@GsonDeserializer.frame(
                { entry.value },
                {
                    java.util.Map.entry<String, V?>(
                        entry.key,
                        valueEndec.decode(this.ctx, this@GsonDeserializer)
                    )
                }
            )
        }
    }

    private inner class Struct (private val `object`: JsonObject) : Deserializer.Struct {
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
            return this@GsonDeserializer.frame(
                { element },
                { endec.decode(ctx, this@GsonDeserializer) }
            )
        }
    }

    companion object {
        fun of(serialized: JsonElement?): GsonDeserializer {
            return GsonDeserializer(serialized)
        }
    }
}
