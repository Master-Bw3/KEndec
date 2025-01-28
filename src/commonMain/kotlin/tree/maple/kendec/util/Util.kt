@file:JsExport

package tree.maple.kendec.util

import kotlin.js.JsExport
import kotlin.reflect.KClass

internal expect fun <E: Enum<E>> getEnumConstants(enum: KClass<E>): Array<E> ;