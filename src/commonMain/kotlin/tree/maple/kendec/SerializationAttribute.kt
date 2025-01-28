@file:JsExport


package tree.maple.kendec

import kotlin.js.JsExport
import kotlin.js.JsStatic

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

        @JsStatic
        fun marker(name: String): Marker {
            return Marker(name)
        }

        @JsStatic
        fun <T> withValue(name: String): WithValue<T> {
            return WithValue(name)
        }
    }
}
