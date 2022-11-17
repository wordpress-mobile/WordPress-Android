package org.wordpress.android.resolver

import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.QuickStartTaskModel
import org.wordpress.android.fluxc.model.SiteModel

class ResolverUtilityTest : BaseUnitTest() {
    private val dbWrapper: DbWrapper = mock()
    private val sqliteDatabase: SQLiteDatabase = mock()
    private val sqliteStatement: SQLiteStatement = mock()
    private val resolverUtility = ResolverUtility(
            dbWrapper = dbWrapper
    )

    @Before
    fun setup() {
        whenever(dbWrapper.giveMeWritableDb()).thenReturn(sqliteDatabase)
        whenever(sqliteDatabase.compileStatement(anyString())).thenReturn(sqliteStatement)
    }

    @Test
    fun `Transaction is completed succesfully when no errors`() {
        resolverUtility.copySitesWithIndexes(sites = listOf(SiteModel(), SiteModel()))
        verify(sqliteDatabase, times(1)).beginTransaction()
        verify(sqliteDatabase, times(1)).setTransactionSuccessful()
        verify(sqliteDatabase, times(1)).endTransaction()
    }


    @Test(expected = SQLException::class)
    fun `Transaction is not completed on exception`() {
        whenever(sqliteStatement.execute()).thenThrow(SQLException("Error"))
        resolverUtility.copySitesWithIndexes(sites = listOf(SiteModel(), SiteModel()))
        verify(sqliteDatabase, times(1)).beginTransaction()
        verify(sqliteDatabase, never()).setTransactionSuccessful()
        verify(sqliteDatabase, times(1)).endTransaction()
    }

    @Test
    fun `SiteModel table is deleted`() {
        resolverUtility.copySitesWithIndexes(sites = listOf(
                SiteModel(),
                SiteModel(),
                SiteModel()
        ))
        verify(sqliteDatabase, times(1)).delete(
                "SiteModel", null, null
        )
    }

    @Test
    fun `SiteModel index is deleted`() {
        resolverUtility.copySitesWithIndexes(sites = listOf(
                SiteModel(),
                SiteModel(),
                SiteModel()
        ))
        verify(sqliteDatabase, times(1)).delete(
                "sqlite_sequence", "name='SiteModel'", null
        )
    }

    @Test
    fun `Autoincrement sequence is inserted once and then updated`() {
        resolverUtility.copySitesWithIndexes(sites = listOf(
                SiteModel().apply { id = 10 },
                SiteModel().apply { id = 8 },
                SiteModel().apply { id = 20 }
        ))

        verify(sqliteDatabase, times(1)).compileStatement(
                "INSERT INTO SQLITE_SEQUENCE (name,seq) VALUES ('SiteModel', ?)"
        )
        verify(sqliteDatabase, times(2)).compileStatement(
                "UPDATE SQLITE_SEQUENCE SET seq=? WHERE name='SiteModel'"
        )
    }

    @Test(expected = SQLException::class)
    fun `copyQsDataWithIndexes fails if the copy of one table fails`() {
        whenever(sqliteStatement.execute()).thenThrow(SQLException("Error"))
        val result = resolverUtility.copyQsDataWithIndexes(listOf(), listOf(QuickStartTaskModel()))
        assertThat(result).isFalse()
    }
}
