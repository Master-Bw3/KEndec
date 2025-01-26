package tree.maple.kendec


object Utils {
    fun <T> make(supplier: () -> T, consumer: (T) -> Unit): T {
        val t = supplier()

        consumer(t)

        return t
    }
}
