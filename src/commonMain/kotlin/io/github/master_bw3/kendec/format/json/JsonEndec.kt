package io.github.master_bw3.kendec.format.json


import io.github.master_bw3.kendec.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class JsonEndec private constructor() : Endec<JsonElement> {
    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: JsonElement) {
        if (serializer is SelfDescribedSerializer<*>) {
            JsonDeserializer.of(value).readAny(ctx, serializer)
            return
        }

        serializer.writeString(ctx, value.toString())
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): JsonElement {
        if (deserializer is SelfDescribedDeserializer<*>) {
            val json: JsonSerializer = JsonSerializer.of()
            deserializer.readAny<JsonElement>(ctx, json)

            return json.result()!!
        }

        return Json.decodeFromString(deserializer.readString(ctx))
    }

    companion object {
        val INSTANCE: JsonEndec = JsonEndec()
    }
}
