@file:JsExport

package io.github.master_bw3.kendec.util

import kotlin.js.JsExport

interface Endable : AutoCloseable {
    fun end()

    override fun close() {
        this.end()
    }
}
