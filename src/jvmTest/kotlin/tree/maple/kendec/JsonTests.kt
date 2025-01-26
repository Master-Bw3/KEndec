package tree.maple.kendec

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tree.maple.kendec.format.gson.GsonDeserializer
import tree.maple.kendec.format.gson.GsonEndec
import tree.maple.kendec.format.gson.GsonSerializer
import tree.maple.kendec.impl.StructEndecBuilder
import tree.maple.kendec.impl.StructField
import java.util.function.Consumer
import java.util.function.Supplier

class JsonTests {
    @Test
    @DisplayName("encode string")
    fun encodeString() {
        val value = "an epic string"
        val result = Endec.STRING.encodeFully(GsonSerializer::of, value)
        println("Result: " + result + ", Type: " + result?.javaClass?.simpleName)
    }

    @Test
    @DisplayName("encode struct")
    fun encodeStruct() {


        val endec= StructEndecBuilder.of(
            Endec.STRING.fieldOf("a_field", StructObject::aField),
            Endec.STRING.mapOf().fieldOf("a_nested_field", StructObject::aNestedField),
            Endec.DOUBLE.listOf().fieldOf("list_moment", StructObject::listMoment),
            Endec.STRING.nullableOf().fieldOf("another_field", StructObject::anotherField),
            StructEndecBuilder.Function4 { aField, aNestedField, listMoment, anotherField  ->
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
            Utils.make({ LinkedHashMap() }, { map: java.util.LinkedHashMap<String, String> ->
                map["a"] = "bruh"
                map["b"] = "nested field value, epic"
            }),
            java.util.List.of(1.0, 5.7, Double.MAX_VALUE),
            "this too"
        )

        val encodedElement = endec.encodeFully(GsonSerializer::of, structObject)

        Assertions.assertEquals(
            Utils.make<JsonObject>(
                { JsonObject() },
                { jsonObject: JsonObject ->
                    jsonObject.addProperty("a_field", "an epic field value")
                    jsonObject.add(
                        "a_nested_field",
                        Utils.make<JsonObject>({ JsonObject() },
                            { jsonObject1: JsonObject ->
                                jsonObject1.addProperty("a", "bruh")
                                jsonObject1.addProperty("b", "nested field value, epic")
                            })
                    )
                    jsonObject.add(
                        "list_moment",
                        Utils.make<JsonArray>({ JsonArray() },
                            { jsonArray: JsonArray ->
                                jsonArray.add(1.0)
                                jsonArray.add(5.7)
                                jsonArray.add(Double.MAX_VALUE)
                            })
                    )
                    jsonObject.addProperty("another_field", "this too")
                }),
            encodedElement
        )

        val decodedValue = endec.decodeFully(GsonDeserializer::of, encodedElement)

        Assertions.assertEquals(structObject, decodedValue)
    }

    @Test
    @DisplayName("encode json to json")
    fun encodeJsonToJson() {
        val json = Utils.make<JsonObject>(
            { JsonObject() }, { jsonObject: JsonObject ->
                jsonObject.addProperty("a field", "some json here")
                jsonObject.add(
                    "another field",
                    Utils.make<JsonArray>({ JsonArray() },
                        { jsonArray: JsonArray ->
                            jsonArray.add(1.0)
                            jsonArray.add(
                                Utils.make<JsonObject>(
                                    { JsonObject() },
                                    { jsonObject1: JsonObject ->
                                        jsonObject1.add("hmmm", JsonNull.INSTANCE)
                                    })
                            )
                        })
                )
            }
        )

        val encoded = GsonEndec.INSTANCE.encodeFully(GsonSerializer::of, json)
        Assertions.assertEquals(json, encoded)
    }

    @Test
    @DisplayName("omit optional field during encoding / read default during decoding")
    fun optionalFieldHandling() {
        val endec = StructEndecBuilder.of(
            Endec.INT.optionalFieldOf("field", SingleInteger::integer) { 0 }
        ) { integer -> SingleInteger(integer) }

        Assertions.assertEquals(JsonObject(), endec.encodeFully(GsonSerializer::of, SingleInteger(null)))

        Assertions.assertEquals(SingleInteger(0), endec.decodeFully(GsonDeserializer::of, JsonObject()))
    }

    @JvmRecord
    data class SingleInteger(val integer: Int?)

    @JvmRecord
    data class StructObject(
        val aField: String,
        val aNestedField: Map<String, String>,
        val listMoment: List<Double>,
        val anotherField: String?
    )
}
