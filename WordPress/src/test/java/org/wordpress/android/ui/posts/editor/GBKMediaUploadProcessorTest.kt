package org.wordpress.android.ui.posts.editor

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File

// BaseUnitTest carries this too, but Kotlin's opt-in requirement is not inherited by subclasses.
@ExperimentalCoroutinesApi
class GBKMediaUploadProcessorTest : BaseUnitTest() {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var appContext: Context
    private lateinit var mediaUtilsWrapper: MediaUtilsWrapper
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var siteUtilsWrapper: SiteUtilsWrapper
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    private lateinit var stagedFile: File
    private lateinit var cacheDir: File

    @Before
    fun setUp() {
        // A real directory so tests can assert on the temp files the processor writes there.
        cacheDir = tempFolder.newFolder("cache")
        appContext = mock {
            on { getString(R.string.error_media_file_type_not_allowed) } doReturn FILE_TYPE_ERROR
            on { getString(R.string.error_media_video_duration_exceeds_limit) } doReturn VIDEO_LIMIT_ERROR
            on { getCacheDir() } doReturn cacheDir
        }
        mediaUtilsWrapper = mock()
        appPrefsWrapper = mock()
        // Default to a paid plan: the free-plan rejection is opt-in per test.
        siteUtilsWrapper = mock {
            on { onFreePlan(any()) } doReturn false
        }
        analyticsTrackerWrapper = mock()
        analyticsUtilsWrapper = mock()
        stagedFile = tempFolder.newFile("photo.jpg").apply { writeText("staged-bytes") }
    }

    private fun createProcessor(site: SiteModel = wpComSite()) = GBKMediaUploadProcessor(
        site = site,
        appContext = appContext,
        mediaUtilsWrapper = mediaUtilsWrapper,
        appPrefsWrapper = appPrefsWrapper,
        siteUtilsWrapper = siteUtilsWrapper,
        analyticsTrackerWrapper = analyticsTrackerWrapper,
        analyticsUtilsWrapper = analyticsUtilsWrapper,
        ioDispatcher = testDispatcher()
    )

    private fun wpComSite() = SiteModel().apply { setIsWPCom(true) }

    private fun selfHostedSite() = SiteModel().apply { setIsWPCom(false) }

    /** Puts the site on a free WP.com plan, where audio/document uploads are plan-restricted. */
    private fun onFreePlan() {
        whenever(siteUtilsWrapper.onFreePlan(any())).thenReturn(true)
    }

