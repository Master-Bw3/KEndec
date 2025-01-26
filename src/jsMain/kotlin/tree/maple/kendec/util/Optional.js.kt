package tree.maple.kendec.util

actual class Optional<T> {
    actual fun isPresent(): Boolean {
        TODO()
    }

    actual fun isEmpty(): Boolean {
        TODO()
    }
}

actual fun <T> Optional<T>.get(): T {
    TODO()
}
actual fun <T, U : Any> Optional<T>.map(mapper: (T) -> U): Optional<U> {
    TODO()
}
actual fun <T> Optional<T>.ifPresentOrElse(function: (T) -> Unit, function1: () -> Unit) {
    TODO()
}
actual fun <T> Optional<T>.ifPresent(function: (T) -> Unit) {
    TODO()
}
actual fun <T> Optional<T>.orElse(other: T?): T? {
    TODO()
}
actual fun <T> Optional<T>.orElseGet(other: () -> T): T {
    TODO()
}

actual fun <V : Any> OptionalOf(value: V): Optional<V> {
    TODO()
}
actual fun <V> OptionalOfEmpty(): Optional<V> {
    TODO()
}
actual fun <V> OptionalOfNullable(value: V): Optional<V> {
    TODO()
}