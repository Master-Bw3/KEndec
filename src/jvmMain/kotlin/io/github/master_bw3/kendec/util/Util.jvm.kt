package io.github.master_bw3.kendec.util

import kotlin.reflect.KClass

internal actual fun <E : Enum<E>> getEnumConstants(enum: KClass<E>): Array<E> {
    return enum.java.enumConstants
}