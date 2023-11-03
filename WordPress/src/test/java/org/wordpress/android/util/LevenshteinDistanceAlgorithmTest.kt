package org.wordpress.android.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

private const val ASSERTION_DELTA = 0.1

class LevenshteinDistanceAlgorithmTest {
    private val algorithm = LevenshteinDistanceAlgorithm()


    @Test
    fun `GIVEN identical strings WHEN levenshteinSimilarity THEN return 1`() {
        assertEquals(1.0, algorithm.levenshteinSimilarity("example.com", "example.com"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN one character off WHEN levenshteinSimilarity THEN return 0_9`() {
        assertEquals(0.9, algorithm.levenshteinSimilarity("example.com", "exzmple.com"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN completely different domains WHEN levenshteinSimilarity THEN return 0_15`() {
        assertEquals(0.15, algorithm.levenshteinSimilarity("example.net", "different.com"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN same length but different characters WHEN levenshteinSimilarity THEN return 0`() {
        assertEquals(0.0, algorithm.levenshteinSimilarity("aaaaaa", "bbbbbb"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN empty strings WHEN levenshteinSimilarity THEN return 0`() {
        assertEquals(0.0, algorithm.levenshteinSimilarity("", ""), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN empty and non-empty string WHEN levenshteinSimilarity THEN return 0`() {
        assertEquals(0.0, algorithm.levenshteinSimilarity("", "nonempty"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN similar strings WHEN levenshteinSimilarity THEN return 0_82`() {
        assertEquals(0.82, algorithm.levenshteinSimilarity("example", "examp1e"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN string and its prefix WHEN levenshteinSimilarity THEN return 0_5`() {
        assertEquals(0.5, algorithm.levenshteinSimilarity("example", "exam"), ASSERTION_DELTA)
    }

    @Test
    fun `GIVEN string and its suffix WHEN levenshteinSimilarity THEN return 0_71`() {
        assertEquals(0.71, algorithm.levenshteinSimilarity("example", "ample"), ASSERTION_DELTA)
    }
}
