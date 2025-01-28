@file:JsExport
package tree.maple.kendec.impl

import tree.maple.kendec.Endec
import kotlin.js.JsExport

class KeyedEndec<F>(
    private val key: String,
    private val endec: Endec<F>,
    private val defaultValueFactory: () -> F
) {
    fun key(): String {
        return this.key
    }

    fun endec(): Endec<F> {
        return this.endec
    }

    fun defaultValue(): F {
        return defaultValueFactory()
    }
}
