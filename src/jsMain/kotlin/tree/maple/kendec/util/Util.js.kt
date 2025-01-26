package tree.maple.kendec.util

import kotlin.reflect.KClass

internal actual fun <E : Enum<E>> getEnumConstants(enum: KClass<E>): Array<E>  {
    throw UnsupportedOperationException()
}