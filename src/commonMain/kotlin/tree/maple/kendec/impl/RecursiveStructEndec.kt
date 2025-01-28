@file:JsExport
package tree.maple.kendec.impl

import tree.maple.kendec.*
import kotlin.js.JsExport

class RecursiveStructEndec<T>(builder: (StructEndec<T>) -> StructEndec<T>) : StructEndec<T> {
    val structEndec: StructEndec<T> = builder(this)

    override fun encodeStruct(
        ctx: SerializationContext,
        serializer: Serializer<*>,
        struct: StructSerializer,
        value: T
    ) {
        structEndec.encodeStruct(ctx, serializer, struct, value)
    }

    override fun decodeStruct(
        ctx: SerializationContext,
        deserializer: Deserializer<*>,
        struct: StructDeserializer
    ): T {
        return structEndec.decodeStruct(ctx, deserializer, struct)
    }
}
