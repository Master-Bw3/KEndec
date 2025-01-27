package tree.maple.kendec.format.edm

import okio.Buffer
import okio.IOException
import okio.internal.commonAsUtf8ToByteArray
import tree.maple.kendec.util.Optional
import tree.maple.kendec.util.OptionalOf
import tree.maple.kendec.util.OptionalOfEmpty

object EdmIo {
    @Throws(IOException::class)
    fun encode(output: Buffer, data: EdmElement<*>) {
        output.writeByte(data.type().ordinal)
        encodeElementData(output, data)
    }

    @Throws(IOException::class)
    fun decode(input: Buffer): EdmElement<*> {
        return decodeElementData(input, input.readByte())
    }

    @Throws(IOException::class)
    fun encodeElementData(output: Buffer, data: EdmElement<*>) {
        when (data.type()) {
            EdmElement.Type.I8, EdmElement.Type.U8 -> output.writeByte(data.cast<Byte>().toInt())
            EdmElement.Type.I16, EdmElement.Type.U16 -> output.writeShort(data.cast<Short>().toInt())
            EdmElement.Type.I32, EdmElement.Type.U32 -> output.writeInt(data.cast())
            EdmElement.Type.I64, EdmElement.Type.U64 -> output.writeLong(data.cast())
            EdmElement.Type.F32 -> output.writeInt(data.cast<Float>().toRawBits())
            EdmElement.Type.F64 -> output.writeLong(data.cast<Double>().toRawBits())
            EdmElement.Type.BOOLEAN -> output.writeByte(if (data.cast<Boolean>()) 1 else 0)
            EdmElement.Type.STRING ->{
                val str = data.cast<String>()
                output.writeLong(str.commonAsUtf8ToByteArray().size.toLong())
                output.writeUtf8(str)
            }
            EdmElement.Type.BYTES -> {
                output.writeInt(data.cast<ByteArray>().size)
                output.write(data.cast<ByteArray>())
            }

            EdmElement.Type.OPTIONAL -> {
                val optional = data.cast<Optional<EdmElement<*>>>()

                output.writeByte(if (optional.isPresent()) 1 else 0)
                if (optional.isPresent()) {
                    val element = optional.get()

                    output.writeByte(element.type().ordinal)
                    encodeElementData(output, element)
                }
            }

            EdmElement.Type.SEQUENCE -> {
                val list = data.cast<List<EdmElement<*>>>()

                output.writeInt(list.size)
                if (!list.isEmpty()) {
                    for (element in list) {
                        output.writeByte(element.type().ordinal)
                        encodeElementData(output, element)
                    }
                }
            }

            EdmElement.Type.MAP -> {
                val map = data.cast<Map<String, EdmElement<*>>>()

                output.writeInt(map.size)
                for ((key, value) in map) {
                    output.writeLong(key.commonAsUtf8ToByteArray().size.toLong())
                    output.writeUtf8(key)

                    output.writeByte(value.type().ordinal)
                    encodeElementData(output, value)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun decodeElementData(input: Buffer, type: Byte): EdmElement<*> {
        return when (EdmElement.Type.entries[type.toInt()]) {
            EdmElement.Type.I8 -> EdmElement.i8(input.readByte())
            EdmElement.Type.U8 -> EdmElement.u8(input.readByte())
            EdmElement.Type.I16 -> EdmElement.i16(input.readShort())
            EdmElement.Type.U16 -> EdmElement.u16(input.readShort())
            EdmElement.Type.I32 -> EdmElement.i32(input.readInt())
            EdmElement.Type.U32 -> EdmElement.u32(input.readInt())
            EdmElement.Type.I64 -> EdmElement.i64(input.readLong())
            EdmElement.Type.U64 -> EdmElement.u64(input.readLong())
            EdmElement.Type.F32 -> EdmElement.f32(Float.fromBits(input.readInt()))
            EdmElement.Type.F64 -> EdmElement.f64(Double.fromBits(input.readLong()))
            EdmElement.Type.BOOLEAN -> EdmElement.bool(input.readByte() == 1.toByte())
            EdmElement.Type.STRING ->  {
                val length = input.readLong()
                EdmElement.string(input.readUtf8(length))
            }
            EdmElement.Type.BYTES -> {
                val result = ByteArray(input.readInt())
                input.readFully(result)

                EdmElement.bytes(result)
            }

            EdmElement.Type.OPTIONAL -> {
                if (input.readByte().toInt() != 0) {
                    EdmElement.optional(
                        OptionalOf<EdmElement<*>>(
                            decodeElementData(
                                input,
                                input.readByte()
                            )
                        )
                    )
                } else {
                    EdmElement.optional(OptionalOfEmpty<EdmElement<*>>())
                }
            }

            EdmElement.Type.SEQUENCE -> {
                val length = input.readInt()
                if (length != 0) {
                    val result = ArrayList<EdmElement<*>>(length)

                    for (i in 0 until length) {
                        result.add(decodeElementData(input, input.readByte()))
                    }

                    EdmElement.sequence(result)
                } else {
                    EdmElement.sequence(listOf<EdmElement<*>>())
                }
            }

            EdmElement.Type.MAP -> {
                val length = input.readInt()
                val result = LinkedHashMap<String, EdmElement<*>>(length)

                for (i in 0 until length) {
                    val keyLength = input.readLong()
                    result[input.readUtf8(keyLength)] = decodeElementData(input, input.readByte())
                }

                EdmElement.consumeMap(result)
            }
        }
    }
}
