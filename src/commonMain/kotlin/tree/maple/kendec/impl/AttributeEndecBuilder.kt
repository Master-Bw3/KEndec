package tree.maple.kendec.impl

import tree.maple.kendec.*


class AttributeEndecBuilder<T>(endec: Endec<T>, attribute: SerializationAttribute) {
    private val branches: MutableMap<SerializationAttribute, Endec<T>> = LinkedHashMap()

    init {
        branches[attribute] = endec
    }

    fun orElseIf(endec: Endec<T>, attribute: SerializationAttribute): AttributeEndecBuilder<T> {
        return orElseIf(attribute, endec)
    }

    fun orElseIf(attribute: SerializationAttribute, endec: Endec<T>): AttributeEndecBuilder<T> {
        check(!branches.containsKey(attribute)) { "Cannot have more than one branch for attribute " + attribute.name }

        branches[attribute] = endec
        return this
    }

    fun orElse(endec: Endec<T>): Endec<T> {
        return object : Endec<T> {
            override fun encode(ctx: SerializationContext, serializer: Serializer<*>, value: T) {
                var branchEndec = endec

                for ((key, value1) in this@AttributeEndecBuilder.branches) {
                    if (ctx.hasAttribute(key)) {
                        branchEndec = value1
                        break
                    }
                }

                branchEndec.encode(ctx, serializer, value)
            }

            override fun decode(ctx: SerializationContext, deserializer: Deserializer<*>): T {
                var branchEndec = endec

                for ((key, value) in this@AttributeEndecBuilder.branches) {
                    if (ctx.hasAttribute(key)) {
                        branchEndec = value
                        break
                    }
                }

                return branchEndec.decode(ctx, deserializer)
            }
        }
    }
}
