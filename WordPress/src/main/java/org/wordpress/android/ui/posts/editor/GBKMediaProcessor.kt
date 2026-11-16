package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.m4m.IProgressListener
import org.m4m.MediaComposer
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.MediaFileNameUtils
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.WPVideoUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.gutenberg.MediaProcessor
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File
import java.io.IOException
import javax.inject.Named
import kotlin.coroutines.resume

/**
 * Applies the app's media settings (image optimization, location stripping, video optimization) to
 * GutenbergKit editor uploads, matching the legacy editor's upload pipeline.
 *
 * GutenbergKit still delivers the result, so WordPress's raw response (including the header core
 * uses to recover a failed server-side resize) reaches the editor unchanged. Contract notes:
 * - Returning [ProcessedProxyFile.Original] forwards the original request body, so in-place edits
 *   to the staged file are never uploaded.
 * - GutenbergKit deletes [ProcessedProxyFile.Processed] files after the upload.
 * - A thrown exception's message is shown to the user as an editor notice, so it must be localized.
 */
@Suppress("LongParameterList")
class GBKMediaProcessor @AssistedInject constructor(
    @Assisted private val site: SiteModel,
    private val appContext: Context,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : MediaProcessor {
    /**
     * Metadata-only gate GutenbergKit checks before copying the upload to a temp file. Declining
     * forwards the original body, so this claims only files [processFile] would actually read.
     */
    override fun handlesFile(mimeType: String, filename: String): Boolean {
        val resolvedMimeType = resolveMimeType(mimeType, filename)

        return when {
            resolvedMimeType in UNPROCESSED_IMAGE_TYPES -> false
            // Only the opt-in transcode reads a video.
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> appPrefsWrapper.isVideoOptimize
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> needsImageFile(resolvedMimeType)
            else -> false
        }
    }

    /** Mirrors [processImage]'s exits: optimization, the self-hosted rotation, and the location strip. */
    private fun needsImageFile(resolvedMimeType: String): Boolean {
        val stripsLocation = appPrefsWrapper.isStripImageLocation && resolvedMimeType in EXIF_MIME_TYPES
        return optimizesImages() || !site.isWPCom || stripsLocation
    }

    /** Mirrors `WPMediaUtils.getOptimizedMedia`, which skips original size at 100% quality. */
    private fun optimizesImages(): Boolean {
        val keepsOriginal = appPrefsWrapper.imageOptimizeMaxSize <= IMAGE_MAX_SIZE_ORIGINAL &&
                appPrefsWrapper.imageOptimizeQuality == IMAGE_QUALITY_MAX
        return appPrefsWrapper.isImageOptimize && !keepsOriginal
    }

    override suspend fun processFile(file: File, mimeType: String, filename: String): ProcessedProxyFile {
        var processed: ProcessedProxyFile.Processed? = null
        try {
            return withContext(ioDispatcher) {
                process(file, mimeType, filename).also { processed = it as? ProcessedProxyFile.Processed }
            }
        } catch (e: CancellationException) {
            // A cancel during the blocking image steps drops the result, and GutenbergKit only deletes
            // a processed file it receives.
            processed?.file?.deleteWithDir()
            throw e
        }
    }

    private suspend fun process(file: File, mimeType: String, filename: String): ProcessedProxyFile {
        val resolvedMimeType = resolveMimeType(mimeType, filename)

        return when {
            resolvedMimeType in UNPROCESSED_IMAGE_TYPES -> ProcessedProxyFile.Original
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> processVideo(file, filename)
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> processImage(file, resolvedMimeType, filename)
            else -> ProcessedProxyFile.Original
        }
    }

    @Suppress("ReturnCount")
    private suspend fun processVideo(file: File, filename: String): ProcessedProxyFile {
        // Matches the legacy pipeline, where video optimization is opt-in.
        if (!appPrefsWrapper.isVideoOptimize) return ProcessedProxyFile.Original

        val output = transcodeMutex.withLock { transcodeVideo(file) } ?: return ProcessedProxyFile.Original

        // Matches VideoOptimizer: keep the transcode only when it's smaller.
        if (output.length() >= file.length()) {
            output.deleteWithDir()
            return ProcessedProxyFile.Original
        }

        return ProcessedProxyFile.Processed(
            file = output,
            mimeType = MIME_MP4,
            filename = MediaFileNameUtils.replaceExtension(filename, EXTENSION_MP4)
        )
    }

    /**
     * Transcodes per the user's video settings, resolving to null on any failure so the original
     * uploads instead, as [org.wordpress.android.ui.uploads.VideoOptimizer] does.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    private suspend fun transcodeVideo(input: File): File? = suspendCancellableCoroutine { continuation ->
        val output = createProcessedFile(MediaFileNameUtils.replaceExtension(input.name, EXTENSION_MP4))
        if (output == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val listener = object : IProgressListener {
            // Set when encoding starts, matching VideoOptimizer's elapsed_time_ms.
            @Volatile
            private var startTimeMs = System.currentTimeMillis()

            override fun onMediaStart() {
                startTimeMs = System.currentTimeMillis()
            }

            override fun onMediaProgress(progress: Float) = Unit
            override fun onMediaPause() = Unit

            // Also fires before onMediaDone, so only onMediaDone and onError complete the coroutine.
            override fun onMediaStop() = Unit

            // Skipped once cancelled: the output is gone and nobody awaits the result.
            override fun onMediaDone() {
                if (!continuation.isActive) return
                trackTranscodeFinished(input, output, startTimeMs, null)
                continuation.resume(output)
            }

            override fun onError(exception: Exception) {
                if (!continuation.isActive) return
                AppLog.e(AppLog.T.MEDIA, "GBKMediaProcessor > video transcode failed", exception)
                trackTranscodeFinished(input, output, startTimeMs, exception)
                output.deleteWithDir()
                continuation.resume(null)
            }
        }

        // m4m throws NPEs, among others, on some malformed inputs.
        var wasNpeDetected = false
        val composer = try {
            WPVideoUtils.getVideoOptimizationComposer(
                appContext,
                input.absolutePath,
                output.absolutePath,
                listener,
                appPrefsWrapper.videoOptimizeWidth,
                appPrefsWrapper.videoOptimizeQuality
            )
        } catch (e: NullPointerException) {
            AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > NPE getting composer: ${e.message}")
            wasNpeDetected = true
            null
        } catch (e: Exception) {
            AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > error getting composer: $e")
            null
        }

        if (composer == null) {
            trackCantOptimize(input, wasNpeDetected)
            output.deleteWithDir()
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            composer.stopQuietly()
            output.deleteWithDir()
        }

        // Composer setup is slow and blocking, so the upload may already be cancelled. Starting
        // anyway would orphan a transcode that holds the codecs after the mutex is released.
        if (!continuation.isActive) return@suspendCancellableCoroutine

        // m4m throws when it can't set up the codec; upload the original rather than fail.
        try {
            composer.start()
        } catch (e: Exception) {
            AppLog.e(AppLog.T.MEDIA, "GBKMediaProcessor > failed to start composer", e)
            output.deleteWithDir()
            if (continuation.isActive) continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        // A cancel that landed during start() stopped the composer before it had a pipeline to stop.
        if (!continuation.isActive) composer.stopQuietly()
    }

    /** Stopping twice is harmless: m4m only signals the running sources to stop. */
    @Suppress("TooGenericExceptionCaught")
    private fun MediaComposer.stopQuietly() {
        try {
            stop()
        } catch (e: Exception) {
            AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > error stopping composer: ${e.message}")
        }
    }

    /** Mirrors VideoOptimizer's null-composer event so the two pipelines compare in telemetry. */
    private fun trackCantOptimize(input: File, wasNpeDetected: Boolean) {
        val properties = analyticsUtilsWrapper.getMediaProperties(true, null, input.absolutePath)
        properties["was_npe_detected"] = wasNpeDetected
        properties[PROPERTY_OPTIMIZER_LIB] = OPTIMIZER_LIB_M4M
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE, properties)
    }

    /**
     * Mirrors VideoOptimizer's events, including its `input_video_`/`output_video_` prefixes. Like
     * VideoOptimizer, errors also describe the partial output.
     */
    private fun trackTranscodeFinished(input: File, output: File, startTimeMs: Long, exception: Exception?) {
        val properties = mutableMapOf<String, Any?>()
        analyticsUtilsWrapper.getMediaProperties(true, null, input.absolutePath)
            .forEach { (key, value) -> properties["input_video_$key"] = value }
        analyticsUtilsWrapper.getMediaProperties(true, null, output.absolutePath)
            .forEach { (key, value) -> properties["output_video_$key"] = value }
        properties["saved_megabytes"] = ((input.length() - output.length()) / BYTES_PER_MEGABYTE).toString()

        if (exception != null) {
            properties["exception_name"] = exception.javaClass.canonicalName
            properties["exception_message"] = exception.message
        }

        properties["elapsed_time_ms"] = System.currentTimeMillis() - startTimeMs
        properties[PROPERTY_OPTIMIZER_LIB] = OPTIMIZER_LIB_M4M

        val stat = if (exception == null) {
            AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZED
        } else {
            AnalyticsTracker.Stat.MEDIA_VIDEO_OPTIMIZE_ERROR
        }
        analyticsTrackerWrapper.track(stat, properties)
    }

    @Suppress("ReturnCount")
    private fun processImage(file: File, mimeType: String, filename: String): ProcessedProxyFile {
        val optimized = mediaUtilsWrapper.getOptimizedMedia(file.absolutePath, false)
            .toDistinctExistingFile(file)
        if (optimized != null) {
            return processedImage(optimized, filename)
        }

        // Self-hosted sites may not rotate on the server: core does it only for JPEGs (WP 5.3+) and
        // only when PHP's `exif` extension is available. Matches the legacy editors (Gutenberg Mobile and Aztec).
        if (!site.isWPCom) {
            val rotated = mediaUtilsWrapper.fixOrientationIssue(file.absolutePath, false)
                .toDistinctExistingFile(file)
            if (rotated != null) {
                return processedImage(rotated, filename)
            }
        }

        if (appPrefsWrapper.isStripImageLocation && mimeType in EXIF_MIME_TYPES) {
            val copy = copyForLocationStrip(file)
            stripLocationOrReject(copy)
            return ProcessedProxyFile.Processed(copy, mimeType, filename)
        }

        // Nothing to do, so skip a needless lossy re-encode.
        return ProcessedProxyFile.Original
    }

    /**
     * Copies [file] for stripping, since an in-place edit is lost when the original body is forwarded.
     * A failure rejects the upload, as uploading the original would publish the location the user
     * asked to remove.
     */
    private fun copyForLocationStrip(file: File): File {
        val copy = createProcessedFile(file.name)
            ?: throw GBKMediaUploadException(appContext.getString(R.string.error_media_upload))
        try {
            return file.copyTo(copy, overwrite = true)
        } catch (e: IOException) {
            AppLog.e(AppLog.T.MEDIA, "GBKMediaProcessor > couldn't copy the image to strip its location", e)
            copy.deleteWithDir()
            throw GBKMediaUploadException(appContext.getString(R.string.error_media_upload))
        }
    }

    /**
     * Places outputs in the processed media cache, whose leftovers the app sweeps at launch. A file
     * orphaned by process death would otherwise stay in the cache root until the OS evicts it.
     */
    private fun createProcessedFile(fileName: String): File? =
        WPMediaUtils.createProcessedMediaFile(appContext, fileName).also {
            if (it == null) AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > couldn't create a processed media file")
        }

    /** Also drops the file's directory, which [createProcessedFile] creates for it alone. */
    private fun File.deleteWithDir() {
        delete()
        parentFile?.delete()
    }

    /**
     * Rejects the upload when [file] keeps its location, e.g. a full disk or bytes ExifInterface
     * can't rewrite, since uploading it would publish what the user asked to remove.
     */
    private fun stripLocationOrReject(file: File) {
        if (!mediaUtilsWrapper.stripImageLocation(file.absolutePath)) {
            file.deleteWithDir()
            throw GBKMediaUploadException(appContext.getString(R.string.error_media_upload))
        }
    }

    /**
     * Resolves an optimize/rotate result to its output file, or null when the step was a no-op
     * (it returns the input path) or the output is missing. The path goes through
     * `getRealPathFromURI` because these URIs come from `Uri.parse`, whose `getPath()` truncates
     * a file name at a `#` or `?`.
     */
    @Suppress("ReturnCount")
    private fun Uri?.toDistinctExistingFile(input: File): File? {
        val resolvedPath = this?.let { mediaUtilsWrapper.getRealPathFromURI(it) } ?: return null
        if (resolvedPath == input.absolutePath) return null

        val output = File(resolvedPath)
        if (!output.exists()) {
            AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > processed image is missing, using the original")
            return null
        }
        return output
    }

    /**
     * Strips location when enabled and labels the output by its encoded format. ImageUtils writes
     * PNG only for a `.png` output and JPEG for everything else, naming the output to match.
     */
    private fun processedImage(output: File, filename: String): ProcessedProxyFile {
        if (appPrefsWrapper.isStripImageLocation) {
            // getOptimizedMedia copies the original EXIF, GPS included, onto its output.
            stripLocationOrReject(output)
        }

        val isPng = output.extension.lowercase() == EXTENSION_PNG
        return ProcessedProxyFile.Processed(
            file = output,
            mimeType = if (isPng) MIME_PNG else MIME_JPEG,
            filename = MediaFileNameUtils.replaceExtension(filename, if (isPng) EXTENSION_PNG else EXTENSION_JPG)
        )
    }

    /**
     * Normalizes the declared type (parameters, casing) and falls back to the file extension when
     * it's blank or `application/octet-stream`, which the browser sends for a file it can't type.
     */
    private fun resolveMimeType(mimeType: String, filename: String): String {
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        if (normalized.isNotBlank() && normalized != MIME_OCTET_STREAM) return normalized

        val extension = filename.substringAfterLast('.', "").lowercase()
        // getSingleton() is null under the unit test stubs.
        val fromExtension = MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
        return fromExtension ?: normalized.ifBlank { MIME_OCTET_STREAM }
    }

    @AssistedFactory
    interface Factory {
        fun create(site: SiteModel): GBKMediaProcessor
    }

    companion object {
        /** Process-wide, so stacked editor instances can't run parallel codec-heavy transcodes. */
        private val transcodeMutex = Mutex()

        private const val IMAGE_MAX_SIZE_ORIGINAL = 1
        private const val IMAGE_QUALITY_MAX = 100

        private const val PROPERTY_OPTIMIZER_LIB = "optimizer_lib"
        private const val OPTIMIZER_LIB_M4M = "m4m"
        private const val BYTES_PER_MEGABYTE = 1024 * 1024

        private const val MIME_IMAGE_PREFIX = "image/"
        private const val MIME_GIF = "image/gif"
        private const val MIME_SVG = "image/svg+xml"
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
        private const val MIME_MP4 = "video/mp4"
        private const val MIME_OCTET_STREAM = "application/octet-stream"

        private const val EXTENSION_PNG = "png"
        private const val EXTENSION_JPG = "jpg"
        private const val EXTENSION_MP4 = "mp4"

        /**
         * Uploaded as is: re-encoding would flatten a GIF's animation, and an SVG is vector, so
         * there's nothing to resize and the image steps can't decode it.
         */
        private val UNPROCESSED_IMAGE_TYPES = setOf(MIME_GIF, MIME_SVG)

        /** Formats androidx ExifInterface can rewrite; HEIC/HEIF would fail and keep their GPS. */
        private val EXIF_MIME_TYPES = setOf(MIME_JPEG, MIME_PNG, "image/webp")
    }
}

/** Rejects an upload; GutenbergKit shows [message] as an editor notice, so it must be localized. */
class GBKMediaUploadException(message: String) : Exception(message)
