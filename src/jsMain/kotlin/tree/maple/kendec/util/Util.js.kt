package tree.maple.kendec.util

import okio.Buffer
import kotlin.reflect.KClass

internal actual fun <E : Enum<E>> getEnumConstants(enum: KClass<E>): Array<E>  {
    throw UnsupportedOperationException()
}

@OptIn(ExperimentalUnsignedTypes::class)
@JsExport
fun createBuffer(bytes: ByteArray? = null): Buffer {
    return if(bytes == null) Buffer() else Buffer().write(bytes)
}

@JsExport
fun writeByte(buffer: Buffer, byte: Byte): Buffer {
    return buffer.writeByte(byte.toInt())
}

@JsExport
fun readByte(buffer: Buffer): Byte {
    return buffer.readByte()
}

@JsExport
fun toBytes(buffer: Buffer): ByteArray {
    return buffer.readByteArray()
}

@JsExport
fun <T> listOf(array: Array<T>): List<T> {
    return array.toList()
}

@JsExport
fun mapOf(entries: Array<Array<Any>>): Map<Any, Any> {
    return entries.map{ it[0] to it[1] }.toMap()
}
