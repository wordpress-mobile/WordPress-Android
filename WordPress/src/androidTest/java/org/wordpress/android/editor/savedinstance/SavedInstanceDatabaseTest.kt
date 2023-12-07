package org.wordpress.android.editor.savedinstance

import android.content.Context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SavedInstanceDatabaseTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var context: Context

    private lateinit var db: SavedInstanceDatabase

    @Before
    fun setUp() {
        hiltRule.inject()
        db = SavedInstanceDatabase.getDatabase(context)!!
    }

    @Test
    fun testStoreAndRetrieve() {
        val parcelId = "testParcelId"
        val parcelData = TestParcelable("testData")
        db.addParcel(parcelId, parcelData)
        val result = db.getParcel(parcelId, TestParcelable.CREATOR)
        assertEquals(parcelData, result)
    }
}
