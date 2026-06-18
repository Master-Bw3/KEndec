@file:JsExport

package io.github.master_bw3.kendec

import kotlin.js.JsExport

interface SelfDescribedDeserializer<T> : Deserializer<T> {
    fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>)
}
