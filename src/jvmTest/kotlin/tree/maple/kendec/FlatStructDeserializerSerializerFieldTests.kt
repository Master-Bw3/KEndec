package tree.maple.kendec

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tree.maple.kendec.Utils.make
import tree.maple.kendec.format.gson.GsonSerializer
import tree.maple.kendec.impl.StructEndecBuilder

class FlatStructDeserializerSerializerFieldTests {
    @Test
    @DisplayName("encode child class to json")
    fun encodeChildClassToJson() {
        val fieldsEndec = StructEndecBuilder.of(
            PrimitiveEndecs.STRING.fieldOf("a_field") { o -> o.aField },
            PrimitiveEndecs.INT.fieldOf("another_field") { `object` -> `object`.anotherField }
        ) { aField: String, anotherField: Int -> ParentData(aField, anotherField) }

        val childClassEndec: StructEndec<ChildClass> = StructEndecBuilder.of(
            fieldsEndec.flatFieldOf { obj: ParentClass -> obj.parentData() },
            PrimitiveEndecs.DOUBLE.listOf().fieldOf("third_field") { o -> o.thirdField }
        ) { parentData, thirdField -> ChildClass(parentData.aField, parentData.anotherField, thirdField) }

        val encodedElement =
            childClassEndec.encodeFully(GsonSerializer::of, ChildClass("a", 7, listOf(1.2, 2.4)))

        Assertions.assertEquals(
            make({ JsonObject() }, { jsonObject ->
                jsonObject.addProperty("a_field", "a")
                jsonObject.addProperty("another_field", 7)
                jsonObject.add(
                    "third_field",
                    make({ JsonArray() },  { jsonArray ->
                        jsonArray.add(1.2)
                        jsonArray.add(2.4)
                    })
                )
            }),
            encodedElement
        )
    }

    @Test
    @DisplayName("encode grandchild class to json")
    fun encodeGrandChildClassToJson() {
        val fieldsEndec = StructEndecBuilder.of(
            PrimitiveEndecs.STRING.fieldOf("a_field") { `object` -> `object`.aField },
            PrimitiveEndecs.INT.fieldOf("another_field") { `object` -> `object`.anotherField }
        ) { aField: String, anotherField: Int -> ParentData(aField, anotherField) }

        val childClassEndec: StructEndec<ChildClass> = StructEndecBuilder.of(
            fieldsEndec.flatFieldOf { obj: ParentClass -> obj.parentData() },
            PrimitiveEndecs.DOUBLE.listOf().fieldOf("third_field") { o -> o.thirdField }
        ) { parentData, thirdField -> ChildClass(parentData.aField, parentData.anotherField, thirdField) }

        val grandChildClassEndec: StructEndec<GrandchildClass> = StructEndecBuilder.of(
            childClassEndec.flatInheritedFieldOf(),
            PrimitiveEndecs.BOOLEAN.fieldOf("bruh") { o -> o.bruh }
        ) { childClass, thirdField ->
            GrandchildClass(
                childClass.aField,
                childClass.anotherField,
                childClass.thirdField,
                thirdField
            )
        }

        val encodedElement =
            grandChildClassEndec.encodeFully(GsonSerializer::of, GrandchildClass("b", 77, listOf(3.4, 3.5), false))

        Assertions.assertEquals(
            make({ JsonObject() }, { jsonObject ->
                jsonObject.addProperty("a_field", "b")
                jsonObject.addProperty("another_field", 77)
                jsonObject.add(
                    "third_field",
                    make({ JsonArray() }, { jsonArray ->
                        jsonArray.add(3.4)
                        jsonArray.add(3.5)
                    })
                )
                jsonObject.addProperty("bruh", false)
            }),
            encodedElement
        )
    }

    abstract class ParentClass protected constructor(val aField: String, val anotherField: Int) {
        fun parentData(): ParentData {
            return ParentData(this.aField, this.anotherField)
        }
    }

    @JvmRecord
    data class ParentData(val aField: String, val anotherField: Int)

    open class ChildClass(aField: String, anotherField: Int, val thirdField: List<Double>) :
        ParentClass(aField, anotherField)

    class GrandchildClass(aField: String, anotherField: Int, thirdField: List<Double>, val bruh: Boolean) :
        ChildClass(aField, anotherField, thirdField)
}
