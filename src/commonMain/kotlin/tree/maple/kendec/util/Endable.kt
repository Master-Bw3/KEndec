package tree.maple.kendec.util

interface Endable : AutoCloseable {
    fun end()

    override fun close() {
        this.end()
    }
}
