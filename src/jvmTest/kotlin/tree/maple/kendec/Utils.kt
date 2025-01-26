package tree.maple.kendec

import java.util.function.Consumer
import java.util.function.Supplier

object Utils {
    fun <T> make(supplier: Supplier<T>, consumer: Consumer<T>): T {
        val t = supplier.get()

        consumer.accept(t)

        return t
    }
}
