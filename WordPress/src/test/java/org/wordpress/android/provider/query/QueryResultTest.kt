package org.wordpress.android.provider.query

import android.database.MatrixCursor
import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Test

class QueryResultTest {
    private val classToTest = QueryResult()

    @Test
    fun `Should get JSON from provided Cursor and return expected object mapped correctly`() {
        val expected = arrayListOf("a", "b", "c")
        val expectedCursor = mock<MatrixCursor>()
        whenever(expectedCursor.getString(0)).thenReturn(Gson().toJson(expected))
        val actual = classToTest.getValue<ArrayList<String>>(expectedCursor)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `Should return null if failed to map object JSON from provided Cursor`() {
        val expected = null
        val expectedCursor = mock<MatrixCursor>()
        whenever(expectedCursor.getString(0)).thenReturn(Gson().toJson(expected))
        val actual = classToTest.getValue<ArrayList<String>>(expectedCursor)
        Assert.assertEquals(expected, actual)
    }
}
