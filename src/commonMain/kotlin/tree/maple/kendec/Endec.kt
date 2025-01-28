@file:JsExport

package tree.maple.kendec

import tree.maple.kendec.impl.*
import tree.maple.kendec.util.*
import tree.maple.kendec.util.getEnumConstants
import kotlin.js.ExperimentalJsCollectionsApi
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.js.JsStatic
import kotlin.reflect.KClass

/**
 * A combined **en**coder and **dec**oder for values of type `T`.
 *
 *
 * To convert between single instances of `T` and their serialized form,
 * use [.encodeFully] and [.decodeFully]
 */
interface Endec<T> {
    /**
     * Write all data required to reconstruct `value` into `serializer`
     */
    fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T)

    /**
     * Decode the data specified by [.encode] and reconstruct
     * the corresponding instance of `T`.
     *
     *
     * Endecs which intend to handle deserialization failure by decoding a different
     * structure on error, must wrap their initial reads in a call to [Deserializer.tryRead]
     * to ensure that deserializer state is restored for the subsequent attempt
     */
    fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T

    // ---
    /**
     * Create a new serializer with result type `E`, call [.encode]
     * once for the provided `value` and return the serializer's [result][Serializer.result]
     */
    @JsName("encodeFullyWithCtx")
    fun <E> encodeFully(ctx: SerializationContext, serializerConstructor: () -> Serializer<E>, value: T): E {
        val serializer = serializerConstructor()
        this.encode(serializer.setupContext(ctx), serializer, value)

        return serializer.result()
    }

    fun <E> encodeFully(serializerConstructor: () -> Serializer<E>, value: T): E {
        return encodeFully(SerializationContext.empty(), serializerConstructor, value)
    }

    /**
     * Create a new deserializer by calling `deserializerConstructor` with `value`
     * and return the result of [.decode]
     */
    @JsName("decodeFullyWithCtx")
    fun <E> decodeFully(
        ctx: SerializationContext,
        deserializerConstructor: (E) -> Deserializer<E>,
        value: E
    ): T {
        val deserializer = deserializerConstructor(value)
        return this.decode(deserializer.setupContext(ctx), deserializer)
    }

    fun <E> decodeFully(deserializerConstructor: (E) -> Deserializer<E>, value: E): T {
        return decodeFully(SerializationContext.empty(), deserializerConstructor, value)
    }

    // --- Serializer compound types ---
    /**
     * Create a new endec which serializes a list of elements
     * serialized using this endec
     */
    fun listOf(): Endec<List<T>> {
        return endecOf({ ctx: SerializationContext, serializer: Serializer<*>, list: List<T> ->
            serializer.sequence(ctx, this, list.size).use { sequence ->
                list.forEach { element: T -> sequence.element(element) }
            }
        }, { ctx: SerializationContext, deserializer: Deserializer<*> ->
            val sequenceState = deserializer.sequence(ctx, this)
            val list = ArrayList<T>(sequenceState.estimatedSize())
            sequenceState.forEach { e: T -> list.add(e) }
            list.toList()
        })
    }

    /**
     * Create a new endec which serializes a map from string
     * keys to values serialized using this endec
     */
    fun mapOf(): Endec<Map<String, T>> {
        return endecOf({ ctx: SerializationContext, serializer: Serializer<*>, map: Map<String, T> ->
            serializer.map(ctx, this, map.size).use { mapState ->
                map.forEach { (key: String, value: T) -> mapState.entry(key, value) }
            }
        }, { ctx: SerializationContext, deserializer: Deserializer<*> ->
            val mapState = deserializer.map(ctx, this)
            val map = HashMap<String, T>(mapState.estimatedSize())
            mapState.forEach { entry: Map.Entry<String, T> -> map[entry.key] = entry.value }
            map
        })
    }

    /**
     * Create a new endec which serializes an optional value
     * serialized using this endec
     */
    fun optionalOf(): Endec<Optional<T & Any>> {
        return endecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, value: Optional<T & Any> ->
                serializer.writeOptional(
                    ctx,
                    this,
                    value
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*> ->
                deserializer.readOptional(
                    ctx,
                    this
                )
            }
        )
    }

    // --- Endec composition ---
    /**
     * Create a new endec which converts between instances of `T` and `R`
     * using `to` and `from` before encoding / after decoding
     */
    fun <R> xmap(to: (T) -> R, from: (R) -> T): Endec<R> {
        return endecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, value: R ->
                this@Endec.encode(
                    ctx,
                    serializer,
                    from(value)
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*> ->
                to(
                    this@Endec.decode(
                        ctx,
                        deserializer
                    )
                )
            }
        )
    }

    /**
     * Create a new endec which converts between instances of `T` and `R`
     * using `to` and `from` before encoding / after decoding, optionally using
     * the current [serialization context][SerializationContext]
     */
    fun <R> xmapWithContext(
        to: (SerializationContext, T) -> R,
        from: (SerializationContext, R) -> T
    ): Endec<R> {
        return endecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, value: R ->
                this@Endec.encode(
                    ctx,
                    serializer,
                    from(ctx, value)
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*> ->
                to(
                    ctx,
                    this@Endec.decode(ctx, deserializer)
                )
            }
        )
    }

    /**
     * Create a new endec which runs `validator` (giving it the chance to throw on
     * an invalid value) before encoding / after decoding
     */
    fun validate(validator: (T) -> Unit): Endec<T> {
        return this.xmap({ t: T ->
            validator(t)
            t
        }, { t: T ->
            validator(t)
            t
        })
    }

    /**
     * Create a new endec which, if decoding using this endec's [.decode] fails,
     * instead tries to decode using `decodeOnError`
     */
    fun catchErrors(decodeOnError: DecoderWithError<T>): Endec<T> {
        return endecOf<T>({ ctx: SerializationContext, serializer: Serializer<*>, value: T ->
            this.encode(
                ctx,
                serializer,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> ->
            try {
                return@endecOf deserializer.tryRead<T> { deserializer1 ->
                    this.decode(
                        ctx,
                        deserializer1
                    )
                }
            } catch (e: Exception) {
                return@endecOf decodeOnError.decode(ctx, deserializer, e)
            }
        })
    }

    /**
     * Create a new endec which serializes a set of elements
     * serialized using this endec as an xmapped list
     */
    fun setOf(): Endec<Set<T>> {
        return this.listOf().xmap({ c: List<T> -> HashSet(c) }, { c: Set<T> -> ArrayList(c) })
    }

    /**
     * Create a new endec by wrapping [.optionalOf] and mapping between
     * present optional &lt;-&gt; value and empty optional &lt;-&gt; null
     */
    fun nullableOf(): Endec<T?> {
        return optionalOf().xmap(
            { o -> o.orElse(null) },
            { value -> OptionalOfNullable(value) })
    }

    // --- Conversion ---
    /**
     * Create a new keyed endec which (de)serializes the entry
     * with key `key` into/from a [MapCarrier],
     * decoding to `defaultValue` if the map does not contain such an entry
     *
     *
     * If `T` is of a mutable type, you almost always want to use [.keyed] instead
     */
    fun keyed(key: String, defaultValue: T): KeyedEndec<T> {
        return KeyedEndec(key, this, { defaultValue })
    }

    /**
     * Create a new keyed endec which (de)serializes the entry
     * with key `key` into/from a [MapCarrier],
     * decoding to the result of invoking `defaultValueFactory` if the map does not contain such an entry
     *
     *
     * If `T` is of an immutable type, you almost always want to use [.keyed] instead
     */
    @JsName("keyedFromSupplier")
    fun keyed(key: String, defaultValueFactory: () -> T): KeyedEndec<T> {
        return KeyedEndec(key, this, defaultValueFactory)
    }

    // ---
    fun structOf(name: String): StructEndec<T> {
        return structEndecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: T ->
                struct.field(
                    name,
                    ctx,
                    this@Endec,
                    value
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer ->
                struct.field(
                    name,
                    ctx,
                    this@Endec,
                    null
                )
            })
    }

    fun recursiveStruct(builderFunc: (StructEndec<T>) -> StructEndec<T>): StructEndec<T> {
        return RecursiveStructEndec(builderFunc)
    }

    fun <S> fieldOf(name: String, getter: (S) -> T): StructField<S, T> {
        return StructField(name, this, getter)
    }

    fun <S> optionalFieldOf(name: String, getter: (S) -> T?, defaultValue: () -> T & Any): StructField<S, T?> {
        return StructField(
            name,
            optionalOf().xmap(
                { optional -> optional.orElseGet(defaultValue) },
                { value -> OptionalOfNullable(value) }),
            { key -> key?.let(getter) } ,
            defaultValue
        )
    }
}


