package tree.maple.kendec.impl

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuidFrom
import tree.maple.kendec.Endec
import tree.maple.kendec.SerializationAttributes

object BuiltInEndecs {
    // --- Java Types ---
    val INT_ARRAY: Endec<IntArray> = Endec.INT.listOf()
        .xmap({ list -> list.toIntArray() }, { ints -> ints.toList() })

    val LONG_ARRAY: Endec<LongArray> = Endec.LONG.listOf()
        .xmap({ list -> list.toLongArray() },
            { longs -> longs.toList() })
    
    val Uuid: Endec<Uuid> = Endec
        .ifAttr(
            SerializationAttributes.HUMAN_READABLE,
            Endec.STRING.xmap({ name: String -> uuidFrom(name) }, { it.toString() })
        ).orElse(
            INT_ARRAY.xmap({ obj: IntArray -> toUuid(obj) }, BuiltInEndecs::toIntArray)
        )

    private fun toUuid(array: IntArray): Uuid {
        return Uuid(
            array[0].toLong() shl 32 or (array[1].toLong() and 4294967295L),
            array[2].toLong() shl 32 or (array[3].toLong() and 4294967295L)
        )
    }

    private fun toIntArray(uuid: Uuid): IntArray {
        return toIntArray(uuid.mostSignificantBits, uuid.leastSignificantBits)
    }

    private fun toIntArray(uuidMost: Long, uuidLeast: Long): IntArray {
        return intArrayOf((uuidMost shr 32).toInt(), uuidMost.toInt(), (uuidLeast shr 32).toInt(), uuidLeast.toInt())
    }

    fun <C, V> vectorEndec(
        name: String,
        componentEndec: Endec<C>,
        constructor: (C, C) -> V,
        xGetter: (V) -> C,
        yGetter: (V) -> C
    ): Endec<V> {
        return componentEndec.listOf()
            .validate(validateSize(name, 2))
            .xmap(
                { components: List<C> -> constructor(components[0], components[1]) },
                { vector: V -> listOf(xGetter(vector), yGetter(vector)) }
            )
    }

    fun <C, V> vectorEndec(
        name: String,
        componentEndec: Endec<C>,
        constructor: StructEndecBuilder.Function3<C, C, C, V>,
        xGetter: (V) -> C,
        yGetter: (V) -> C,
        zGetter: (V) -> C,
    ): Endec<V> {
        return componentEndec.listOf()
            .validate(validateSize(name, 3))
            .xmap(
                { components: List<C> -> constructor.apply(components[0], components[1], components[2]) },
                { vector: V -> listOf(xGetter(vector), yGetter(vector), zGetter(vector)) }
            )
    }

    fun <C, V> vectorEndec(
        name: String,
        componentEndec: Endec<C>,
        constructor: StructEndecBuilder.Function4<C, C, C, C, V>,
        xGetter: (V) -> C,
        yGetter: (V) -> C,
        zGetter: (V) -> C,
        wGetter: (V) -> C,
    ): Endec<V> {
        return componentEndec.listOf()
            .validate(validateSize(name, 4))
            .xmap(
                { components: List<C> ->
                    constructor.apply(
                        components[0],
                        components[1],
                        components[2],
                        components[3]
                    )
                },
                { vector: V ->
                    listOf(
                        xGetter(vector),
                        yGetter(vector),
                        zGetter(vector),
                        wGetter(vector)
                    )
                }
            )
    }

    private fun <C> validateSize(name: String, requiredSize: Int): (List<C>) -> Unit {
        return { collection: List<C> ->
            check(
                collection.size == 4
            ) { name + "collection must have " + requiredSize + " elements" }
        }
    }
}
