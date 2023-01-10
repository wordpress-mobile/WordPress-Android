package org.wordpress.android.provider.query

import android.database.MatrixCursor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    fun `Should get JSON from provided Cursor and return expected object mapped correctly using the provided Type`() {
        val expected = mapOf(
            1 to TestQueryResult("a", 1, true),
            2 to TestQueryResult("b", 2, false)
        )
        val expectedCursor = mock<MatrixCursor>()
        whenever(expectedCursor.getString(0)).thenReturn(Gson().toJson(expected))
        val expectedType = object : TypeToken<Map<Int, TestQueryResult>?>() {}.type
        val actual = classToTest.getValue<Map<Int, TestQueryResult>>(expectedCursor, expectedType)
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

data class TestQueryResult(val testString: String, val testInteger: Int, val testBoolean: Boolean)
