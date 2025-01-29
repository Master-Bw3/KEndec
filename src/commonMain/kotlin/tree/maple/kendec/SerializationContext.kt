@file:JsExport


package tree.maple.kendec

import tree.maple.kendec.SerializationAttribute.WithValue
import tree.maple.kendec.impl.MissingAttributeValueException
import kotlin.js.JsExport
import kotlin.js.JsStatic

class SerializationContext private constructor(
    attributeValues: Map<SerializationAttribute, Any?>,
    suppressedAttributes: Set<SerializationAttribute>
) {
    private val attributeValues: Map<SerializationAttribute, Any?> = attributeValues.toMap()
    private val suppressedAttributes: Set<SerializationAttribute> = suppressedAttributes.toSet()

    fun withAttributes(vararg attributes: SerializationAttribute.Instance): SerializationContext {
        val newAttributes = unpackAttributes(attributes)
        attributeValues.forEach { (attribute: SerializationAttribute, value: Any?) ->
            if (!newAttributes.containsKey(attribute)) {
                newAttributes[attribute] = value
            }
        }

        return SerializationContext(newAttributes, this.suppressedAttributes)
    }

    fun withoutAttributes(vararg attributes: SerializationAttribute?): SerializationContext {
        val newAttributes = HashMap(this.attributeValues)
        for (attribute in attributes) {
            newAttributes.remove(attribute)
        }

        return SerializationContext(newAttributes, this.suppressedAttributes)
    }

    fun withSuppressed(vararg attributes: SerializationAttribute): SerializationContext {
        val newSuppressed = HashSet(this.suppressedAttributes)
        newSuppressed.addAll(attributes.toList())

        return SerializationContext(this.attributeValues, newSuppressed)
    }

    fun withoutSuppressed(vararg attributes: SerializationAttribute): SerializationContext {
        val newSuppressed = HashSet(this.suppressedAttributes)
        for (attribute in attributes) {
            newSuppressed.remove(attribute)
        }

        return SerializationContext(this.attributeValues, newSuppressed)
    }

    fun and(other: SerializationContext): SerializationContext {
        val newAttributeValues = HashMap(this.attributeValues)
        newAttributeValues.putAll(other.attributeValues)

        val newSuppressed = HashSet(this.suppressedAttributes)
        newSuppressed.addAll(other.suppressedAttributes)

        return SerializationContext(newAttributeValues, newSuppressed)
    }

    fun hasAttribute(attribute: SerializationAttribute): Boolean {
        return attributeValues.containsKey(attribute) && !suppressedAttributes.contains(attribute)
    }

    fun <A> getAttributeValue(attribute: WithValue<A>): A? {
        return attributeValues[attribute] as A?
    }

    fun <A> requireAttributeValue(attribute: WithValue<A>): A? {
        if (!this.hasAttribute(attribute)) {
            throw MissingAttributeValueException("Context did not provide a value for attribute '" + attribute.name + "'")
        }

        return this.getAttributeValue(attribute)
    }

    companion object {
        private val EMPTY = SerializationContext(mapOf(), setOf())

        @JsStatic
        fun empty(): SerializationContext {
            return EMPTY
        }

        @JsStatic
        fun attributes(vararg attributes: SerializationAttribute.Instance): SerializationContext {
            if (attributes.isEmpty()) return EMPTY
            return SerializationContext(unpackAttributes(attributes), setOf())
        }

        @JsStatic
        fun suppressed(vararg attributes: SerializationAttribute): SerializationContext {
            if (attributes.isEmpty()) return EMPTY
            return SerializationContext(mapOf(), attributes.toSet())
        }

        private fun unpackAttributes(attributes: Array<out SerializationAttribute.Instance>): MutableMap<SerializationAttribute, Any?> {
            val attributeValues = HashMap<SerializationAttribute, Any?>()
            for (instance in attributes) {
                attributeValues[instance.attribute()] = instance.value()
            }

            return attributeValues
        }
    }
}
