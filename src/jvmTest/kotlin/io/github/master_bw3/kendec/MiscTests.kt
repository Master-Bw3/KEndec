package io.github.master_bw3.kendec

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.github.master_bw3.kendec.Utils.make
import io.github.master_bw3.kendec.format.edm.EdmDeserializer
import io.github.master_bw3.kendec.format.edm.EdmElement
import io.github.master_bw3.kendec.format.edm.EdmSerializer
import io.github.master_bw3.kendec.format.json.JsonDeserializer
import io.github.master_bw3.kendec.format.json.JsonEndec
import io.github.master_bw3.kendec.format.json.JsonSerializer
import io.github.master_bw3.kendec.util.RangeNumberException

class MiscTests {
    @Test
    @DisplayName("xmap string to codepoints")
    fun xmapStringToCodePoints() {
        val codepointEndec = PrimitiveEndecs.SHORT.listOf()
            .xmap(
                { shorts ->
                    val chars = CharArray(shorts.size)
                    for (i in 0 until shorts.size) {
                        chars[i] = Char(shorts[i].toUShort())
                    }
                    String(chars)
                },
                { str -> str.chars().mapToObj { value -> value.toShort() }.toList() }
            )

        val serialized =
            codepointEndec.encodeFully(JsonSerializer::of, "a string") // jsonEncoder.convert(toJson(codepointEndec, ));
        println("encoded: $serialized")
        Assertions.assertEquals(make({ JsonArray() }, { jsonArray ->
            jsonArray.add(97)
            jsonArray.add(32)
            jsonArray.add(115)
            jsonArray.add(116)
            jsonArray.add(114)
            jsonArray.add(105)
            jsonArray.add(110)
            jsonArray.add(103)
        }), serialized)

        val decoded = codepointEndec.decodeFully(JsonDeserializer::of, serialized)
        println("decoded: $decoded")
        Assertions.assertEquals("a string", decoded)
    }

    @Test
    @DisplayName("encode json to edm")
    fun encodeJsonToEdm() {
        val json = make({ JsonObject() },{ jsonObject ->
            jsonObject.addProperty("a field", "some json here")
            jsonObject.add(
                "another_field",
                make({ JsonArray() }, { jsonArray ->
                    jsonArray.add(1.0)
                    jsonArray.add(make({ JsonObject() }, { jsonObject1 ->
                        jsonObject1.add("hmmm", JsonNull.INSTANCE)
                    }))
                })
            )
        })

        val byteBuf = JsonEndec.INSTANCE.encodeFully({ EdmSerializer.of() }, json)

        val decodedJson = JsonEndec.INSTANCE.decodeFully(EdmDeserializer::of, byteBuf!!)

        Assertions.assertEquals(json, decodedJson)
    }

    @Test
    @DisplayName("ranged nums")
    fun rangedNums() {
        Assertions.assertEquals(
            JsonPrimitive(-2),
            PrimitiveEndecs.INT.clamped(-2, 10).encodeFully(JsonSerializer::of, -10)
        )

        Assertions.assertEquals(
            JsonPrimitive(10),
            PrimitiveEndecs.INT.clamped(0, 10).encodeFully(JsonSerializer::of, 15)
        )

        Assertions.assertThrows(RangeNumberException::class.java) {
            PrimitiveEndecs.FLOAT.ranged(-2f, -0.25f, true).encodeFully(JsonSerializer::of, 0.0f)
        }
    }

    @Test
    @DisplayName("attribute branching")
    fun attributeBranching() {
        val attr1 = SerializationAttribute.marker("attr1")
        val attr2 = SerializationAttribute.marker("attr2")

        val endec = ifAttr(attr1, PrimitiveEndecs.BYTE as Endec<Number>)
            .orElseIf(attr2, PrimitiveEndecs.INT as Endec<Number>)
            .orElse(PrimitiveEndecs.LONG as Endec<Number>)

        Assertions.assertEquals(
            EdmElement.Type.I8,
            endec.encodeFully(SerializationContext.attributes(attr1), EdmSerializer::of, 16.toByte())?.type()
        )
        Assertions.assertEquals(
            EdmElement.Type.I8,
            endec.encodeFully(SerializationContext.attributes(attr1, attr2), EdmSerializer::of, 16.toByte())?.type()
        )
        Assertions.assertEquals(
            EdmElement.Type.I8,
            endec.encodeFully(SerializationContext.attributes(attr2, attr1), EdmSerializer::of, 16.toByte())?.type()
        )
        Assertions.assertEquals(
            EdmElement.Type.I32,
            endec.encodeFully(SerializationContext.attributes(attr2), EdmSerializer::of, 16)?.type()
        )
        Assertions.assertEquals(
            EdmElement.Type.I64,
            endec.encodeFully(SerializationContext.empty(), EdmSerializer::of, 16L)?.type()
        )

        Assertions.assertNotEquals(
            EdmElement.Type.I32,
            endec.encodeFully(SerializationContext.attributes(attr1), EdmSerializer::of, 16.toByte())?.type()
        )
    }
}