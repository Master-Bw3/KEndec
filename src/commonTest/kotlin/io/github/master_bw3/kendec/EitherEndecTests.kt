package io.github.master_bw3.kendec

import io.github.master_bw3.kendec.format.json.JsonDeserializer
import io.github.master_bw3.kendec.format.json.JsonSerializer
import io.github.master_bw3.kendec.impl.EitherEndec
import io.github.master_bw3.kendec.util.Either
import kotlin.test.Test
import kotlin.test.assertTrue

class EitherEndecTests {

    @Test
    fun leftEitherTest() {
        val leftValue = "the_thing"
        val element = Either.Left(leftValue)
        val elementEndec = EitherEndec(PrimitiveEndecs.STRING, PrimitiveEndecs.INT, true)

        val serialized = elementEndec.encodeFully(JsonSerializer::of, element)
        val deserialized = elementEndec.decodeFully(JsonDeserializer::of, serialized)

        assertTrue { deserialized.isLeft() }
        assertTrue { deserialized.fold({ it }, { it }) == leftValue }
    }


    @Test
    fun rightEitherTest() {
        val rightValue = "the_thing"
        val element = Either.Right(rightValue)
        val elementEndec = EitherEndec(PrimitiveEndecs.INT, PrimitiveEndecs.STRING, true)

        val serialized = elementEndec.encodeFully(JsonSerializer::of, element)
        val deserialized = elementEndec.decodeFully(JsonDeserializer::of, serialized)

        assertTrue { deserialized.isRight() }
        assertTrue { deserialized.fold({ it }, { it }) == rightValue }
    }
}