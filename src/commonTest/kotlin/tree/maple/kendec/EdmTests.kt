package tree.maple.kendec

import okio.Buffer
import okio.IOException
import tree.maple.kendec.EdmTests.Triple
import tree.maple.kendec.format.edm.EdmDeserializer
import tree.maple.kendec.format.edm.EdmElement
import tree.maple.kendec.format.edm.EdmIo
import tree.maple.kendec.format.edm.EdmSerializer
import tree.maple.kendec.impl.StructEndecBuilder
import tree.maple.kendec.util.OptionalOf
import tree.maple.kendec.util.OptionalOfEmpty
import kotlin.jvm.JvmRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EdmTests {
    @Test
    fun toStringFormatting() {
        val edmElement = EdmElement.map(
            Utils.make({ LinkedHashMap() }, { innerMap ->
                innerMap.put(
                    "ah_yes",
                    EdmElement.sequence(listOf(EdmElement.i32(17), EdmElement.string("a")))
                )
                innerMap.put("hmmm", EdmElement.optional(OptionalOfEmpty()))
                innerMap.put(
                    "uhhh", EdmElement.optional(
                        EdmElement.map(
                            Utils.make({ LinkedHashMap() }, { map ->
                                map.put("b", EdmElement.optional(EdmElement.f32(16.5f)))
                            })
                        )
                    )
                )
            })
        )

        assertEquals(
            """
                map({
                  "ah_yes": sequence([
                    i32(17),
                    string("a")
                  ]),
                  "hmmm": optional(),
                  "uhhh": optional(map({
                    "b": optional(f32(16.5))
                  }))
                })
                """.trimIndent(),
            edmElement.toString()
        )
    }

    @Test
    fun structEncode() {

        val endec: StructEndec<Triple<List<Int>, String?, Map<String, Float?>?>> = StructEndecBuilder.of(
            Endec.INT.listOf().fieldOf("ah_yes", Tri::a),
            Endec.STRING.nullableOf().fieldOf("hmmm", Tri::b),
            Endec.FLOAT.nullableOf().mapOf().nullableOf().fieldOf("uhhh", Tri::c)
        ) { a, b, c -> Triple(a, b, c) }

        val value: Tri = Triple(
            listOf(34, 35),
            "test",
            mapOf("b" to 16.5f)
        )

        val originalEdmElement = EdmElement.map(
            Utils.make({ LinkedHashMap() }, { innerMap ->
                innerMap.put("ah_yes", EdmElement.sequence(listOf(EdmElement.i32(34), EdmElement.i32(35))))
                innerMap.put("hmmm", EdmElement.optional(OptionalOf(EdmElement.string("test"))))
                innerMap.put(
                    "uhhh", EdmElement.optional(
                        EdmElement.map(
                            Utils.make({ LinkedHashMap() }, { map ->
                                map.put("b", EdmElement.optional(EdmElement.f32(16.5f)))
                            })
                        )
                    )
                )
            })
        )

        val encodedElement = endec.encodeFully(EdmSerializer::of, value)

        assertEquals(originalEdmElement, encodedElement)
    }

    @Test
    fun structDecode() {
        val endec = StructEndecBuilder.of(
            Endec.INT.listOf().fieldOf("ah_yes", Tri::a),
            Endec.STRING.nullableOf().fieldOf("hmmm", Tri::b),
            Endec.FLOAT.nullableOf().mapOf().nullableOf().fieldOf("uhhh", Tri::c)
        ) { a, b, c -> Triple(a, b, c) }

        val edmElement = EdmElement.map(
            mapOf(
                "ah_yes" to EdmElement.sequence(listOf(EdmElement.i32(34), EdmElement.i32(35))),
                "hmmm" to EdmElement.optional(OptionalOfEmpty()),
                "uhhh" to EdmElement.optional(EdmElement.map(mapOf("b" to EdmElement.optional(EdmElement.f32(16.5f)))))
            )
        )

        val decodedValue = endec.decodeFully(EdmDeserializer::of, edmElement)

        assertEquals(decodedValue.a, listOf(34, 35))
        assertNull(decodedValue.b)
        assertEquals(decodedValue.c, mapOf("b" to 16.5f))
    }

    data class Triple<A, B, C>(val a: A, val b: B, val c: C)

    @Test
    fun edmEncodeAndDecode() {
        val edmElement = EdmElement.map(
            Utils.make({ LinkedHashMap() }, { innerMap ->
                innerMap.put(
                    "ah_yes",
                    EdmElement.sequence(listOf(EdmElement.i32(17), EdmElement.string("a")))
                )
                innerMap.put("hmmm", EdmElement.optional(OptionalOf(EdmElement.string("test"))))
                innerMap.put(
                    "uhhh",
                    EdmElement.optional(
                        EdmElement.map(
                            Utils.make({ LinkedHashMap() }, { map ->
                                map.put("b", EdmElement.optional(EdmElement.f32(16.5f)))
                            })
                        )
                    )
                )
            })
        )


        assertEquals(edmElement, decodeEdmElement(encodeEdmElement(edmElement)))
    }

    @Test
    fun bytesFormatting() {
        println(EdmElement.bytes(byteArrayOf(1, 2, 4, 8, 16)))
    }

    companion object {
        private fun encodeEdmElement(edmElement: EdmElement<*>): ByteArray {
            try {
                val dataOutput = Buffer()

                EdmIo.encode(dataOutput, edmElement)

                return dataOutput.readByteArray()
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        private fun decodeEdmElement(bytes: ByteArray): EdmElement<*> {
            try {
                val dataInput = Buffer().write(bytes)

                return EdmIo.decode(dataInput)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }
    }
}

typealias Tri = Triple<List<Int>, String?, Map<String, Float?>?>
