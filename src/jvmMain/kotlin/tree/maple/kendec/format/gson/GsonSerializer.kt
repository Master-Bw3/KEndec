package tree.maple.kendec.format.gson

import com.google.gson.*
import tree.maple.kendec.*
import tree.maple.kendec.util.RecursiveSerializer
import tree.maple.kendec.util.RecursiveSerializer.FrameAction
import java.util.*

class GsonSerializer protected constructor(private var prefix: JsonElement?) : RecursiveSerializer<JsonElement?>(null),
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
        val result = JsonArray(bytes.size)
        for (i in bytes.indices) {
            result.add(bytes[i])
        }

        this.consume(result)
    }

    override fun <V> writeOptional(ctx: SerializationContext, endec: Endec<V>, optional: Optional<V & Any>) {
        optional.ifPresentOrElse(
            { value: V -> endec.encode(ctx, this, value) },
            { this.consume(JsonNull.INSTANCE) }
        )
    }

    // ---
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>, size: Int): Serializer.Sequence<E> {
        return Sequence<E>(ctx, elementEndec, size)
    }

    override fun <V> map(ctx: SerializationContext, valueEndec: Endec<V>, size: Int): Serializer.Map<V> {
        return Map<V>(ctx, valueEndec)
    }

    override fun struct(): Serializer.Struct {
        return Map<Any>(null, null)
    }

    // ---
    private inner class Map<V> constructor(
        private val ctx: SerializationContext?,
        private val valueEndec: Endec<V>?
    ) : Serializer.Map<V>, Serializer.Struct {
        private var result: JsonObject? = null

        init {
            if (this@GsonSerializer.prefix != null) {
                if (prefix is JsonObject) {
                    this.result = prefix as JsonObject
                    this@GsonSerializer.prefix = null
                } else {
                    throw IllegalStateException("Incompatible prefix of type " + prefix?.javaClass?.simpleName + " used for JSON map/struct")
                }
            } else {
                this.result = JsonObject()
            }
        }

        override fun entry(key: String, value: V) {
            this@GsonSerializer.frame { encoded: EncodedValue<JsonElement?> ->
                valueEndec?.encode(this.ctx!!, this@GsonSerializer, value)
                this.result!!.add(key, encoded.require("map value"))
            }
        }

        override fun <F> field(
            name: String,
            ctx: SerializationContext,
            endec: Endec<F>,
            value: F,
            mayOmit: Boolean
        ): Serializer.Struct {
            this@GsonSerializer.frame { encoded ->
                endec.encode(ctx, this@GsonSerializer, value)
                val element = encoded.require("struct field")

                if (mayOmit && element == JsonNull.INSTANCE) return@frame
                this.result!!.add(name, element)
            }

            return this
        }

        override fun end() {
            this@GsonSerializer.consume(result)
        }
    }

    private inner class Sequence<V> constructor(
        private val ctx: SerializationContext,
        private val valueEndec: Endec<V>,
        size: Int
    ) : Serializer.Sequence<V> {
        private var result: JsonArray? = null

        init {
            if (this@GsonSerializer.prefix != null) {
                if (prefix is JsonArray) {
                    this.result = prefix as JsonArray
                    this@GsonSerializer.prefix = null
                } else {
                    throw IllegalStateException("Incompatible prefix of type " + prefix?.javaClass?.simpleName + " used for JSON sequence")
                }
            } else {
                this.result = JsonArray(size)
            }
        }

        override fun element(element: V) {
            this@GsonSerializer.frame { encoded: EncodedValue<JsonElement?> ->
                valueEndec.encode(this.ctx, this@GsonSerializer, element)
                this.result!!.add(encoded.require("sequence element"))
            }
        }

        override fun end() {
            this@GsonSerializer.consume(result)
        }
    }

    companion object {
        @JvmOverloads
        fun of(prefix: JsonElement? = null): GsonSerializer {
            return GsonSerializer(prefix)
        }
    }
}
