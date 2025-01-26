package tree.maple.kendec.format.gson

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import tree.maple.kendec.Deserializer
import tree.maple.kendec.SerializationContext
import tree.maple.kendec.impl.KeyedEndec
import tree.maple.kendec.util.MapCarrier
import kotlin.jvm.functions.Function1

class GsonMapCarrier(private val `object`: JsonObject) : MapCarrier {
    override fun <T> getWithErrors(ctx: SerializationContext, key: KeyedEndec<T>): T {
        return if (`object`.has(key.key())) key.endec().decodeFully(
            ctx,
            { serialized: JsonElement? ->
                GsonDeserializer.of(serialized)
            },
            `object`[key.key()]
        ) else key.defaultValue()
    }

    override fun <T> put(ctx: SerializationContext, key: KeyedEndec<T>, value: T) {
        `object`.add(key.key(), key.endec().encodeFully(ctx, GsonSerializer::of, value))
    }

    override fun <T> delete(key: KeyedEndec<T>) {
        `object`.remove(key.key())
    }

    override fun <T> has(key: KeyedEndec<T>): Boolean {
        return `object`.has(key.key())
    }
}
