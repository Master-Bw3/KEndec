package tree.maple.kendec.impl

import tree.maple.kendec.Deserializer
import tree.maple.kendec.SerializationContext
import tree.maple.kendec.Serializer
import tree.maple.kendec.StructEndec

class RecursiveStructEndec<T>(builder: (StructEndec<T>) -> StructEndec<T>) : StructEndec<T> {
    val structEndec: StructEndec<T> = builder(this)

    override fun encodeStruct(
        ctx: SerializationContext,
        serializer: Serializer<*>,
        struct: Serializer.Struct,
        value: T
    ) {
        structEndec.encodeStruct(ctx, serializer, struct, value)
    }

    override fun decodeStruct(
        ctx: SerializationContext,
        deserializer: Deserializer<*>,
        struct: Deserializer.Struct
    ): T {
        return structEndec.decodeStruct(ctx, deserializer, struct)
    }
}
