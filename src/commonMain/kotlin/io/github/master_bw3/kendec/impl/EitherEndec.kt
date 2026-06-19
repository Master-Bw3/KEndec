package io.github.master_bw3.kendec.impl

import io.github.master_bw3.kendec.Deserializer
import io.github.master_bw3.kendec.Endec
import io.github.master_bw3.kendec.PrimitiveEndecs
import io.github.master_bw3.kendec.SelfDescribedDeserializer
import io.github.master_bw3.kendec.SelfDescribedSerializer
import io.github.master_bw3.kendec.SerializationContext
import io.github.master_bw3.kendec.Serializer
import io.github.master_bw3.kendec.util.Either


class EitherEndec<L, R>(
    private val leftEndec: Endec<L>,
    private val rightEndec: Endec<R>,
    private val exclusive: Boolean
) : Endec<Either<L, R>> {
    override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: Either<L, R>) {
        if (serializer is SelfDescribedSerializer<*>) {
            value.ifLeft({ left -> this.leftEndec.encode(ctx, serializer, left) })
                .ifRight({ right -> this.rightEndec.encode(ctx, serializer, right) })
        } else {
            serializer.struct().use { struct ->
                struct.field("is_left", ctx, PrimitiveEndecs.BOOLEAN, value.isLeft())
                value.ifLeft({ left -> struct.field("left", ctx, this.leftEndec, left) })
                    .ifRight({ right -> struct.field("right", ctx, this.rightEndec, right) })
            }
        }
    }

    override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): Either<L, R> {
        val selfDescribing = deserializer is SelfDescribedDeserializer<*>

        if (selfDescribing) {
            var leftResult: Either<L, R>? = null
            try {
                leftResult =
                    Either.Left(deserializer.tryRead({ deserializer1 -> this.leftEndec.decode(ctx, deserializer1) }))
            } catch (ignore: Exception) {
            }

            if (!this.exclusive && leftResult != null) return leftResult

            var rightResult: Either<L, R>? = null
            try {
                rightResult =
                    Either.Right(deserializer.tryRead({ deserializer1 -> this.rightEndec.decode(ctx, deserializer1) }))
            } catch (ignore: Exception) {
            }

            check(!(this.exclusive && leftResult != null && rightResult != null)) { "Both alternatives read successfully, can not pick the correct one; first: $leftResult second: $rightResult" }

            if (leftResult != null) return leftResult
            if (rightResult != null) return rightResult

            throw IllegalStateException("Neither alternative read successfully")
        } else {
            val struct = deserializer.struct(ctx)

            return if (struct.field("is_left", ctx, PrimitiveEndecs.BOOLEAN) { false })
                Either.Left(struct.field("left", ctx, this.leftEndec) { error("failed to decode Either") })
            else
                Either.Right(struct.field("right", ctx, this.rightEndec) { error("failed to decode Either") })
        }
    }
}