@file:JsExport

package tree.maple.kendec.util

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.reflect.KClass

internal expect fun <E: Enum<E>> getEnumConstants(enum: KClass<E>): Array<E>;

@JsName("uuidToString")
fun Uuid.toString(): String {
    return this.toString()
}

fun uuidFromString(string: String): Uuid {
    return uuidFrom(string)
}