typealias Encoder<T> = (ctx: SerializationContext, serializer: Serializer<*>, value: T) -> Unit

typealias Decoder<T> = (ctx: SerializationContext, deserializer: Deserializer<*>) -> T

fun interface DecoderWithError<T> {
    fun decode(ctx: SerializationContext, serializer: Deserializer<*>, exception: Exception): T
}

// --- Constructors ---
fun <T> endecOf(encoder: Encoder<T>, decoder: Decoder<T>): Endec<T> {
    return object : Endec<T> {
        override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
            encoder(ctx, serializer, value)
        }

        override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
            return decoder(ctx, deserializer)
        }
    }
}

fun <T> recursiveEndecOf(builderFunc: (Endec<T>) -> Endec<T>): Endec<T> {
    return RecursiveEndec(builderFunc)
}

fun <T> unitEndecOf(instance: T): StructEndec<T> {
    return unitEndecOf { instance }
}

@JsName("unitEndecFromSupplier")
fun <T> unitEndecOf(instance: () -> T): StructEndec<T> {
    return structEndecOf(
        { ctx: SerializationContext, serializer: Serializer<*>, struct: StructSerializer, value: T -> },
        { ctx: SerializationContext, deserializer: Deserializer<*>, struct: StructDeserializer -> instance() })
}

