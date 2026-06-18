package io.github.master_bw3.kendec.format.json


import io.github.master_bw3.kendec.SerializationContext
import io.github.master_bw3.kendec.impl.KeyedEndec
import io.github.master_bw3.kendec.util.MapCarrier
import kotlinx.serialization.json.JsonElement
import kotlin.text.get

class JsonMapCarrier(private val `object`: MutableMap<String, JsonElement>) : MapCarrier {
    override fun <T> getWithErrors(ctx: SerializationContext, key: KeyedEndec<T>): T {
        return if (`object`.containsKey(key.key())) key.endec().decodeFully(
            ctx,
            { serialized: JsonElement? ->
                JsonDeserializer.of(serialized)
            },
            `object`[key.key()]
        ) else key.defaultValue()
    }

    override fun <T> put(ctx: SerializationContext, key: KeyedEndec<T>, value: T) {
        key.endec().encodeFully(ctx, JsonSerializer::of, value)?.let { `object`.put(key.key(), it) }
    }

    override fun <T> delete(key: KeyedEndec<T>) {
        `object`.remove(key.key())
    }

    override fun <T> has(key: KeyedEndec<T>): Boolean {
        return `object`.containsKey(key.key())
    }
}
