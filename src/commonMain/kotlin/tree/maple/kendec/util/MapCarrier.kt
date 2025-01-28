@file:JsExport
package tree.maple.kendec.util

import tree.maple.kendec.SerializationContext
import tree.maple.kendec.impl.KeyedEndec
import kotlin.js.JsExport
import kotlin.js.JsName

interface MapCarrier {
    /**
     * Get the value stored under `key` in this object's associated map.
     * If no such value exists, the default value of `key` is returned
     *
     *
     * Any exceptions thrown during decoding are propagated to the caller
     */
    @JsName("getWithErrorsWithCtx")
    fun <T> getWithErrors(ctx: SerializationContext, key: KeyedEndec<T>): T {
        throw UnsupportedOperationException("Interface default method called")
    }

    fun <T> getWithErrors(key: KeyedEndec<T>): T {
        return getWithErrors(SerializationContext.empty(), key)
    }

    /**
     * Store `value` under `key` in this object's associated map
     */
    @JsName("putWithCtx")
    fun <T> put(ctx: SerializationContext, key: KeyedEndec<T>, value: T) {
        throw UnsupportedOperationException("Interface default method called")
    }

    fun <T> put(key: KeyedEndec<T>, value: T) {
        put(SerializationContext.empty(), key, value)
    }

    /**
     * Delete the value stored under `key` from this object's associated map,
     * if it is present
     */
    fun <T> delete(key: KeyedEndec<T>) {
        throw UnsupportedOperationException("Interface default method called")
    }

    /**
     * Test whether there is a value stored under `key` in this object's associated map
     */
    fun <T> has(key: KeyedEndec<T>): Boolean {
        throw UnsupportedOperationException("Interface default method called")
    }

    // ---
    /**
     * Get the value stored under `key` in this object's associated map.
     * If no such value exists *or* an exception is thrown during decoding,
     * the default value of `key` is returned
     */
    @JsName("getWithCtx")
    fun <T> get(ctx: SerializationContext, key: KeyedEndec<T>): T {
        return try {
            this.getWithErrors(ctx, key)
        } catch (e: Exception) {
            key.defaultValue()
        }
    }

    fun <T> get(key: KeyedEndec<T>): T {
        return get(SerializationContext.empty(), key)
    }


    fun <T> putIfNotNull(key: KeyedEndec<T>, value: T?) {
        putIfNotNull(SerializationContext.empty(), key, value)
    }

    /**
     * If `value` is not `null`, store it under `key` in this
     * object's associated map
     */
    @JsName("putIfNotNullCtx")
    fun <T> putIfNotNull(ctx: SerializationContext, key: KeyedEndec<T>, value: T?) {
        if (value == null) return
        this.put(ctx, key, value)
    }

    fun <T> copy(key: KeyedEndec<T>, other: MapCarrier) {
        copy(SerializationContext.empty(), key, other)
    }

    /**
     * Store the value associated with `key` in this object's associated map
     * into the associated map of `other` under `key`
     *
     *
     * Importantly, this does not copy the value itself - be careful with mutable types
     */
    @JsName("copyWithCtx")
    fun <T> copy(ctx: SerializationContext, key: KeyedEndec<T>, other: MapCarrier) {
        other.put(ctx, key, this.get(ctx, key))
    }

    fun <T> copyIfPresent(key: KeyedEndec<T>, other: MapCarrier) {
        copyIfPresent(SerializationContext.empty(), key, other)
    }

    /**
     * Like [.copy], but only if this object's associated map
     * has a value stored under `key`
     */
    @JsName("copyIfPresentWithCtx")
    fun <T> copyIfPresent(ctx: SerializationContext, key: KeyedEndec<T>, other: MapCarrier) {
        if (!this.has(key)) return
        this.copy(ctx, key, other)
    }

    fun <T> mutate(key: KeyedEndec<T>, mutator: (T) -> T) {
        mutate(SerializationContext.empty(), key, mutator)
    }

    /**
     * Get the value stored under `key` in this object's associated map, apply
     * `mutator` to it and store the result under `key`
     */
    @JsName("mutateWithCtx")
    fun <T> mutate(ctx: SerializationContext, key: KeyedEndec<T>, mutator: (T) -> T) {
        this.put(ctx, key, mutator(this.get(ctx, key)))
    }
}