    /**
     * A site subject to the free-plan video duration limit. Note this is [SiteModel.hasFreePlan]
     * (the API's `plan.is_free`), which gates the duration check, and is distinct from
     * [SiteUtilsWrapper.onFreePlan] (a plan-id match) used for the mime allowlist.
     */
    private fun freePlanSite() = SiteModel().apply {
        setIsWPCom(true)
        setHasFreePlan(true)
    }

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
    fun `failed strip copy does not leak the temp file`() = test {
        // GutenbergKit only deletes files handed back to it, so a temp file abandoned mid-copy
        // (a full disk being the likely cause) would sit in the cache dir indefinitely.
        val missing = File(tempFolder.root, "vanished.jpg")
        whenever(mediaUtilsWrapper.getOptimizedMedia(missing.absolutePath, false)).thenReturn(null)
        whenever(appPrefsWrapper.isStripImageLocation).thenReturn(true)
        val cacheFilesBefore = cacheDir.listFiles()?.size ?: 0

        val thrown = runCatching {
            createProcessor(wpComSite()).processFile(missing, "image/jpeg", "vanished.jpg")
        }.exceptionOrNull()

        assertThat(thrown).isNotNull()
        assertThat(cacheDir.listFiles()?.size ?: 0).isEqualTo(cacheFilesBefore)
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
    fun `document disallowed by a free plan throws with localized message`() = test {
        onFreePlan()
        whenever(mediaUtilsWrapper.isApplicationMimeType("application/zip")).thenReturn(true)
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
    fun `audio disallowed by a free plan throws with localized message`() = test {
        onFreePlan()
        whenever(mediaUtilsWrapper.isAudioMimeType("audio/mpeg")).thenReturn(true)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(anyOrNull(), any())).thenReturn(false)
        val audioStaged = tempFolder.newFile("song.mp3")

        val thrown = runCatching {
            createProcessor().processFile(audioStaged, "audio/mpeg", "song.mp3")
        }.exceptionOrNull()

        assertThat(thrown)
            .isInstanceOf(GBKMediaUploadException::class.java)
            .hasMessage(FILE_TYPE_ERROR)
    }

    @Test
    fun `document missing from the allowlist uploads on a paid plan`() = test {
        // Paid and self-hosted sites are not plan-restricted, so the stale MimeTypes table must
        // not reject for them — the server is authoritative. The allowlist is left unstubbed
        // deliberately: reaching it at all would be the bug.
        val zipStaged = tempFolder.newFile("archive.zip")

        val result = createProcessor().processFile(zipStaged, "application/zip", "archive.zip")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    @Test
    fun `image missing from the allowlist uploads even on a free plan`() = test {
        // AVIF is core-supported since WP 6.5 but absent from the app's MimeTypes table. Images
        // are never plan-restricted, so the table must not be consulted for them at all.
        onFreePlan()
        whenever(mediaUtilsWrapper.getOptimizedMedia(any(), any())).thenReturn(null)
        val avifStaged = tempFolder.newFile("photo.avif")

        val result = createProcessor().processFile(avifStaged, "image/avif", "photo.avif")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    @Test
    fun `unresolvable type is not rejected as a plan-restricted document`() = test {
        // resolveMimeType emits application/octet-stream for "unknown bytes". It would classify as
        // an application type, so the exclusion has to short-circuit ahead of that classification —
        // isApplicationMimeType is deliberately left unstubbed to pin that ordering.
        onFreePlan()
        val unknownStaged = tempFolder.newFile("mystery.bin")

        val result = createProcessor().processFile(unknownStaged, "application/octet-stream", "mystery.bin")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    @Test
    fun `video exceeding duration limit throws with localized message`() = test {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        whenever(mediaUtilsWrapper.isProhibitedVideoDuration(any(), any(), any<File>(), any()))
            .thenReturn(true)
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
        whenever(mediaUtilsWrapper.isProhibitedVideoDuration(any(), any(), any<File>(), any()))
            .thenReturn(false)
        whenever(appPrefsWrapper.isVideoOptimize).thenReturn(false)
        val videoStaged = tempFolder.newFile("movie.mp4")

        val result = createProcessor().processFile(videoStaged, "video/mp4", "movie.mp4")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
    }

    @Test
    fun `duration check receives the resolved mime type, not the staged path`() = test {
        // The staged file is named after the client-supplied filename, which need not carry an
        // extension. Deriving "is this a video" from that path is an extension-only test, so the
        // duration limit must be keyed off the resolved mime type instead.
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        whenever(mediaUtilsWrapper.isProhibitedVideoDuration(any(), any(), any<File>(), any()))
            .thenReturn(false)
        whenever(appPrefsWrapper.isVideoOptimize).thenReturn(false)
        val extensionlessVideo = tempFolder.newFile("upload")

        createProcessor().processFile(extensionlessVideo, "video/mp4", "upload")

        verify(mediaUtilsWrapper).isProhibitedVideoDuration(
            any(),
            any(),
            eq(extensionlessVideo),
            eq("video/mp4")
        )
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
    fun `handlesFile declines non-media so the copy is skipped`() {
        assertThat(createProcessor().handlesFile("application/pdf", "doc.pdf")).isFalse()
    }

    @Test
    fun `handlesFile claims images when optimization is on`() {
        whenever(appPrefsWrapper.isImageOptimize).thenReturn(true)

        assertThat(createProcessor().handlesFile("image/jpeg", "photo.jpg")).isTrue()
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
    fun `handlesFile claims videos on a free plan for the duration check`() {
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)

        assertThat(createProcessor(freePlanSite()).handlesFile("video/mp4", "movie.mp4")).isTrue()
    }

    @Test
    fun `handlesFile declines videos when nothing would touch the file`() {
        // Paid plan with video optimization off — a common configuration, since optimization is
        // opt-in. Claiming here copies the whole video to cache for a guaranteed passthrough.
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)

        assertThat(createProcessor(wpComSite()).handlesFile("video/mp4", "movie.mp4")).isFalse()
    }

    @Test
    fun `handlesFile declines videos on a free plan with VideoPress enabled`() {
        // VideoPress lifts the duration limit, so the check does not measure and nothing else
        // reads the file with optimization off.
        whenever(mediaUtilsWrapper.isVideoMimeType("video/mp4")).thenReturn(true)
        val site = freePlanSite().apply { activeModules = "videopress" }

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
    fun `text plain is treated as a placeholder and resolved from the filename`() = test {
        // GutenbergKit's multipart parser defaults a part with no Content-Type to text/plain
        // (RFC 7578), and picks the file part by its filename parameter rather than its type — so
        // a real image can arrive labeled text/plain and must not be rejected as a disallowed type.
        //
        // MimeTypeMap is a stub returning null under unit tests, so the extension lookup cannot
        // resolve here; this asserts the surrounding contract instead — a free-plan site does not
        // reject text/plain, which is neither audio nor an application type.
        onFreePlan()
        val staged = tempFolder.newFile("note.txt")

        val result = createProcessor().processFile(staged, "text/plain", "note.txt")

        assertThat(result).isEqualTo(ProcessedProxyFile.Original)
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    @Test
    fun `blank mime type never resolves to an empty string`() {
        // An empty resolved type would be meaningless to the type routing; a placeholder is the
        // safe floor. octet-stream routes to neither image nor video, so the file is not claimed.
        assertThat(createProcessor().handlesFile("", "mystery")).isFalse()
    }

    @Test
    fun `handlesFile claims plan-rejected types so processFile can reject them locally`() {
        // Declining would relay the file to WordPress instead, wasting a full upload and replacing
        // our localized message with the server's. Pairs with the processFile rejection test above,
        // which uses the same mime type.
        onFreePlan()
        whenever(mediaUtilsWrapper.isApplicationMimeType("application/zip")).thenReturn(true)
        whenever(mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(anyOrNull(), any())).thenReturn(false)

        assertThat(createProcessor().handlesFile("application/zip", "archive.zip")).isTrue()
    }

    @Test
    fun `handlesFile declines documents that are not plan-rejected`() {
        // On a paid plan there is nothing for processFile to say about a document, so claiming it
        // would cost a full byte-for-byte copy for a guaranteed passthrough. The plan test
        // short-circuits before the type is even classified.
        assertThat(createProcessor().handlesFile("application/zip", "archive.zip")).isFalse()
        verify(mediaUtilsWrapper, never()).isMimeTypeSupportedBySitePlan(anyOrNull(), any())
    }

    private fun fileUri(file: File): android.net.Uri = mock {
        on { path } doReturn file.absolutePath
    }

    companion object {
        private const val FILE_TYPE_ERROR = "This file type is not allowed"
        private const val VIDEO_LIMIT_ERROR = "Uploading videos longer than 5 minutes requires a paid plan."
    }
}
