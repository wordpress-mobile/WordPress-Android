package org.wordpress.android.util

import android.content.Context
import android.widget.ImageView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R

@RunWith(MockitoJUnitRunner::class)
class MediaPickerUtilsTest {
    @Mock
    lateinit var imageThumbnail: ImageView

    @Mock
    lateinit var context: Context

    @Before
    fun setUp() {
        whenever(imageThumbnail.context).thenReturn(context)
    }

    @Test
    fun `announces video selected when item is selected and is video`() {
        // Arrange
        val expectedString = "Video selected"
        whenever(context.getString(R.string.photo_picker_video_thumbnail_selected))
            .thenReturn(expectedString)

        // Act
        MediaPickerUtils.announceSelectedMediaForAccessibility(
            imageThumbnail,
            true, // isVideo
            true  // itemSelected
        )

        // Assert
        verify(context).getString(R.string.photo_picker_video_thumbnail_selected)
        verify(imageThumbnail).announceForAccessibility(expectedString)
    }

    @Test
    fun `announces image selected when item is selected and is not video`() {
        // Arrange
        val expectedString = "Image selected"
        whenever(context.getString(R.string.photo_picker_image_thumbnail_selected))
            .thenReturn(expectedString)

        // Act
        MediaPickerUtils.announceSelectedMediaForAccessibility(
            imageThumbnail,
            false, // isVideo
            true   // itemSelected
        )

        // Assert
        verify(context).getString(R.string.photo_picker_image_thumbnail_selected)
        verify(imageThumbnail).announceForAccessibility(expectedString)
    }

    @Test
    fun `announces video unselected when item is not selected and is video`() {
        // Arrange
        val expectedString = "Video unselected"
        whenever(context.getString(R.string.photo_picker_video_thumbnail_unselected))
            .thenReturn(expectedString)

        // Act
        MediaPickerUtils.announceSelectedMediaForAccessibility(
            imageThumbnail,
            true,  // isVideo
            false  // itemSelected
        )

        // Assert
        verify(context).getString(R.string.photo_picker_video_thumbnail_unselected)
        verify(imageThumbnail).announceForAccessibility(expectedString)
    }

    @Test
    fun `announces image unselected when item is not selected and is not video`() {
        // Arrange
        val expectedString = "Image unselected"
        whenever(context.getString(R.string.photo_picker_image_thumbnail_unselected))
            .thenReturn(expectedString)

        // Act
        MediaPickerUtils.announceSelectedMediaForAccessibility(
            imageThumbnail,
            false, // isVideo
            false  // itemSelected
        )

        // Assert
        verify(context).getString(R.string.photo_picker_image_thumbnail_unselected)
        verify(imageThumbnail).announceForAccessibility(expectedString)
    }
}
