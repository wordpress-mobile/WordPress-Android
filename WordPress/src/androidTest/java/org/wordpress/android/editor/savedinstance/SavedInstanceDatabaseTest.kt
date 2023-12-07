package org.wordpress.android.editor.savedinstance

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
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

    data class TestParcelable(val data: String) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readString()!!)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(data)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<TestParcelable> {
            override fun createFromParcel(parcel: Parcel): TestParcelable {
                return TestParcelable(parcel)
            }

            override fun newArray(size: Int): Array<TestParcelable?> {
                return arrayOfNulls(size)
            }
        }
    }
}
