package io.github.master_bw3.kendec.format.json

import io.github.master_bw3.kendec.*
import io.github.master_bw3.kendec.util.Optional
import io.github.master_bw3.kendec.util.RecursiveSerializer
import io.github.master_bw3.kendec.util.ifPresentOrElse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonSerializer protected constructor(private var prefix: JsonElement?) : RecursiveSerializer<JsonElement?>(null),
    SelfDescribedSerializer<JsonElement?> {
    override fun setupContext(ctx: SerializationContext): SerializationContext {
        return super<RecursiveSerializer>.setupContext(ctx).withAttributes(SerializationAttributes.HUMAN_READABLE)
    }

    // ---
    override fun writeByte(ctx: SerializationContext, value: Byte) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeShort(ctx: SerializationContext, value: Short) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeInt(ctx: SerializationContext, value: Int) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeLong(ctx: SerializationContext, value: Long) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeFloat(ctx: SerializationContext, value: Float) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeDouble(ctx: SerializationContext, value: Double) {
        this.consume(JsonPrimitive(value))
    }

    // ---
    override fun writeVarInt(ctx: SerializationContext, value: Int) {
        this.writeInt(ctx, value)
    }

    override fun writeVarLong(ctx: SerializationContext, value: Long) {
        this.writeLong(ctx, value)
    }

    // ---
    override fun writeBoolean(ctx: SerializationContext, value: Boolean) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeString(ctx: SerializationContext, value: String) {
        this.consume(JsonPrimitive(value))
    }

    override fun writeBytes(ctx: SerializationContext, bytes: ByteArray) {
        val result = ArrayList<JsonElement>(bytes.size)
        for (i in bytes.indices) {
            result.add(JsonPrimitive(bytes[i]))
        }

        this.consume(JsonArray(result.toList()))
    }

    override fun <V> writeOptional(ctx: SerializationContext, endec: Endec<V>, optional: Optional<V & Any>) {
        optional.ifPresentOrElse(
            { value: V -> endec.encode(ctx, this, value) },
            { this.consume(JsonNull) }
        )
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>, size: Int): io.github.master_bw3.kendec.SequenceSerializer<E> {
        return SequenceSerializer<E>(ctx, elementEndec, size)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>, size: Int): io.github.master_bw3.kendec.MapSerializer<V> {
        return MapSerializer<V>(ctx, valueEndec)
    }

    override fun struct(): StructSerializer {
        return MapSerializer<Any>(null, null)
    }

    // ---
    private inner class MapSerializer<V>(
        private val ctx: SerializationContext?,
        private val valueEndec: Endec<V>?
    ) : io.github.master_bw3.kendec.MapSerializer<V>, StructSerializer {
        private var result: MutableMap<String, JsonElement>? = null

        init {
            if (this@JsonSerializer.prefix != null) {
                if (prefix is JsonObject) {
                    this.result = (prefix as JsonObject).toMutableMap()
                    this@JsonSerializer.prefix = null
                } else {
                    throw IllegalStateException("Incompatible prefix of typex used for JSON map/struct")
                }
            } else {
                this.result = mutableMapOf()
            }
        }

        override fun entry(key: String, value: V) {
            this@JsonSerializer.frame { encoded: EncodedValue<JsonElement?> ->
                valueEndec?.encode(this.ctx!!, this@JsonSerializer, value)
                this.result!![key] = encoded.require("map value") ?: JsonNull
            }
        }

        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            value: F,
            mayOmit: Boolean
        ): StructSerializer {
            this@JsonSerializer.frame { encoded ->
                endec.encode(ctx, this@JsonSerializer, value)
                val element = encoded.require("struct field")

                if (mayOmit && element == JsonNull) return@frame
                this.result!![name] = element!!
            }

            return this
        }

        override fun end() {
            this@JsonSerializer.consume(result?.toMap()?.let(::JsonObject))
        }
    }

    private inner class SequenceSerializer<V>(
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        size: Int
    ) : io.github.master_bw3.kendec.SequenceSerializer<V> {
        private var result: MutableList<JsonElement>? = null

        init {
            if (this@JsonSerializer.prefix != null) {
                if (prefix is JsonArray) {
                    this.result = (prefix as JsonArray).toMutableList()
                    this@JsonSerializer.prefix = null
                } else {
                    throw IllegalStateException("Incompatible prefix used for JSON sequence")
                }
            } else {
                this.result = ArrayList(size)
            }
        }

        override fun element(element: V) {
            this@JsonSerializer.frame { encoded: EncodedValue<JsonElement?> ->
                valueEndec.encode(this.ctx, this@JsonSerializer, element)
                this.result!!.add(encoded.require("sequence element") ?: JsonNull)
            }
        }

        override fun end() {
            this@JsonSerializer.consume(result?.toList()?.let(::JsonArray))
        }
    }

    companion object {
        fun of(prefix: JsonElement? = null): JsonSerializer {
            return JsonSerializer(prefix)
        }
    }
}
