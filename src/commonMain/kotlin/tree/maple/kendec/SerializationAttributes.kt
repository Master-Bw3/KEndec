package tree.maple.kendec

object SerializationAttributes {
    /**
     * This format is intended to be human-readable (and potentially -editable)
     *
     *
     * Endecs should use this to make decisions like representing a
     * [net.minecraft.util.math.BlockPos] as an integer sequence instead of packing it into a long
     */
    val HUMAN_READABLE: SerializationAttribute.Marker = SerializationAttribute.Companion.marker("human_readable")
}