/**
 * Create a new endec which serializes a map from keys serialized using
 * `keyEndec` to values serialized using `valueEndec`.
 *
 *
 * Due to the endec data model only natively supporting maps
 * with string keys, the resulting endec's serialized representation
 * is a list of key-value pairs
 */
fun <K, V> mapEndecOf(keyEndec: Endec<K>, valueEndec: Endec<V>): Endec<Map<K, V>> {
    return StructEndecBuilder.of(
        keyEndec.fieldOf("k") { it.first },
        valueEndec.fieldOf("v") { it.second }
    ) { k: K, v: V -> k to v }.listOf().xmap({ entries: List<Pair<K, V>> ->
        entries.filterNotNull().toMap()
    }, { kvMap: Map<K, V> -> kvMap.entries.toList().map { it.key to it.value } })
}

/**
 * Create a new endec which serializes a map from keys encoded as strings using
 * `keyToString` and decoded using `stringToKey` to values serialized
 * using `valueEndec`
 */
@JsName("stringMapEndecOf")
fun <K, V> mapEndecOf(
    keyToString: (K) -> String,
    stringToKey: (String) -> K,
    valueEndec: Endec<V>
): Endec<Map<K, V>> {
    return endecOf({ ctx: SerializationContext, serializer: Serializer<*>, map: Map<K, V> ->
        serializer.map(ctx, valueEndec, map.size).use { mapState ->
            map.forEach { (k: K, v: V) -> mapState.entry(keyToString(k), v) }
        }
    }, { ctx: SerializationContext, deserializer: Deserializer<*> ->
        val mapState = deserializer.map(ctx, valueEndec)
        val map = HashMap<K, V>(mapState.estimatedSize())
        mapState.forEach { entry: Map.Entry<String, V> ->
            map[stringToKey(entry.key)] = entry.value
        }
        map
    })
}

/**
 * Create a new endec which serializes the enum constants of `enumClass`
 *
 *
 * In a human-readable format, the endec serializes to the [constant&#39;s name][Enum.name],
 * and to its [ordinal][Enum.ordinal] otherwise
 */
fun <E : Enum<E>> enumEndecOf(enumClass: KClass<E>): Endec<E> {
    return ifAttr<E>(
        SerializationAttributes.HUMAN_READABLE,
        PrimitiveEndecs.STRING.xmap<E>({ name: String ->
            getEnumConstants(enumClass).toList().first { e: E -> e.name == name }
        }, { obj: E -> obj.name })
    ).orElse(
        PrimitiveEndecs.VAR_INT.xmap({ ordinal: Int? -> getEnumConstants(enumClass)[ordinal!!] }, { obj: E -> obj.ordinal })
    )
}

