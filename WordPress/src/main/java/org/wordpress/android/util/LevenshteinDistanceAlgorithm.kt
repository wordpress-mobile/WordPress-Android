package org.wordpress.android.util

import javax.inject.Inject

/**
 * This class provides an implementation of the Levenshtein distance algorithm which is
 * a string metric for measuring the difference between two sequences. It is calculated
 * as the minimum number of single-character edits (insertions, deletions or substitutions)
 * required to change one word into the other.
 *
 * This implementation includes a similarity function that calculates a normalized score
 * indicating how similar two strings are, on a scale from 0 to 1, where 1 is an exact match.
 */
class LevenshteinDistanceAlgorithm @Inject constructor() {
    /**
     * Calculates the similarity between two strings based on the Levenshtein distance.
     * The result is a double value between 0 and 1, where 1 means the strings are identical.
     *
     * @param first The first string to be compared.
     * @param second The second string to be compared.
     * @return A double value representing the similarity score.
     */
    fun levenshteinSimilarity(first: String, second: String): Double {
        val firstLength = first.length
        val secondLength = second.length
        val threshold = if (firstLength > secondLength) {
            (second.length.toDouble() / first.length.toDouble()).coerceAtMost(0.8)
        } else {
            (first.length.toDouble() / second.length.toDouble()).coerceAtMost(0.8)
        }
        if (first.isBlank() && second.isBlank()) return 0.0
        val levenshteinDistance = levenshtein(first, second)
        val maxLength = maxOf(first.length, second.length)
        val similarity = (maxLength - levenshteinDistance) / maxLength.toDouble()
        return if (similarity >= threshold) similarity else 0.0
    }

    /**
     * Calculates the Levenshtein distance between two character sequences.
     * It is a measure of the number of single-character edits required to change one sequence into the other.
     *
     * @param leftHandSide The first character sequence.
     * @param rightHandSide The second character sequence.
     * @return The computed Levenshtein distance as an integer.
     */
    private fun levenshtein(leftHandSide: CharSequence, rightHandSide: CharSequence): Int {
        val leftHandSideLength = leftHandSide.length
        val rightHandSideLength = rightHandSide.length

        var cost = IntArray(leftHandSideLength + 1) { it }
        var newCost = IntArray(leftHandSideLength + 1) { 0 }

        for (i in 1..rightHandSideLength) {
            newCost[0] = i

            for (j in 1..leftHandSideLength) {
                val match = if (leftHandSide[j - 1] == rightHandSide[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[leftHandSideLength]
    }
}
