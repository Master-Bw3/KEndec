package io.github.master_bw3.kendec


import io.github.master_bw3.kendec.format.json.JsonDeserializer
import io.github.master_bw3.kendec.format.json.JsonEndec
import io.github.master_bw3.kendec.format.json.JsonSerializer
import io.github.master_bw3.kendec.impl.StructEndecBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonTests {
    @Test
    fun encodeString() {
        val value = "an epic string"
        val result = PrimitiveEndecs.STRING.encodeFully(JsonSerializer::of, value)
        println("Result: " + result)
    }

    @Test
    fun encodeStruct() {
        val endec = StructEndecBuilder.of(
            PrimitiveEndecs.STRING.fieldOf("a_field", StructObject::aField),
            PrimitiveEndecs.STRING.mapOf().fieldOf("a_nested_field", StructObject::aNestedField),
            PrimitiveEndecs.DOUBLE.listOf().fieldOf("list_moment", StructObject::listMoment),
            PrimitiveEndecs.STRING.nullableOf().fieldOf("another_field", StructObject::anotherField),
            StructEndecBuilder.Function4 { aField, aNestedField, listMoment, anotherField ->
                StructObject(
                    aField,
                    aNestedField,
                    listMoment,
                    anotherField
                )
            }
        )

        val structObject = StructObject(
            "an epic field value",
            Utils.make({ LinkedHashMap() }, { map: LinkedHashMap<String, String> ->
                map["a"] = "bruh"
                map["b"] = "nested field value, epic"
            }),
            listOf(1.0, 5.7, Double.MAX_VALUE),
            "this too"
        )

        val encodedElement = endec.encodeFully(JsonSerializer::of, structObject)

        assertEquals(
            JsonObject(
                mapOf(
                    "a_field" to JsonPrimitive("an epic field value"),

                    "a_nested_field" to JsonObject(
                        mapOf(
                            "a" to JsonPrimitive("bruh"),
                            "b" to JsonPrimitive("nested field value, epic")
                        )
                    ),

                    "list_moment" to JsonArray(
                        listOf(
                            JsonPrimitive(1.0),
                            JsonPrimitive(5.7),
                            JsonPrimitive(Double.MAX_VALUE)
                        )
                    ),

                    "another_field" to JsonPrimitive("this too")
                )
            ),
            encodedElement
        )

        val decodedValue = endec.decodeFully(JsonDeserializer::of, encodedElement)

        assertEquals(structObject, decodedValue)
    }

    @Test
    fun encodeJsonToJson() {
        val json = JsonObject(
            mapOf(
                "a field" to JsonPrimitive("some json here"),

                "another field" to JsonArray(
                    listOf(
                        JsonPrimitive(1.0),
                        JsonObject(
                            mapOf(
                                "hmmm" to JsonNull
                            )
                        )
                    )
                )
            )
        )

        val encoded = JsonEndec.INSTANCE.encodeFully(JsonSerializer::of, json)
        assertEquals(json, encoded)
    }

    @Test
    fun optionalFieldHandling() {
        val endec = StructEndecBuilder.of(
            PrimitiveEndecs.INT.optionalFieldOf("field", SingleInteger::integer) { 0 }
        ) { integer -> SingleInteger(integer) }

        assertEquals(JsonObject(mapOf()), endec.encodeFully(JsonSerializer::of, SingleInteger(null)))

        assertEquals(SingleInteger(0), endec.decodeFully(JsonDeserializer::of, JsonObject(mapOf())))
    }

    data class SingleInteger(val integer: Int?)

    data class StructObject(
        val aField: String,
        val aNestedField: Map<String, String>,
        val listMoment: List<Double>,
        val anotherField: String?
    )
}