// ---
/**
 * Create a new struct-dispatch endec which serializes variants of the struct `T`
 *
 *
 * To do this, it inserts an additional field given by `variantKey` into the beginning of the
 * struct and writes the variant identifier obtained from `instanceToVariant` into it
 * using `variantEndec`. When decoding, this variant identifier is read and the rest
 * of the struct decoded with the endec obtained from `variantToEndec`
 *
 *
 * For example, assume there is some interface like this
 * <pre>`public interface Herbert {
 * Identifier id();
 * ... more functionality here
 * }
`</pre> *
 *
 * which is implemented by `Harald` and `Albrecht`, whose endecs we have
 * stored in a map:
 * <pre>`public final class Harald implements Herbert {
 * public static final StructEndec<Harald> = StructEndecBuilder.of(...);
 *
 * private final int haraldOMeter;
 * ...
 * }
 *
 * public final class Albrecht implements Herbert {
 * public static final StructEndec<Harald> = StructEndecBuilder.of(...);
 *
 * private final List<String> dadJokes;
 * ...
 * }
 *
 * public static final Map<Identifier, StructEndec<? extends Herbert>> HERBERT_REGISTRY = Map.of(
 * new Identifier("herbert", "harald"), Harald.ENDEC,
 * new Identifier("herbert", "albrecht"), Albrecht.ENDEC
 * );
`</pre> *
 *
 * We could then create an endec capable of serializing either `Harald` or `Albrecht` as follows:
 * <pre>`Endec.dispatchedStruct(HERBERT_REGISTRY::get, Herbert::id, BuiltInEndecs.IDENTIFIER, "type")
`</pre> *
 *
 * If we now encode an instance of `Albrecht` to JSON using this endec, we'll get the following result:
 * <pre>`{
 * "type": "herbert:albrecht",
 * "dad_jokes": [
 * "What does a sprinter eat before a race? Nothing, they fast!",
 * "Why don't eggs tell jokes? They'd crack each other up."
 * ]
 * }
`</pre> *
 *
 * And similarly, the following data could be used for decoding an instance of `Harald`:
 * <pre>`{
 * "type": "herbert:harald",
 * "harald_o_meter": 69
 * }
`</pre> *
 */
fun <T, K> dispatchedStructEndecOf(
    variantToEndec: (K?) -> StructEndec<out T>,
    instanceToVariant: (T) -> K,
    variantEndec: Endec<K>,
    variantKey: String = "type"
): StructEndec<T> {
    return object : StructEndec<T> {
        override fun encodeStruct(
            ctx: SerializationContext,
            serializer: Serializer<*>,
            struct: StructSerializer,
            value: T
        ) {
            val variant = instanceToVariant(value)
            struct.field(variantKey, ctx, variantEndec, variant)

            (variantToEndec(variant) as StructEndec<T?>).encodeStruct(ctx, serializer, struct, value)
        }

        override fun decodeStruct(
            ctx: SerializationContext,
            deserializer: Deserializer<*>,
            struct: StructDeserializer
        ): T {
            val variant = struct.field(variantKey, ctx, variantEndec, null)
            return variantToEndec(variant).decodeStruct(ctx, deserializer, struct)
        }
    }
}

/**
 * Create a new dispatch endec which serializes variants of `T`
 *
 *
 * Such an endec is conceptually similar to a struct-dispatch one created through [.dispatchedStruct]
 * (check the documentation on that function for a complete usage example), but because this family of endecs does not
 * require `T` to be a struct, the variant identifier field cannot be merged with the rest and is encoded separately
 */
