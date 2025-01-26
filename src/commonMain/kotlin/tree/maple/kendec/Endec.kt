package tree.maple.kendec

import tree.maple.kendec.Endec.Decoder
import tree.maple.kendec.impl.*
import tree.maple.kendec.util.*
import tree.maple.kendec.util.getEnumConstants
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
        return of({ ctx: SerializationContext, serializer: Serializer<*>, list: List<T> ->
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
        return of({ ctx: SerializationContext, serializer: Serializer<*>, map: Map<String, T> ->
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
    fun optionalOf(): Endec<Optional<T>> {
        return of(
            { ctx: SerializationContext, serializer: Serializer<*>, value: Optional<T> ->
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
        return of(
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
        return of(
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
        return of<T>({ ctx: SerializationContext, serializer: Serializer<*>, value: T ->
            this.encode(
                ctx,
                serializer,
                value
            )
        }, { ctx: SerializationContext, deserializer: Deserializer<*> ->
            try {
                return@of deserializer.tryRead<T> { deserializer1 ->
                    this.decode(
                        ctx,
                        deserializer1
                    )
                }
            } catch (e: Exception) {
                return@of decodeOnError.decode(ctx, deserializer, e)
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
            { value -> OptionalOfNullable(value) as Optional<T> })
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
    fun keyed(key: String, defaultValueFactory: () -> T): KeyedEndec<T> {
        return KeyedEndec(key, this, defaultValueFactory)
    }

    // ---
    fun structOf(name: String): StructEndec<T> {
        return StructEndec.of(
            { ctx: SerializationContext, serializer: Serializer<*>, struct: Serializer.Struct, value: T ->
                struct.field(
                    name,
                    ctx,
                    this@Endec,
                    value
                )
            },
            { ctx: SerializationContext, deserializer: Deserializer<*>, struct: Deserializer.Struct ->
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

    fun <S> optionalFieldOf(name: String, getter: (S) -> T, defaultValue: () -> T): StructField<S, T> {
        return StructField(
            name,
            optionalOf().xmap(
                { optional -> optional.orElseGet(defaultValue) },
                { value: T -> OptionalOfNullable(value) }), getter, defaultValue
        )
    }

    fun interface Encoder<T> {
        fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T)
    }

    fun interface Decoder<T> {
        fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T
    }

    fun interface DecoderWithError<T> {
        fun decode(ctx: SerializationContext, serializer: Deserializer<*>, exception: Exception): T
    }

    companion object {
        // --- Constructors ---
        fun <T> of(encoder: Encoder<T>, decoder: Decoder<T>): Endec<T> {
            return object : Endec<T> {
                override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
                    encoder.encode(ctx, serializer, value)
                }

                override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
                    return decoder.decode(ctx, deserializer)
                }
            }
        }

        fun <T> recursive(builderFunc: (Endec<T>) -> Endec<T>): Endec<T> {
            return RecursiveEndec(builderFunc)
        }

        fun <T> unit(instance: T): StructEndec<T> {
            return unit { instance }
        }

        fun <T> unit(instance: () -> T): StructEndec<T> {
            return StructEndec.of(
                { ctx: SerializationContext, serializer: Serializer<*>, struct: Serializer.Struct, value: T -> },
                { ctx: SerializationContext, deserializer: Deserializer<*>, struct: Deserializer.Struct -> instance() })
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
        fun <K, V> map(keyEndec: Endec<K?>, valueEndec: Endec<V?>): Endec<Map<K?, V?>> {
            return StructEndecBuilder.of(
                keyEndec.fieldOf<Pair<K?, V?>>("k") { it.first },
                valueEndec.fieldOf<Pair<K?, V?>>("v") { it.second }
            ) { k: K?, v: V? -> k to v }.listOf().xmap({ entries: List<Pair<K?, V?>> ->
                entries.toMap()
            }, { kvMap: Map<K?, V?> -> kvMap.entries.toList().map { it.key to it.value } })
        }

//        /**
//         * Create a new endec which serializes a map from keys encoded as strings using
//         * `keyToString` and decoded using `stringToKey` to values serialized
//         * using `valueEndec`
//         */
//        fun <K, V> map(
//            keyToString: Function<K, String>,
//            stringToKey: Function<String, K>,
//            valueEndec: Endec<V>?
//        ): Endec<Map<K, V>>? {
//            return of(Encoder { ctx: SerializationContext?, serializer: Serializer<*>, map: Map<K, V> ->
//                serializer.map(ctx, valueEndec, map.size).use { mapState ->
//                    map.forEach { (k: K, v: V) -> mapState.entry(keyToString.apply(k), v) }
//                }
//            }, Decoder { ctx: SerializationContext?, deserializer: Deserializer<*> ->
//                val mapState = deserializer.map(ctx, valueEndec)
//                val map = HashMap<K, V>(mapState.estimatedSize())
//                mapState.forEachRemaining { entry: Map.Entry<String?, V> ->
//                    map[stringToKey.apply(entry.key)] = entry.value
//                }
//                map
//            })
//        }

        /**
         * Create a new endec which serializes the enum constants of `enumClass`
         *
         *
         * In a human-readable format, the endec serializes to the [constant&#39;s name][Enum.name],
         * and to its [ordinal][Enum.ordinal] otherwise
         */
        fun <E : Enum<E>> forEnum(enumClass: KClass<E>): Endec<E> {
            return ifAttr(
                SerializationAttributes.HUMAN_READABLE,
                STRING.xmap<E>({ name: String ->
                    getEnumConstants(enumClass).toList().first { e: E -> e.name == name }
                }, { obj: E -> obj.name })
            ).orElse(
                VAR_INT.xmap({ ordinal: Int? -> getEnumConstants(enumClass)[ordinal!!] }, { obj: E -> obj.ordinal })
            )
        }

        // ---
        /**
         * Shorthand for [.dispatchedStruct]
         * which always uses `type` as the `variantKey`
         */
        fun <T, K> dispatchedStruct(
            variantToEndec: (K?) -> StructEndec<out T>,
            instanceToVariant: (T) -> K,
            variantEndec: Endec<K>
        ): StructEndec<out T> {
            return dispatchedStruct(variantToEndec, instanceToVariant, variantEndec, "type")
        }

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
        fun <T, K> dispatchedStruct(
            variantToEndec: (K?) -> StructEndec<out T>,
            instanceToVariant: (T) -> K,
            variantEndec: Endec<K>,
            variantKey: String
        ): StructEndec<T> {
            return object : StructEndec<T> {
                override fun encodeStruct(
                    ctx: SerializationContext,
                    serializer: Serializer<*>,
                    struct: Serializer.Struct,
                    value: T
                ) {
                    val variant = instanceToVariant(value)
                    struct.field(variantKey, ctx, variantEndec, variant)

                    (variantToEndec(variant) as StructEndec<T?>).encodeStruct(ctx, serializer, struct, value)
                }

                override fun decodeStruct(
                    ctx: SerializationContext,
                    deserializer: Deserializer<*>,
                    struct: Deserializer.Struct
                ): T? {
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
        fun <T, K> dispatched(
            variantToEndec: (K?) -> Endec<out T>,
            instanceToVariant: (T) -> K,
            variantEndec: Endec<K>
        ): StructEndec<T> {
            return object : StructEndec<T> {
                override fun encodeStruct(
                    ctx: SerializationContext,
                    serializer: Serializer<*>,
                    struct: Serializer.Struct,
                    value: T
                ) {
                    val variant = instanceToVariant(value)
                    struct.field("variant", ctx, variantEndec, variant)

                    struct.field("instance", ctx, (variantToEndec(variant) as Endec<T>), value)
                }

                override fun decodeStruct(
                    ctx: SerializationContext,
                    deserializer: Deserializer<*>,
                    struct: Deserializer.Struct
                ): T? {
                    val variant = struct.field("variant", ctx, variantEndec, null)
                    return struct.field("instance", ctx, variantToEndec(variant), null)
                }
            }
        }

        // ---
        fun <T> ifAttr(attribute: SerializationAttribute, endec: Endec<T>): AttributeEndecBuilder<T> {
            return AttributeEndecBuilder(endec, attribute)
        }

        fun <N> clampedMax(endec: Endec<N>, max: N): Endec<N> where N : Number, N : Comparable<N> {
            return clamped<N>(endec, null, max)
        }

        fun <N> rangedMax(
            endec: Endec<N>,
            max: N,
            throwError: Boolean
        ): Endec<N> where N : Number, N : Comparable<N> {
            return ranged(endec, null, max, throwError)
        }

        fun <N> clampedMin(endec: Endec<N>, min: N): Endec<N> where N : Number, N : Comparable<N> {
            return clamped(endec, min, null)
        }

        fun <N> rangedMin(
            endec: Endec<N>,
            min: N,
            throwError: Boolean
        ): Endec<N> where N : Number, N : Comparable<N> {
            return ranged(endec, min, null, throwError)
        }

        fun <N> clamped(endec: Endec<N>, min: N?, max: N?): Endec<N> where N : Number, N : Comparable<N> {
            return ranged(endec, min, max, false)
        }

        fun <N> ranged(
            endec: Endec<N>,
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

            return endec.xmap({ t: N -> errorChecker(t) }, { t: N -> errorChecker(t) })
        }

        // --- Serializer Primitives ---
        val VOID: Endec<Unit> = of({ ctx: SerializationContext, serializer: Serializer<*>, unused: Unit -> },
            { ctx: SerializationContext, deserializer: Deserializer<*> -> null })

        val BOOLEAN: Endec<Boolean> =
            of({ ctx: SerializationContext, serializer: Serializer<*>, value: Boolean ->
                serializer.writeBoolean(
                    ctx,
                    value
                )
            }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readBoolean(ctx) })
        val BYTE: Endec<Byte> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Byte ->
            serializer.writeByte(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readByte(ctx) })
        val SHORT: Endec<Short> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Short ->
            serializer.writeShort(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readShort(ctx) })
        val INT: Endec<Int> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Int ->
            serializer.writeInt(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readInt(ctx) })
        val VAR_INT: Endec<Int> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Int ->
            serializer.writeVarInt(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readVarInt(ctx) })
        val LONG: Endec<Long> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Long ->
            serializer.writeLong(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readLong(ctx) })
        val VAR_LONG: Endec<Long> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Long ->
            serializer.writeVarLong(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readVarLong(ctx) })
        val FLOAT: Endec<Float> = of({ ctx: SerializationContext, serializer: Serializer<*>, value: Float ->
            serializer.writeFloat(
                ctx,
                value
            )
        }, Decoder { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readFloat(ctx) })
        val DOUBLE: Endec<Double> =
            of({ ctx: SerializationContext, serializer: Serializer<*>, value: Double ->
                serializer.writeDouble(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readDouble(ctx) })
        val STRING: Endec<String> =
            of({ ctx: SerializationContext, serializer: Serializer<*>, value: String ->
                serializer.writeString(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readString(ctx) })
        val BYTES: Endec<ByteArray> =
            of({ ctx: SerializationContext, serializer: Serializer<*>, value: ByteArray ->
                serializer.writeBytes(
                    ctx,
                    value
                )
            }, { ctx: SerializationContext, deserializer: Deserializer<*> -> deserializer.readBytes(ctx) })
    }
}