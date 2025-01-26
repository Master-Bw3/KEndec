package tree.maple.kendec

interface SelfDescribedDeserializer<T> : Deserializer<T> {
    fun <S> readAny(ctx: SerializationContext, visitor: Serializer<S>)
}
