@file:JsExport


package tree.maple.kendec

import kotlin.js.JsExport

interface SelfDescribedSerializer<T> : Serializer<T> {
    /**
     * Discriminator for use inside typescript
     */
    val isSelfDescribedDescribedSerializer: Boolean
        get() = true
}
