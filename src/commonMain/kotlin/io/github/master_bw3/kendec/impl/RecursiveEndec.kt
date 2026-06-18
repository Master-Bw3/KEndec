@file:JsExport

package io.github.master_bw3.kendec.impl

import io.github.master_bw3.kendec.Deserializer
import io.github.master_bw3.kendec.Endec
import io.github.master_bw3.kendec.SerializationContext
import io.github.master_bw3.kendec.Serializer
import kotlin.js.JsExport

class RecursiveEndec<T>(builder: (Endec<T>) -> Endec<T>) :
    Endec<T> {
    val endec: Endec<T> = builder(this)

    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
        endec.encode(ctx, serializer, value)
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
        return endec.decode(ctx, deserializer)
    }
}
