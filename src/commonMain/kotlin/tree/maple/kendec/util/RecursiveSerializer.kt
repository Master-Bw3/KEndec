package tree.maple.kendec.util

import tree.maple.kendec.Serializer
import kotlin.js.JsName

/**
 * A template class for implementing serializers which produce as result an
 * instance of some recursive data structure (like JSON, NBT or EDM)
 *
 *
 * Check [EdmSerializer] for a reference implementation
 */
abstract class RecursiveSerializer<T> protected constructor(@JsName("_result") protected var result: T) : Serializer<T> {
    protected val frames: ArrayDeque<(T) -> Unit> = ArrayDeque()

    init {
        frames.addFirst({ t: T -> this.result = t })
    }

    /**
     * Store `value` into the current encoding location
     *
     *
     * This location is altered by [.frame] and
     * initially is just the serializer's result directly
     */
    protected fun consume(value: T) {
        frames.first()(value)
    }

    /**
     * Encode the next value down the tree by pushing a new frame
     * onto the encoding stack and invoking `action`
     *
     *
     * `action` receives `encoded`, which is where the next call
     * to [.consume] (which `action` must somehow cause) will
     * store the value and allow `action` to retrieve it using [EncodedValue.value]
     * or, preferably, [EncodedValue.require]
     */
    protected fun frame(action: FrameAction<T>) {
        val encoded = EncodedValue<T>()

        frames.addFirst({ value: T -> encoded.set(value) })
        action.accept(encoded)
        frames.removeFirst()
    }

    override fun result(): T {
        return this.result
    }

    protected fun interface FrameAction<T> {
        fun accept(encoded: EncodedValue<T>)
    }

    protected class EncodedValue<T> {
        private var value: T? = null
        private var encoded = false

        fun set(value: T) {
            this.value = value
            this.encoded = true
        }

        fun value(): T? {
            return this.value
        }

        fun wasEncoded(): Boolean {
            return this.encoded
        }

        fun require(name: String): T {
            check(this.encoded) { "Endec for $name serialized nothing" }
            return this.value()!!
        }
    }
}
