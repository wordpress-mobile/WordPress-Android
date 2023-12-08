package org.wordpress.android.editor.savedinstance

import android.content.Context
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.parcelize.parcelableCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        db.reset(db.writableDatabase)
    }

    @Test
    fun testStoreAndRetrieve() {
        val parcelId = "testParcelId"
        val parcelData = TestParcelable("testData")
        db.addParcel(parcelId, parcelData)
        val result = db.getParcel(parcelId, parcelableCreator<TestParcelable>())
        assertEquals(parcelData, result)
    }

    @Test
    fun testHasParcel() {
        val parcelId = "testParcelId"
        val parcelData = TestParcelable("testData")
        db.addParcel(parcelId, parcelData)
        val result = db.hasParcel(parcelId)
        assertTrue(result)
    }

    @Test
    fun testHasNoParcel() {
        val parcelId1 = "testParcelId1"
        val parcelId2 = "testParcelId2"
        val parcelData = TestParcelable("testData")
        db.addParcel(parcelId1, parcelData)
        val result = db.hasParcel(parcelId2)
        assertFalse(result)
    }

    @Test
    fun testNullParcel() {
        val parcelId = "testParcelId"
        db.addParcel(parcelId, null)
        val result = db.hasParcel(parcelId)
        assertFalse(result)
    }
}
