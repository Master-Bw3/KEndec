package tree.maple.kendec.util

@JsExport
actual class Optional<T> private  constructor(val value: T? = null){

    actual fun get(): T {
        return value ?: throw NullPointerException()
    }

    actual fun isPresent(): Boolean {
        return value != null
    }

    actual fun isEmpty(): Boolean {
        return value == null
    }

    fun <U> map(mapper: (T) -> U): Optional<U> {
        return if (this.value == null) {
            Optional()
        } else {
            Optional(mapper(this.value))
        }
    }

    fun ifPresentOrElse(function: (T) -> Unit, function1: () -> Unit) {
        if (value != null) {
            function(value)
        } else {
            function1()
        }
    }

    fun ifPresent(function: (T) -> Unit) {
        if (value != null) {
            function(value)
        }
    }

    fun orElse(other: T?): T? {
        return value ?: other
    }

    fun orElseGet(other: () -> T): T {
        return value ?: other()
    }

    override fun equals(other: Any?): Boolean {
        return other is Optional<*> && other.value == this.value
    }

    override fun hashCode(): Int {
        return 31 * value.hashCode()
    }

    companion object {

        @JsStatic
        fun <V : Any> of(value: V): Optional<V> {
            return Optional(value)
        }

        @JsStatic
        fun <V> empty(): Optional<V & Any> {
            return Optional()
        }

        @JsStatic
        fun <V> ofNullable(value: V): Optional<V & Any> {
            return if (value == null) {
                Optional()
            } else {
                Optional(value)
            }
        }
    }

}


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
    return Optional.of(value)
}
actual fun <V> OptionalOfEmpty(): Optional<V & Any> {
    return Optional.empty()
}
actual fun <V> OptionalOfNullable(value: V): Optional<V & Any> {
    return  Optional.ofNullable(value)
}