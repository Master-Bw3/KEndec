@file:JsExport

package io.github.master_bw3.kendec.util

import io.github.master_bw3.kendec.Endec
import io.github.master_bw3.kendec.SerializationContext
import kotlin.js.JsExport
import kotlin.js.JsName

interface EndecBuffer {
    fun <T> write(endec: Endec<T>?, value: T) {
        write(SerializationContext.empty(), endec, value)
    }

    @JsName("writeWithCtx")
    fun <T> write(ctx: SerializationContext?, endec: Endec<T>?, value: T) {
        throw UnsupportedOperationException()
    }

    fun <T> read(endec: Endec<T>?): T {
        return read(SerializationContext.empty(), endec)
    }

    @JsName("readWithCtx")
    fun <T> read(ctx: SerializationContext?, endec: Endec<T>?): T {
        throw UnsupportedOperationException()
    }
}
