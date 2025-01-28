@file:JsExport
package tree.maple.kendec.util

import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.jvm.JvmOverloads
import kotlin.math.max

class BlockWriter {
    private val result = StringBuilder()
    private val blocks: ArrayDeque<Pair<Boolean, String>> = ArrayDeque()

    private var indentLevel = 0

    fun write(value: String): BlockWriter {
        result.append(value)

        return this
    }


    @JsName("writeEmptyln")
    fun writeln(): BlockWriter {
        this.writeln("")

        return this
    }

    fun writeln(value: String): BlockWriter {
        result.append(
            value + "\n" + ("  ".repeat(
                max(0.0, indentLevel.toDouble()).toInt()
            ))
        )

        return this
    }

    @JvmOverloads
    fun startBlock(startDelimiter: String, endDelimiter: String, incrementIndentation: Boolean = true): BlockWriter {
        if (incrementIndentation) {
            indentLevel++
            this.writeln(startDelimiter)
        } else {
            this.write(startDelimiter)
        }

        blocks.addLast(incrementIndentation to endDelimiter)

        return this
    }

    @JsName("writeIndentedBlock")
    fun writeBlock(startDelimiter: String, endDelimiter: String, consumer: (BlockWriter) -> Unit): BlockWriter {
        return writeBlock(startDelimiter, endDelimiter, true, consumer)
    }

    fun writeBlock(
        startDelimiter: String,
        endDelimiter: String,
        incrementIndentation: Boolean,
        consumer: (BlockWriter) -> Unit
    ): BlockWriter {
        this.startBlock(startDelimiter, endDelimiter, incrementIndentation)

        consumer(this)

        this.endBlock()

        return this
    }

    fun endBlock(): BlockWriter {
        val endBlockData = blocks.removeLast()

        if (endBlockData.first) {
            indentLevel--
            writeln()
        }

        write(endBlockData.second)

        return this
    }

    fun buildResult(): String {
        return result.toString()
    }
}
