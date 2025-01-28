package tree.maple.kendec.util


expect class Optional<T> {


    fun isPresent(): Boolean
    fun isEmpty(): Boolean

    fun get(): T

}

expect fun <T, U : Any> Optional<T>.map(mapper: (T) -> U): Optional<U>
expect fun <T> Optional<T>.ifPresentOrElse(function: (T) -> Unit, function1: () -> Unit)
expect fun <T> Optional<T>.ifPresent(function: (T) -> Unit)
expect fun <T> Optional<T>.orElse(other: T?): T?
expect fun <T> Optional<T>.orElseGet(other: () -> T): T

expect fun <V : Any> OptionalOf(value: V): Optional<V>
expect fun <V> OptionalOfEmpty(): Optional<V & Any>
expect fun <V> OptionalOfNullable(value: V): Optional<V & Any>