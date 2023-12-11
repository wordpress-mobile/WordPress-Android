package org.wordpress.android.editor.savedinstance

import android.os.Parcelable
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class ParcelableObjectTest {
    private lateinit var parcelable: Parcelable

    @Before
    fun setUp() {
        parcelable = TestParcelable("testData")
    }

    @Test
    fun testConstructorWithParcelable() {
        val parcelableObject = ParcelableObject(parcelable)
        val parcelData = parcelableObject.toBytes()
        val objectFromParcel = ParcelableObject(parcelData)
        assertArrayEquals(objectFromParcel.toBytes(), parcelData)
    }

    @Test
    fun testConstructorWithByteArray() {
        val parcelableObject = ParcelableObject(parcelable)
        val data = parcelableObject.toBytes()
        val parcelableResult = ParcelableObject(data)
        assertArrayEquals(parcelableObject.toBytes(), parcelableResult.toBytes())
    }

    @Test
    fun getParcelReturnsTheSameParcelObject() {
        val parcelableObject = ParcelableObject(parcelable)
        val initialParcel = parcelableObject.getParcel()
        val nextParcel = parcelableObject.getParcel()
        assertEquals(initialParcel, nextParcel)
    }
}
