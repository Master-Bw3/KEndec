package tree.maple.kendec.format.gson

import com.google.gson.JsonElement
import com.google.gson.JsonStreamParser
import tree.maple.kendec.*

class GsonEndec private constructor() : Endec<JsonElement> {
    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: JsonElement) {
        if (serializer is SelfDescribedSerializer<*>) {
            GsonDeserializer.Companion.of(value).readAny(ctx, serializer)
            return
        }

        serializer.writeString(ctx, value.toString())
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): JsonElement {
        if (deserializer is SelfDescribedDeserializer<*>) {
            val json: GsonSerializer = GsonSerializer.of()
            deserializer.readAny(ctx, json)

            return json.result()!!
        }

        return JsonStreamParser(deserializer.readString(ctx)).next()
    }

    companion object {
        val INSTANCE: GsonEndec = GsonEndec()
    }
}
