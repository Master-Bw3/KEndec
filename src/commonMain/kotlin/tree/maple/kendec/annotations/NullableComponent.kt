package tree.maple.kendec.annotations


/**
 * Indicates to the [tree.maple.kendec.impl.RecordEndec] that this record component
 * should be treated as nullable in serialization. Importantly, **this changes the serialized type of this
 * component to an optional**
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class NullableComponent
