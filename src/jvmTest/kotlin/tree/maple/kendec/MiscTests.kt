package tree.maple.kendec

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tree.maple.kendec.Utils.make
import tree.maple.kendec.format.edm.EdmDeserializer
import tree.maple.kendec.format.edm.EdmElement
import tree.maple.kendec.format.edm.EdmSerializer
import tree.maple.kendec.format.gson.GsonDeserializer
import tree.maple.kendec.format.gson.GsonEndec
import tree.maple.kendec.format.gson.GsonSerializer
import tree.maple.kendec.util.RangeNumberException
import kotlin.jvm.functions.Function0
import kotlin.jvm.functions.Function1

class MiscTests {
    @Test
    @DisplayName("xmap string to codepoints")
    fun xmapStringToCodePoints() {
        val codepointEndec = Endec.SHORT.listOf()
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
            codepointEndec.encodeFully(GsonSerializer::of, "a string") // jsonEncoder.convert(toJson(codepointEndec, ));
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

        val decoded = codepointEndec.decodeFully(GsonDeserializer::of, serialized)
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

        val byteBuf = GsonEndec.INSTANCE.encodeFully({ EdmSerializer.of() }, json)

        val decodedJson = GsonEndec.INSTANCE.decodeFully(EdmDeserializer::of, byteBuf!!)

        Assertions.assertEquals(json, decodedJson)
    }

    @Test
    @DisplayName("ranged nums")
    fun rangedNums() {
        Assertions.assertEquals(
            JsonPrimitive(-2),
            Endec.clamped(Endec.INT, -2, 10).encodeFully(GsonSerializer::of, -10)
        )

        Assertions.assertEquals(
            JsonPrimitive(10),
            Endec.clamped(Endec.INT, 0, 10).encodeFully(GsonSerializer::of, 15)
        )

        Assertions.assertThrows(RangeNumberException::class.java) {
            Endec.ranged(Endec.FLOAT, -2f, -0.25f, true).encodeFully(GsonSerializer::of, 0.0f)
        }
    }

    @Test
    @DisplayName("attribute branching")
    fun attributeBranching() {
        val attr1 = SerializationAttribute.marker("attr1")
        val attr2 = SerializationAttribute.marker("attr2")

        val endec = Endec.ifAttr(attr1, Endec.BYTE as Endec<Number>)
            .orElseIf(attr2, Endec.INT as Endec<Number>)
            .orElse(Endec.LONG as Endec<Number>)

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