fun <T, K> dispatchedEndecOf(
    variantToEndec: (K?) -> Endec<out T>,
    instanceToVariant: (T) -> K,
    variantEndec: Endec<K>
): StructEndec<T> {
    return object : StructEndec<T> {
        override fun encodeStruct(
            ctx: SerializationContext,
            serializer: Serializer<*>,
            struct: StructSerializer,
            value: T
        ) {
            val variant = instanceToVariant(value)
            struct.field("variant", ctx, variantEndec, variant)

            struct.field("instance", ctx, (variantToEndec(variant) as Endec<T>), value)
        }

        override fun decodeStruct(
            ctx: SerializationContext,
            deserializer: Deserializer<*>,
            struct: StructDeserializer
        ): T {
            val variant = struct.field("variant", ctx, variantEndec, null)
            return struct.field("instance", ctx, variantToEndec(variant), null)
        }
    }
}

// ---
fun <T> ifAttr(attribute: SerializationAttribute, endec: Endec<T>): AttributeEndecBuilder<T> {
    return AttributeEndecBuilder(endec, attribute)
}

fun <N> Endec<N>.clampedMax(max: N): Endec<N> where N : Number, N : Comparable<N> {
    return this.clamped<N>(null, max)
}

fun <N> Endec<N>.rangedMax(
    max: N,
    throwError: Boolean
): Endec<N> where N : Number, N : Comparable<N> {
    return this.ranged(null, max, throwError)
}

fun <N> Endec<N>.clampedMin(min: N): Endec<N> where N : Number, N : Comparable<N> {
    return this.clamped(min, null)
}

fun <N> Endec<N>.rangedMin(
    min: N,
    throwError: Boolean
): Endec<N> where N : Number, N : Comparable<N> {
    return this.ranged(min, null, throwError)
}

fun <N>  Endec<N>.clamped(min: N?, max: N?): Endec<N> where N : Number, N : Comparable<N> {
    return this.ranged(min, max, false)
}

fun <N> Endec<N>.ranged(
    min: N?,
    max: N?,
    throwError: Boolean
): Endec<N> where N : Number, N : Comparable<N> {
    val errorChecker: (N) -> N = Function@{ n: N ->
        // 1st check if the given min value exist and then compare similar to: [n < min]
        // 2nd check if the given min value exist and then compare similar to: [n > max]
        if (min != null && n < min) {
            if (throwError) throw RangeNumberException(n, min, max)
            return@Function min
        } else if (max != null && n > max) {
            if (throwError) throw RangeNumberException(n, min, max)
            return@Function max
        }
        n
    }

    return this.xmap({ t: N -> errorChecker(t) }, { t: N -> errorChecker(t) })
}

// --- Serializer Primitives ---
class PrimitiveEndecs {
    companion object {
        @JsStatic
        val VOID: Endec<Unit> = endecOf(
            { ctx: SerializationContext, serializer: Serializer<*>, unused: Unit -> },
            { ctx: SerializationContext, deserializer: Deserializer<*> -> null })

        @JsStatic
        val BOOLEAN: Endec<Boolean> =
            endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Boolean ->
                serializer.writeBoolean(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readBoolean(ctx) })

        @JsStatic
        val BYTE: Endec<Byte> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Byte ->
            serializer.writeByte(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readByte(ctx) })

        @JsStatic
        val SHORT: Endec<Short> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Short ->
            serializer.writeShort(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readShort(ctx) })

        @JsStatic
        val INT: Endec<Int> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Int ->
            serializer.writeInt(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readInt(ctx) })

        @JsStatic
        val VAR_INT: Endec<Int> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Int ->
            serializer.writeVarInt(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readVarInt(ctx) })

        @JsStatic
        val LONG: Endec<Long> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Long ->
            serializer.writeLong(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readLong(ctx) })

        @JsStatic
        val VAR_LONG: Endec<Long> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Long ->
            serializer.writeVarLong(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readVarLong(ctx) })

        @JsStatic
        val FLOAT: Endec<Float> = endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Float ->
            serializer.writeFloat(
                ctx,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readFloat(ctx) })

        @JsStatic
        val DOUBLE: Endec<Double> =
            endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: Double ->
                serializer.writeDouble(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readDouble(ctx) })

        @JsStatic
        val STRING: Endec<String> =
            endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: String ->
                serializer.writeString(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readString(ctx) })

        @JsStatic
        val BYTES: Endec<ByteArray> =
            endecOf({ ctx: SerializationContext, serializer: Serializer<*>, value: ByteArray ->
                serializer.writeBytes(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readBytes(ctx) })
    }
}

