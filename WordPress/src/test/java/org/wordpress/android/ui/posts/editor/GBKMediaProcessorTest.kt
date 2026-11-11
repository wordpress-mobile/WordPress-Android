package org.wordpress.android.ui.posts.editor

import android.content.Context
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
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
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File

// BaseUnitTest carries this too, but Kotlin's opt-in requirement is not inherited by subclasses.
@ExperimentalCoroutinesApi
class GBKMediaProcessorTest : BaseUnitTest() {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var appContext: Context
    private lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var stagedFile: File
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        // A real directory so tests can assert on the temp files the processor writes there.
        cacheDir = tempFolder.newFolder("cache")
        appContext = mock {
            on { getString(R.string.error_media_upload) } doReturn UPLOAD_ERROR
            on { getCacheDir() } doReturn cacheDir
        }
        // Default to a strip that works: a failed one rejects the upload, which is opt-in per test.
        mediaUtilsWrapper = mock {
            on { stripImageLocation(any()) } doReturn true
        }
        appPrefsWrapper = mock()
        analyticsTrackerWrapper = mock()
        analyticsUtilsWrapper = mock()
        stagedFile = tempFolder.newFile("photo.jpg").apply { writeText("staged-bytes") }
    }

    private fun createProcessor(site: SiteModel = wpComSite()) = GBKMediaProcessor(
        site = site,
        appContext = appContext,
        mediaUtilsWrapper = mediaUtilsWrapper,
        appPrefsWrapper = appPrefsWrapper,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        analyticsUtilsWrapper = analyticsUtilsWrapper,
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
    fun `optimized path that does not exist falls back to the original`() = test {
        // Uploading a file that isn't there would send nothing.
        val missing = fileUri(File(tempFolder.root, "never-written.jpg"))
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(missing)

        val result = createProcessor().processFile(stagedFile, "image/jpeg", "photo.jpg")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    @Test
    fun `optimized path is resolved through getRealPathFromURI, not Uri getPath`() = test {
        // getPath() truncates a parsed path at '#' or '?'; getRealPathFromURI keeps the whole string.
        // The mock's getPath() returns null, so reading it would fall back to Original.
        val optimized = tempFolder.newFile("IMG_#1?.jpg")
        val optimizedUri = mock<android.net.Uri>()
        whenever(mediaUtilsWrapper.getRealPathFromURI(optimizedUri)).thenReturn(optimized.absolutePath)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(stagedFile, "image/jpeg", "IMG_#1?.jpg")

        result as ProcessedProxyFile.Processed
        assertThat(result.file.absolutePath).isEqualTo(optimized.absolutePath)
    }

    @Test
    fun `failed strip copy rejects the upload with a localized message`() = test {
        // Uploading the original instead would publish the location the user asked to remove, and
        // the raw IOException message would show an English, path-bearing notice.
        val missing = File(tempFolder.root, "vanished.jpg")
        whenever(mediaUtilsWrapper.getOptimizedMedia(missing.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        val thrown = runCatching {
            createProcessor(wpComSite()).processFile(missing, "image/jpeg", "vanished.jpg")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(UPLOAD_ERROR)
    }

    @Test
    fun `failed strip of the copy rejects the upload and deletes the copy`() = test {
        // The copy would still carry GPS, e.g. after a full disk or bytes ExifInterface can't rewrite.
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)
        whenever(mediaUtilsWrapper.stripImageLocation(any())).thenReturn(false)

        val thrown = runCatching {
            createProcessor(wpComSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(UPLOAD_ERROR)
        assertThat(cacheDir.walk().filter { it.isFile }.toList()).isEmpty()
    }

    @Test
    fun `failed strip of the optimized output rejects the upload and deletes the output`() = test {
        val optimized = tempFolder.newFile("optimized.jpg")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(optimizedUri)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)
        whenever(mediaUtilsWrapper.stripImageLocation(optimized.absolutePath)).thenReturn(false)

        val thrown = runCatching {
            createProcessor().processFile(stagedFile, "image/jpeg", "photo.jpg")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(UPLOAD_ERROR)
        assertThat(optimized).doesNotExist()
    }

    @Test
    fun `failed strip copy does not leak the temp file`() = test {
        // GutenbergKit only deletes files handed back to it, so a temp file abandoned mid-copy
        // (a full disk being the likely cause) would sit in the cache dir indefinitely.
        val missing = File(tempFolder.root, "vanished.jpg")
        whenever(mediaUtilsWrapper.getOptimizedMedia(missing.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        runCatching { createProcessor(wpComSite()).processFile(missing, "image/jpeg", "vanished.jpg") }

        assertThat(cacheDir.walk().filter { it.isFile }.toList()).isEmpty()
        assertThat(File(cacheDir, "processed-media").listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun `processed file is deleted when the upload is cancelled during processing`() = test {
        // The blocking image steps can't observe a cancel, so their output is written and then dropped
        // with the result. GutenbergKit only deletes a processed file it receives.
        val optimized = tempFolder.newFile("optimized.jpg")
        val optimizedUri = fileUri(optimized)
        lateinit var upload: Job
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenAnswer {
            upload.cancel()
            optimizedUri
        }
        upload = launch(start = CoroutineStart.LAZY) {
            createProcessor().processFile(stagedFile, "image/jpeg", "photo.jpg")
        }

        upload.join()

        assertThat(upload.isCancelled).isTrue()
        assertThat(optimized).doesNotExist()
    }

    @Test
    fun `strip copy is written to the processed media cache`() = test {
        // The app sweeps that directory at launch, so a copy orphaned by process death is reclaimed.
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        val result = createProcessor(wpComSite()).processFile(stagedFile, "image/jpeg", "photo.jpg")

        result as ProcessedProxyFile.Processed
        assertThat(result.file.parentFile?.parentFile).isEqualTo(File(cacheDir, "processed-media"))
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
    fun `output format is read from the encoded file, not the declared mime type`() = test {
        // ImageUtils picks its encoder from the extension it derives for the output, which for an
        // extensionless upload comes from the file's sniffed bytes rather than the declared mime.
        // Labeling from the declared type would tag these PNG bytes as image/jpeg, and WordPress
        // would store that mislabel permanently.
        val extensionless = tempFolder.newFile("screenshot")
        val optimized = tempFolder.newFile("optimized-sniffed.png")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(extensionless.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(extensionless, "image/jpeg", "screenshot")

        result as ProcessedProxyFile.Processed
        assertThat(result.mimeType).isEqualTo("image/png")
        assertThat(result.filename).isEqualTo("screenshot.png")
    }

    @Test
    fun `jpeg output is labeled jpeg even when the input declared png`() = test {
        val staged = tempFolder.newFile("mystery")
        val optimized = tempFolder.newFile("optimized-sniffed.jpg")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(staged.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(staged, "image/png", "mystery")

        result as ProcessedProxyFile.Processed
        assertThat(result.mimeType).isEqualTo("image/jpeg")
        assertThat(result.filename).isEqualTo("mystery.jpg")
    }

    @Test
    fun `gif passes through untouched`() = test {
        val gifStaged = tempFolder.newFile("anim.gif")

        val result = createProcessor().processFile(gifStaged, "image/gif", "anim.gif")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).getOptimizedMedia(any(), any())
    }

    @Test
    fun `svg passes through untouched on self-hosted`() = test {
        // Self-hosted runs every raster image through the image steps, which can't decode a vector
        // image: optimizing it would only count a false success.
        val svgStaged = tempFolder.newFile("logo.svg")

        val result = createProcessor(selfHostedSite()).processFile(svgStaged, "image/svg+xml", "logo.svg")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).getOptimizedMedia(any(), any())
        verify(mediaUtilsWrapper, never()).fixOrientationIssue(any(), any())
    }

    @Test
    fun `document passes through on a free plan for the server to judge`() = test {
        // The server's rejection reaches the editor in the user's locale, so the processor doesn't
        // second-guess the plan with the app's lagging MimeTypes table.
        val zipStaged = tempFolder.newFile("archive.zip")
        val site = wpComSite().apply { setHasFreePlan(true) }

        val result = createProcessor(site).processFile(zipStaged, "application/zip", "archive.zip")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    @Test
    fun `video passes through when optimization disabled`() = test {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
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

    @Test
    fun `handlesFile declines gif so the copy is skipped`() {
        assertThat(createProcessor().handlesFile("image/gif", "anim.gif")).isFalse()
    }

    @Test
    fun `handlesFile declines svg on self-hosted, which claims every raster image`() {
        assertThat(createProcessor(selfHostedSite()).handlesFile("image/svg+xml", "logo.svg")).isFalse()
    }

    @Test
    fun `handlesFile declines non-media so the copy is skipped`() {
        assertThat(createProcessor().handlesFile("application/pdf", "doc.pdf")).isFalse()
    }

    @Test
    fun `handlesFile claims images when optimization is on`() {
        whenever(appPrefsWrapper.isImageOptimize).thenReturn(true)

        assertThat(createProcessor().handlesFile("image/jpeg", "photo.jpg")).isTrue()
    }

    @Test
    fun `handlesFile declines images when optimization keeps the original size and quality`() {
        // getOptimizedMedia skips original size at 100% quality, so processImage returns Original
        // on WP.com without reading the file.
        whenever(appPrefsWrapper.isImageOptimize).thenReturn(true)
        whenever(appPrefsWrapper.imageOptimizeMaxSize).thenReturn(1)
        whenever(appPrefsWrapper.imageOptimizeQuality).thenReturn(100)

        assertThat(createProcessor(wpComSite()).handlesFile("image/jpeg", "photo.jpg")).isFalse()
    }

    @Test
    fun `handlesFile claims images when optimization keeps the original size but lowers quality`() {
        whenever(appPrefsWrapper.isImageOptimize).thenReturn(true)
        whenever(appPrefsWrapper.imageOptimizeMaxSize).thenReturn(1)
        whenever(appPrefsWrapper.imageOptimizeQuality).thenReturn(85)

        assertThat(createProcessor(wpComSite()).handlesFile("image/jpeg", "photo.jpg")).isTrue()
    }

    @Test
    fun `handlesFile claims images when the location strip applies`() {
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        assertThat(createProcessor().handlesFile("image/jpeg", "photo.jpg")).isTrue()
    }

    @Test
    fun `handlesFile claims images on self-hosted for the rotation fallback`() {
        // Issue #5737: self-hosted sites are not rotated server-side, so processImage still needs
        // the file even with every optimization pref off.
        assertThat(createProcessor(selfHostedSite()).handlesFile("image/jpeg", "photo.jpg")).isTrue()
    }

    @Test
    fun `handlesFile declines images when nothing would touch the file`() {
        // WP.com, optimization off, strip off: processImage returns Original without reading the
        // file, so claiming it would cost a full copy for nothing.
        assertThat(createProcessor(wpComSite()).handlesFile("image/jpeg", "photo.jpg")).isFalse()
    }

    @Test
    fun `handlesFile declines images whose format cannot be exif-stripped`() {
        // androidx ExifInterface cannot rewrite HEIC, so the strip is a no-op for it and the file
        // is never read — see EXIF_MIME_TYPES.
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)

        assertThat(createProcessor(wpComSite()).handlesFile("image/heic", "photo.heic")).isFalse()
    }

    @Test
    fun `handlesFile claims videos when optimization is on`() {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        whenever(appPrefsWrapper.isVideoOptimize).thenReturn(true)

        assertThat(createProcessor().handlesFile("video/mp4", "movie.mp4")).isTrue()
    }

    @Test
    fun `handlesFile declines videos when optimization is off`() {
        // Optimization is opt-in, so this is common. Claiming here copies the whole video to cache
        // for a guaranteed passthrough.
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)

        assertThat(createProcessor(wpComSite()).handlesFile("video/mp4", "movie.mp4")).isFalse()
    }

    @Test
    fun `handlesFile declines videos on a free plan when optimization is off`() {
        // The processor no longer checks the free-plan duration limit, so the plan doesn't matter.
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        val site = wpComSite().apply { setHasFreePlan(true) }

        assertThat(createProcessor(site).handlesFile("video/mp4", "movie.mp4")).isFalse()
    }

    @Test
    fun `mime type parameters are stripped before the type is routed`() = test {
        // Content-Type may legitimately carry parameters. An unnormalized value would miss the
        // image/ prefix test and fall through to the non-media passthrough.
        val optimized = tempFolder.newFile("optimized.jpg")
        val optimizedUri = fileUri(optimized)
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false))
            .thenReturn(optimizedUri)

        val result = createProcessor().processFile(stagedFile, "image/jpeg; charset=binary", "photo.jpg")

        result as ProcessedProxyFile.Processed
        assertThat(result.mimeType).isEqualTo("image/jpeg")
    }

    @Test
    fun `mime type casing is normalized before the type is routed`() = test {
        // An uppercase type must still route to the image path: getOptimizedMedia being consulted
        // is what proves the normalization happened.
        whenever(mediaUtilsWrapper.getOptimizedMedia(stagedFile.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(false)

        val result = createProcessor(wpComSite()).processFile(stagedFile, "IMAGE/JPEG", "photo.jpg")

        verify(mediaUtilsWrapper).getOptimizedMedia(stagedFile.absolutePath, false)
        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    @Test
    fun `blank mime type never resolves to an empty string`() {
        // An empty resolved type would be meaningless to the type routing; a placeholder is the
        // safe floor. octet-stream routes to neither image nor video, so the file is not claimed.
        assertThat(createProcessor().handlesFile("", "mystery")).isFalse()
    }

    @Test
    fun `handlesFile declines documents on a free plan`() {
        // Nothing reads a document, so claiming it would cost a full copy for a guaranteed passthrough.
        val site = wpComSite().apply { setHasFreePlan(true) }

        assertThat(createProcessor(site).handlesFile("application/zip", "archive.zip")).isFalse()
    }

    /** A Uri that [MediaUtilsWrapper.getRealPathFromURI] resolves to [file]. */
    private fun fileUri(file: File): android.net.Uri = mock<android.net.Uri>().also {
        whenever(mediaUtilsWrapper.getRealPathFromURI(it)).thenReturn(file.absolutePath)
    }

    companion object {
        private const val UPLOAD_ERROR = "An error occurred while uploading media"
    }
}
