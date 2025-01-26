package tree.maple.kendec.impl

import tree.maple.kendec.Deserializer
import tree.maple.kendec.Endec
import tree.maple.kendec.SerializationContext
import tree.maple.kendec.Serializer

class RecursiveEndec<T>(builder: (Endec<T>) -> Endec<T>) : Endec<T> {
    val endec: Endec<T> = builder(this)

    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
        endec.encode(ctx, serializer, value)
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
        return endec.decode(ctx, deserializer)
    }
}
