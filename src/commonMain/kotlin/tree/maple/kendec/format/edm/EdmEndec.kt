@file:JsExport
package tree.maple.kendec.format.edm

import okio.Buffer
import okio.IOException
import tree.maple.kendec.*
import kotlin.js.JsExport


class EdmEndec private constructor() : Endec<EdmElement<*>> {
    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: EdmElement<*>) {
        if (serializer is SelfDescribedSerializer<*>) {
            EdmDeserializer(value).readAny(ctx, serializer)
            return
        }

        try {
            val output = Buffer()
            EdmIo.encode(output, value)

            serializer.writeBytes(ctx, output.readByteArray())
        } catch (e: IOException) {
            throw RuntimeException("Failed to encode EDM element in EdmEndec", e)
        }
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): EdmElement<*> {
        if (deserializer is SelfDescribedDeserializer<*>) {
            val nativeSerializer = EdmSerializer()
            deserializer.readAny(ctx, nativeSerializer)

            return nativeSerializer.result()!!
        }

        try {
            return EdmIo.decode(Buffer().write(deserializer.readBytes(ctx))
            )
        } catch (e: IOException) {
            throw RuntimeException("Failed to parse EDM element in EdmEndec", e)
        }
    }

    companion object {
        val INSTANCE: EdmEndec = EdmEndec()
        val MAP: Endec<EdmMap> =
            INSTANCE.xmap({ obj: EdmElement<*> -> obj.asMap() }, { edmMap: EdmMap -> edmMap })
    }
}
