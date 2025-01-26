package tree.maple.kendec.util

class RangeNumberException(val n: Number, val lowerBound: Number?, val upperBound: Number?) : RuntimeException(
    createMsg(
        n, lowerBound, upperBound
    )
) {
    companion object {
        private fun createMsg(n: Number, lowerBound: Number?, upperBound: Number?): String {
            var rangeMessage = ""

            if (lowerBound != null) rangeMessage += ", InclusiveMin: $lowerBound"
            if (upperBound != null) rangeMessage += ", InclusiveMax: $upperBound"

            return "Number value found to be outside allowed bound! [Value: $n$rangeMessage]"
        }
    }
}
