package tree.maple.kendec.util

import tree.maple.kendec.Deserializer

/**
 * A template class for implementing deserializers which consume an
 * instance of some recursive data structure (like JSON, NBT or EDM)
 *
 *
 * Importantly, this class also supplies an implementation for [.tryRead]
 * which backs up the decoding frames and restores them upon failure. If this, for some reason,
 * is not the appropriate behavior for your input format, provide a custom implementation
 *
 *
 * Check [tree.maple.kendec.format.edm.EdmDeserializer] for a reference implementation
 */
abstract class RecursiveDeserializer<T> protected constructor(protected val serialized: T) : Deserializer<T> {
    protected val frames: ArrayDeque<() -> T> = ArrayDeque()

    init {
        frames.addFirst({ this.serialized })
    }

    protected val value: T
        /**
         * Get the value currently to be decoded
         *
         *
         * This value is altered by [.frame] and
         * initially returns the entire serialized input
         */
        get() = frames.first()()

    /**
     * Decode the next value down the tree, given by `nextValue`, by pushing that frame
     * onto the decoding stack, invoking `action`, and popping the frame again. Consequently,
     * all decoding of `nextValue` must happen inside `action`
     *
     *
     * If `nextValue` is reading the field of a struct, `isStructField` must be set
     */
    protected fun <V> frame(nextValue: () -> T, action: () -> V): V {
        try {
            frames.addFirst(nextValue)
            return action()
        } finally {
            frames.removeFirst()
        }
    }

    override fun <V> tryRead(reader: (Deserializer<T>) -> V): V {
        val framesBackup = ArrayDeque(this.frames)

        try {
            return reader(this)
        } catch (e: Exception) {
            frames.clear()
            frames.addAll(framesBackup)

            throw e
        }
    }

    protected data class Frame<T>(val source: () -> T, val isStructField: Boolean)
}
