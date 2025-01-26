package tree.maple.kendec

abstract class SerializationAttribute protected constructor(val name: String) {
    class Marker(name: String) : SerializationAttribute(name), Instance {
        override fun attribute(): SerializationAttribute {
            return this
        }

        override fun value(): Any? {
            return null
        }
    }

    class WithValue<T>(name: String) : SerializationAttribute(name) {
        fun instance(value: T): Instance {
            return object : Instance {
                override fun attribute(): SerializationAttribute {
                    return this@WithValue
                }

                override fun value(): Any? {
                    return value
                }
            }
        }
    }

    interface Instance {
        fun attribute(): SerializationAttribute
        fun value(): Any?
    }

    companion object {
        fun marker(name: String): Marker {
            return Marker(name)
        }

        fun <T> withValue(name: String): WithValue<T> {
            return WithValue(name)
        }
    }
}
