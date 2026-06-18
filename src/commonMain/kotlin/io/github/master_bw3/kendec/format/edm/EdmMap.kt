@file:JsExport

package io.github.master_bw3.kendec.format.edm

import io.github.master_bw3.kendec.*
import io.github.master_bw3.kendec.impl.KeyedEndec
import io.github.master_bw3.kendec.util.MapCarrier
import kotlin.js.JsExport


class EdmMap internal constructor(private val map: MutableMap<String, EdmElement<*>?>) :
    EdmElement<Map<String, EdmElement<*>?>>(
        map.toMap(), Type.MAP
    ), MapCarrier {
    override fun <T> getWithErrors(ctx: SerializationContext, key: KeyedEndec<T>): T {
        if (!this.has(key)) return key.defaultValue()
        return key.endec().decodeFully(
            ctx,
            { serialized: EdmElement<*> ->
                EdmDeserializer.of(serialized) as Deserializer<EdmElement<*>>
            },
            map[key.key()]!!
        )
    }

    override fun <T> put(ctx: SerializationContext, key: KeyedEndec<T>, value: T) {
        map[key.key()] = key.endec().encodeFully<EdmElement<*>>(ctx, { EdmSerializer.of() as Serializer<EdmElement<*>> }, value)
    }

    override fun <T> delete(key: KeyedEndec<T>) {
        map.remove(key.key())
    }

    override fun <T> has(key: KeyedEndec<T>): Boolean {
        return map.containsKey(key.key())
    }
}
