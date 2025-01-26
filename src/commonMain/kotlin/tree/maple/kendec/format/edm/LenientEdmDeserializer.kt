package tree.maple.kendec.format.edm

import tree.maple.kendec.*
import tree.maple.kendec.util.Optional
import tree.maple.kendec.util.OptionalOf
import tree.maple.kendec.util.OptionalOfEmpty
import tree.maple.kendec.util.OptionalOfNullable

class LenientEdmDeserializer protected constructor(serialized: EdmElement<*>?) : EdmDeserializer(serialized) {
    // ---
    override fun readByte(ctx: SerializationContext): Byte {
        return value!!.cast<Number>().toByte()
    }

    override fun readShort(ctx: SerializationContext): Short {
        return value!!.cast<Number>().toShort()
    }

    override fun readInt(ctx: SerializationContext): Int {
        return value!!.cast<Number>().toInt()
    }

    override fun readLong(ctx: SerializationContext): Long {
        return value!!.cast<Number>().toLong()
    }

    // ---
    override fun readFloat(ctx: SerializationContext): Float {
        return value!!.cast<Number>().toFloat()
    }

    override fun readDouble(ctx: SerializationContext): Double {
        return value!!.cast<Number>().toDouble()
    }

    // ---
    override fun readBoolean(ctx: SerializationContext): Boolean {
        val perhapsNumber = value!!.value()
        if (perhapsNumber is Number) {
            return perhapsNumber.toByte().toInt() == 1
        }

        return super.readBoolean(ctx)
    }

    override fun <V> readOptional(ctx: SerializationContext, endec: Endec<V>): Optional<V & Any> {
        val edmElement = this.value

        return if (edmElement == null) {
            OptionalOfEmpty()
        } else if (edmElement.value() is Optional<*>) {
            super.readOptional(ctx, endec)
        } else {
            OptionalOfNullable(endec.decode(ctx, this))
        }
    }

    //--
    override fun <E> sequence(ctx: SerializationContext, elementEndec: Endec<E>): Deserializer.Sequence<E> {
        val value = value!!.value()

        val list: MutableList<EdmElement<*>>

        if (value is ByteArray) {
            list = ArrayList()

            for (b in value) list.add(EdmElement.Companion.i8(b))
        } else if (value is List<*>) {
            list = this.value!!.cast()
        } else {
            throw IllegalStateException("Unable to handle the given value for sequence within LenientEdmDeserializer!")
        }

        return Sequence<E>(ctx, elementEndec, list)
    }

    companion object {
        fun of(serialized: EdmElement<*>?): LenientEdmDeserializer {
            return LenientEdmDeserializer(serialized)
        }
    }
}
