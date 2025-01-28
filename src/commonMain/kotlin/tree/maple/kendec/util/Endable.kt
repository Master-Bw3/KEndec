@file:JsExport
package tree.maple.kendec.util

import kotlin.js.JsExport

interface Endable : AutoCloseable {
    fun end()

    override fun close() {
        this.end()
    }
}
