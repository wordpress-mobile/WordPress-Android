package org.wordpress.android.ui.posts.editor

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GBKMediaUploadProcessorTest : BaseUnitTest() {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var appContext: Context
    private lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var stagedFile: File

    @Before
    fun setUp() {
        appContext = mock {
            on { getString(R.string.error_media_file_type_not_allowed) } doReturn FILE_TYPE_ERROR
            on { getString(R.string.error_media_video_duration_exceeds_limit) } doReturn VIDEO_LIMIT_ERROR
        }
        mediaUtilsWrapper = mock {
            on { isMimeTypeSupportedBySitePlan(anyOrNull(), any()) } doReturn true
        }
        appPrefsWrapper = mock()
        stagedFile = tempFolder.newFile("photo.jpg").apply { writeText("staged-bytes") }
    }

    private fun createProcessor(site: SiteModel = wpComSite()) = GBKMediaUploadProcessor(
        site = site,
        appContext = appContext,
        mediaUtilsWrapper = mediaUtilsWrapper,
        appPrefsWrapper = appPrefsWrapper,
        ioDispatcher = testDispatcher()
    )

    private fun wpComSite() = SiteModel().apply { setIsWPCom(true) }

    private fun selfHostedSite() = SiteModel().apply { setIsWPCom(false) }

    @Test
    fun `image is optimized when optimization produces a new file`() = test {
        val optimized = tempFolder.newFile("optimized.jpg")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isInstanceOf(ProcessedProxyFile.Processed::class.java)
        result as ProcessedProxyFile.Processed
        assertThat(result.file.absolutePath).isEqualTo(optimized.absolutePath)
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.filename).isEqualTo("photo.jpg")
    }

    @Test
    fun `image passes through when processing would be a no-op`() = test {
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(false)

        val result = createProcessor(wpComSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).fixOrientationIssue(any(), any())
    }

    @Test
    fun `gps is stripped onto a copy when strip enabled and optimization off`() = test {
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        val result = createProcessor(wpComSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isInstanceOf(ProcessedProxyFile.Processed::class.java)
        result as ProcessedProxyFile.Processed
        // Stripping must run on a copy, never on the staged file — Original passthrough would
        // re-send the original request body and discard an in-place edit.
        assertThat(result.file.absolutePath).isNotEqualTo(stagedFile.absolutePath)
        assertThat(result.file.readText()).isEqualTo("staged-bytes")
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.filename).isEqualTo("photo.jpg")
        verify(mediaUtilsWrapper).stripImageLocation(result.file.absolutePath)
        result.file.delete()
    }

    @Test
    fun `gps is stripped from the optimized output when strip enabled`() = test {
        val optimized = tempFolder.newFile("optimized.jpg")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(optimizedUri)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        createProcessor().processFile(stagedFile, "image/jpeg", "photo.jpg")

        verify(mediaUtilsWrapper).stripImageLocation(optimized.absolutePath)
    }

    @Test
    fun `png gps is stripped onto a copy when strip enabled and optimization off`() = test {
        val pngStaged = tempFolder.newFile("art.png")
        whenever(mediaUtilsWrapper.getOptimizedMedia(pngStaged.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        // androidx ExifInterface can write PNG, so PNG must take the copy-and-strip branch.
        val result = createProcessor(wpComSite()).processFile(pngStaged, "image/png", "art.png")

        assertThat(result).isInstanceOf(ProcessedProxyFile.Processed::class.java)
        result as ProcessedProxyFile.Processed
        assertThat(result.file.absolutePath).isNotEqualTo(pngStaged.absolutePath)
        verify(mediaUtilsWrapper).stripImageLocation(result.file.absolutePath)
        result.file.delete()
    }

    @Test
    fun `heic passes through when strip enabled and optimization off`() = test {
        val heicStaged = tempFolder.newFile("photo.heic")
        whenever(mediaUtilsWrapper.getOptimizedMedia(heicStaged.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        // androidx ExifInterface cannot write HEIF, so copy-and-strip would silently fail and
        // upload a still-geotagged copy — HEIC must not take the strip branch.
        val result = createProcessor(wpComSite()).processFile(heicStaged, "image/heic", "photo.heic")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).stripImageLocation(any())
    }

    @Test
    fun `heic reports jpeg mime type and extension after optimization`() = test {
        val heicStaged = tempFolder.newFile("photo.heic")
        val optimized = tempFolder.newFile("optimized.heic")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(heicStaged.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(heicStaged, "image/heic", "photo.heic")

        assertThat(result).isInstanceOf(ProcessedProxyFile.Processed::class.java)
        result as ProcessedProxyFile.Processed
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.filename).isEqualTo("photo.jpg")
    }

    @Test
    fun `png keeps png mime type and extension after optimization`() = test {
        val pngStaged = tempFolder.newFile("art.png")
        val optimized = tempFolder.newFile("optimized.png")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(pngStaged.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(pngStaged, "image/png", "art.png")

        result as ProcessedProxyFile.Processed
        assertThat(result.mimeType).isEqualTo("image/png")
        assertThat(result.filename).isEqualTo("art.png")
    }

    @Test
    fun `gif passes through untouched`() = test {
        val gifStaged = tempFolder.newFile("anim.gif")

        val result = createProcessor().processFile(gifStaged, "image/gif", "anim.gif")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).getOptimizedMedia(any(), any())
    }

    @Test
    fun `disallowed file type throws with localized message`() = test {
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(anyOrNull(), any())).thenReturn(false)
        val zipStaged = tempFolder.newFile("archive.zip")

        val thrown = runCatching {
            createProcessor().processFile(zipStaged, "application/zip", "archive.zip")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(FILE_TYPE_ERROR)
    }

    @Test
    fun `video exceeding duration limit throws with localized message`() = test {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        whenever(mediaUtilsWrapper.isProhibitedVideoDuration(any(), any(), any<File>())).thenReturn(true)
        val videoStaged = tempFolder.newFile("movie.mp4")

        val thrown = runCatching {
            createProcessor().processFile(videoStaged, "video/mp4", "movie.mp4")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(VIDEO_LIMIT_ERROR)
    }

    @Test
    fun `video passes through when optimization disabled`() = test {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        whenever(mediaUtilsWrapper.isProhibitedVideoDuration(any(), any(), any<File>())).thenReturn(false)
        whenever(appPrefsWrapper.isVideoOptimize).thenReturn(false)
        val videoStaged = tempFolder.newFile("movie.mp4")

        val result = createProcessor().processFile(videoStaged, "video/mp4", "movie.mp4")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    @Test
    fun `optimization returning the input path is treated as not optimized`() = test {
        // ImageUtils.optimizeImage returns the original path for skips/failures; wrapping it in
        // Processed would mislabel the original file with a corrected mime type.
        val inputPathUri = fileUri(stagedFile)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(inputPathUri)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(false)

        val result = createProcessor(wpComSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    @Test
    fun `self-hosted image is rotated when optimization is off`() = test {
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        val rotated = tempFolder.newFile("rotated.jpg")
        val rotatedUri = fileUri(rotated)
        whenever(mediaUtilsWrapper.fixOrientationIssue(stagedFile.absolutePath, false))
            .thenReturn(rotatedUri)

        val result = createProcessor(selfHostedSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isInstanceOf(ProcessedProxyFile.Processed::class.java)
        result as ProcessedProxyFile.Processed
        assertThat(result.file.absolutePath).isEqualTo(rotated.absolutePath)
    }

    @Test
    fun `non-media file allowed by the site plan passes through`() = test {
        val docStaged = tempFolder.newFile("doc.pdf")

        val result = createProcessor().processFile(docStaged, "application/pdf", "doc.pdf")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    private fun fileUri(file: File): android.net.Uri = mock {
        on { path } doReturn file.absolutePath
    }

    companion object {
        private const val FILE_TYPE_ERROR = "This file type is not allowed"
        private const val VIDEO_LIMIT_ERROR = "Uploading videos longer than 5 minutes requires a paid plan."
    }
}
