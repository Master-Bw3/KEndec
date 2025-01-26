package tree.maple.kendec.util

actual typealias Optional<T> = java.util.Optional<T>

actual fun <T> Optional<T>.get(): T = this.get()

actual fun <T, U : Any> Optional<T>.map(mapper: (T) -> U): Optional<U> {
    return this.map(mapper)
}

actual fun <T> Optional<T>.ifPresentOrElse(function: (T) -> Unit, function1: () -> Unit) {
    this.ifPresentOrElse(function, function1)
}

actual fun <T> Optional<T>.ifPresent(function: (T) -> Unit) {
    this.ifPresent(function)
}

actual fun <T> Optional<T>.orElse(other: T?): T? {
    return this.orElse(other)
}

actual fun <T> Optional<T>.orElseGet(other: () -> T): T {
    return this.orElseGet(other)
}

actual fun <V : Any> OptionalOf(value: V): Optional<V> {
    return java.util.Optional.of(value)
}

actual fun <V> OptionalOfEmpty(): Optional<V & Any> {
    return java.util.Optional.empty<V>()
}

actual fun <V> OptionalOfNullable(value: V): Optional<V & Any> {
    return java.util.Optional.ofNullable(value)
}