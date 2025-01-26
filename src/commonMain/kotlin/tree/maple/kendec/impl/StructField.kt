package tree.maple.kendec.impl

import tree.maple.kendec.*
import kotlin.jvm.JvmOverloads

open class StructField<S, F> @JvmOverloads constructor(
    protected val name: String,
    protected val endec: Endec<F>,
    protected val getter: (S) -> F,
    defaultValueFactory: (() -> F)? = null
) {
    protected val defaultValueFactory: (() -> F)?

    init {
        this.defaultValueFactory = defaultValueFactory
    }


    open fun encodeField(
        ctx: SerializationContext,
        serializer: Serializer<*>,
        struct: Serializer.Struct,
        instance: S
    ) {
        try {
            struct.field(
                this.name, ctx, this.endec,
                getter(instance), this.defaultValueFactory != null
            )
        } catch (e: Exception) {
            throw StructFieldException(
                "Exception occurred when encoding a given StructField: [Field: " + this.name + "]",
                e
            )
        }
    }

    open fun decodeField(ctx: SerializationContext, deserializer: Deserializer<*>, struct: Deserializer.Struct): F {
        try {
            return struct.field(this.name, ctx, this.endec, this.defaultValueFactory) ?: throw NullPointerException()
        } catch (e: Exception) {
            throw StructFieldException(
                "Exception occurred when decoding a given StructField: [Field: " + this.name + "]",
                e
            )
        }
    }

    class Flat<S, F>(endec: StructEndec<F>, getter: (S) -> F) :
        StructField<S, F>("", endec, getter, null) {
        private fun endec(): StructEndec<F> {
            return endec as StructEndec<F>
        }

        override fun encodeField(
            ctx: SerializationContext,
            serializer: Serializer<*>,
            struct: Serializer.Struct,
            instance: S
        ) {
            endec().encodeStruct(ctx, serializer, struct, getter(instance))
        }

        override fun decodeField(
            ctx: SerializationContext,
            deserializer: Deserializer<*>,
            struct: Deserializer.Struct
        ): F {
            return endec().decodeStruct(ctx, deserializer, struct)
        }
    }

    class StructFieldException(message: String?, cause: Throwable?) : IllegalStateException(message, cause)
}
