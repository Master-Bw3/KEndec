@file:JsExport
package tree.maple.kendec.format.edm

import tree.maple.kendec.util.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.JsExport
import kotlin.js.JsName


open class EdmElement<T : Any> internal constructor(private val value: T, private val type: Type) {
    fun value(): T {
        return this.value
    }

    fun <V> cast(): V {
        return value as V
    }

    fun type(): Type {
        return this.type
    }

    fun unwrap(): Any {
        return if (value is List<*>) {
            value.map { o -> (o as EdmElement<*>).unwrap() }.toList()
        } else if (value is Map<*, *>) {
            value.entries.map { entry ->
                    entry.key to (entry.value as EdmElement<*>).unwrap()
                }

        } else if (value is Optional<*>) {
            value.map { o -> (o as EdmElement<*>).unwrap() }
        } else {
            value
        }
    }

    /**
     * Create a copy of this EDM element as an [EdmMap], which
     * implements the [tree.maple.kendec.util.MapCarrier] interface
     */
    fun asMap(): EdmMap {
        check(this.type == Type.MAP) { "Cannot cast EDM element of type " + this.type + " to MAP" }

        return EdmMap(HashMap(this.cast<Map<String, EdmElement<*>>>()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdmElement<*>) return false
        if (value != other.value) return false
        return this.type == other.type
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return format(BlockWriter()).buildResult()
    }

    @OptIn(ExperimentalEncodingApi::class)
    protected fun format(formatter: BlockWriter): BlockWriter {
        return when (this.type) {
            Type.BYTES -> {
                formatter.writeBlock("bytes(", ")", false) { blockWriter ->
                    blockWriter.write(
                        Base64.encode(
                            this.cast()
                        )
                    )
                }
            }

            Type.MAP -> {
                formatter.writeBlock("map({", "})") {
                    val map = this.cast<Map<String, EdmElement<*>>>()
                    var idx = 0
                    for ((key, value1) in map) {
                        formatter.write("\"$key\": ")
                        value1.format(formatter)

                        if (idx < map.size - 1) formatter.writeln(",")

                        idx++
                    }
                }
            }

            Type.SEQUENCE -> {
                formatter.writeBlock("sequence([", "])") {
                    val list = this.cast<List<EdmElement<*>>>()
                    for (idx in list.indices) {
                        list[idx].format(formatter)
                        if (idx < list.size - 1) formatter.writeln(",")
                    }
                }
            }

            Type.OPTIONAL -> {
                formatter.writeBlock("optional(", ")", false) {
                    val optional = this.cast<Optional<EdmElement<*>>>()
                    optional.ifPresentOrElse(
                        { edmElement: EdmElement<*> -> edmElement.format(formatter) },
                        { formatter.write("") })
                }
            }

            Type.STRING -> {
                formatter.writeBlock("string(\"", "\")", false) { blockWriter ->
                    blockWriter.write(
                        value.toString()
                    )
                }
            }

            else -> {
                formatter.writeBlock(type.formatName() + "(", ")", false) { blockWriter ->
                    blockWriter.write(
                        value.toString()
                    )
                }
            }
        }
    }

    enum class Type {
        I8,
        U8,
        I16,
        U16,
        I32,
        U32,
        I64,
        U64,
        F32,
        F64,

        BOOLEAN,
        STRING,
        BYTES,
        OPTIONAL,

        SEQUENCE,
        MAP;

        fun formatName(): String {
            return name.lowercase()
        }
    }

    companion object {
        val EMPTY: EdmElement<Optional<EdmElement<*>>> = EdmElement(OptionalOfEmpty(), Type.OPTIONAL)

        fun i8(value: Byte): EdmElement<Byte> {
            return EdmElement(value, Type.I8)
        }

        fun u8(value: Byte): EdmElement<Byte> {
            return EdmElement(value, Type.U8)
        }

        fun i16(value: Short): EdmElement<Short> {
            return EdmElement(value, Type.I16)
        }

        fun u16(value: Short): EdmElement<Short> {
            return EdmElement(value, Type.U16)
        }

        fun i32(value: Int): EdmElement<Int> {
            return EdmElement(value, Type.I32)
        }

        fun u32(value: Int): EdmElement<Int> {
            return EdmElement(value, Type.U32)
        }

        fun i64(value: Long): EdmElement<Long> {
            return EdmElement(value, Type.I64)
        }

        fun u64(value: Long): EdmElement<Long> {
            return EdmElement(value, Type.U64)
        }

        fun f32(value: Float): EdmElement<Float> {
            return EdmElement(value, Type.F32)
        }

        fun f64(value: Double): EdmElement<Double> {
            return EdmElement(value, Type.F64)
        }

        fun bool(value: Boolean): EdmElement<Boolean> {
            return EdmElement(value, Type.BOOLEAN)
        }

        fun string(value: String): EdmElement<String> {
            return EdmElement(value, Type.STRING)
        }

        fun bytes(value: ByteArray): EdmElement<ByteArray> {
            return EdmElement(value, Type.BYTES)
        }

        fun optional(value: EdmElement<*>): EdmElement<Optional<EdmElement<*>>> {
            return optional(OptionalOfNullable(value))
        }

        @JsName("fromOptional")
        fun optional(value: Optional<EdmElement<*>>): EdmElement<Optional<EdmElement<*>>> {
            if (value.isEmpty()) return EMPTY

            return EdmElement(value, Type.OPTIONAL)
        }

        fun sequence(value: List<EdmElement<*>>): EdmElement<List<EdmElement<*>>> {
            return EdmElement(value.toList(), Type.SEQUENCE)
        }

        fun map(value: Map<String, EdmElement<*>>): EdmElement<Map<String, EdmElement<*>>> {
            return EdmElement(value.toMap(), Type.MAP)
        }

        fun consumeMap(value: Map<String, EdmElement<*>>): EdmElement<Map<String, EdmElement<*>>> {
            return EdmElement(value, Type.MAP) // Hangry
        }
    }
}

