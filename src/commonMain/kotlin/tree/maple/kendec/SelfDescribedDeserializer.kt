@file:JsExport

package tree.maple.kendec

import kotlin.js.JsExport

interface SelfDescribedDeserializer<T> : Deserializer<T> {
    fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>)
}
