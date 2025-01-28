@file:JsExport


package tree.maple.kendec

import tree.maple.kendec.impl.StructField
import kotlin.js.JsExport


/**
 * Marker and template interface for all endecs which serialize structs
 *
 *
 * Every such endec should extend this interface to profit from the implementation of [.mapCodec]
 * and composability which allows [Endec.dispatchedStruct] to work
 */
interface StructEndec<T> : Endec<T> {
    fun encodeStruct(ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: T)

    fun decodeStruct(ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer): T

    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
        serializer.struct().use { struct ->
            this.encodeStruct(ctx, serializer, struct, value)
        }
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
        return this.decodeStruct(ctx, deserializer, deserializer.struct())!!
    }

    fun <S> flatFieldOf(getter: (S) -> T): StructField<S, T> {
        return StructField.Flat(this, getter)
    }

    fun <M : T> flatInheritedFieldOf(): StructField<M, T> {
        return StructField.Flat(this) { m: M -> m }
    }

    override fun <R> xmap(to: (T) -> R, from: (R) -> T): StructEndec<R> {
        return structEndecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: R ->
                this@StructEndec.encodeStruct(
                    ctx,
                    serializer,
                    struct,
                    from(value)
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer ->
                to(
                    this@StructEndec.decodeStruct(ctx, deserializer, struct)!!
                )
            }
        )
    }

    override fun <R> xmapWithContext(
        to: (SerializationContext, T) -> R,
        from: (SerializationContext, R) -> T
    ): Endec<R> {
        return structEndecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: R ->
                this@StructEndec.encodeStruct(
                    ctx,
                    serializer,
                    struct,
                    from(ctx, value)
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer ->
                to(
                    ctx,
                    this@StructEndec.decodeStruct(ctx, deserializer, struct)!!
                )
            }
        )
    }

    fun structuredCatchErrors(decodeOnError: StructuredDecoderWithError<T>): StructEndec<T> {
        return structEndecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: T ->
                this.encodeStruct(
                    ctx,
                    serializer,
                    struct,
                    value
                )
            }
        ) { ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer ->
            try {
                return@structEndecOf deserializer.tryRead<T> { deserializer1 ->
                    this.decodeStruct(
                        ctx,
                        deserializer1,
                        struct
                    )!!
                }
            } catch (e: Exception) {
                return@structEndecOf decodeOnError.decodeStruct(ctx, deserializer, struct, e)
            }
        }
    }

    override fun validate(validator: (T) -> Unit): StructEndec<T> {
        return this.xmap({ t: T ->
            validator(t)
            t
        }, { t: T ->
            validator(t)
            t
        })
    }
}

fun interface StructuredEncoder<T> {
    fun encodeStruct(ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: T)
}

fun interface StructuredDecoder<T> {
    fun decodeStruct(ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer): T
}

fun interface StructuredDecoderWithError<T> {
    fun decodeStruct(
        ctx: SerializationContext?,
        serializer: Deserializer<*>?,
        struct: StructDeserializer?,
        exception: Exception?
    ): T
}

/**
 * Static constructor for [StructEndec] for use when base use of such is desired, it is recommended that
 * you use [StructEndecBuilder] as encoding and decoding of data must be kept
 * in the same order with same field names used across both encoding and decoding or issues may arise for
 * formats that are not Self Describing.
 */
fun <T> structEndecOf(encoder: StructuredEncoder<T>, decoder: StructuredDecoder<T>): StructEndec<T> {
    return object : StructEndec<T> {
        override fun encodeStruct(
            ctx: SerializationContext,
            serializer: Serializer<*>,
            struct: StructSerializer,
            value: T
        ) {
            encoder.encodeStruct(ctx, serializer, struct, value)
        }

        override fun decodeStruct(
            ctx: SerializationContext,
            deserializer: Deserializer<*>,
            struct: StructDeserializer
        ): T {
            return decoder.decodeStruct(ctx, deserializer, struct)
        }
    }
}