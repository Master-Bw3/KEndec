package io.github.master_bw3.kendec.util


sealed class Either<out L, out R> {

    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()

    inline fun <T> fold(
        onLeft: (L) -> T,
        onRight: (R) -> T
    ): T = when (this) {
        is Left -> onLeft(value)
        is Right -> onRight(value)
    }

    fun isLeft(): Boolean = this is Left

    fun isRight(): Boolean = this is Right

    inline fun ifLeft(action: (L) -> Unit): Either<L, R> {
        if (this is Left) action(value)
        return this
    }

    inline fun ifRight(action: (R) -> Unit): Either<L, R> {
        if (this is Right) action(value)
        return this
    }
}