package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.m4m.IProgressListener
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.WPVideoUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.gutenberg.MediaProcessor
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File
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
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : MediaProcessor {
    /**
     * Metadata-only gate GutenbergKit checks before copying the upload to a temp file. Declining
     * forwards the original body, so this claims only files [processFile] would actually read.
     */
    @Suppress("ReturnCount")
    override fun handlesFile(mimeType: String, filename: String): Boolean {
        val resolvedMimeType = resolveMimeType(mimeType, filename)

        // Claimed so processFile can throw the localized rejection instead of the server's error.
        if (isRejectedByFreePlan(resolvedMimeType)) return true

        return when {
            resolvedMimeType == MIME_GIF -> false
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> needsVideoFile()
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> needsImageFile(resolvedMimeType)
            else -> false
        }
    }

    /** Mirrors [processVideo]'s exits: the free-plan duration check and the opt-in transcode. */
    private fun needsVideoFile(): Boolean {
        val durationCheckApplies = site.hasFreePlan && !site.isActiveModuleEnabled(VIDEOPRESS_MODULE)
        return durationCheckApplies || appPrefsWrapper.isVideoOptimize
    }

    /** Mirrors [processImage]'s exits: optimization, the self-hosted rotation, and the location strip. */
    private fun needsImageFile(resolvedMimeType: String): Boolean {
        val stripsLocation = appPrefsWrapper.isStripImageLocation && resolvedMimeType in EXIF_MIME_TYPES
        return appPrefsWrapper.isImageOptimize || !site.isWPCom || stripsLocation
    }

    override suspend fun processFile(
        file: File,
        mimeType: String,
        filename: String
    ): ProcessedProxyFile = withContext(ioDispatcher) {
        val resolvedMimeType = resolveMimeType(mimeType, filename)

        if (isRejectedByFreePlan(resolvedMimeType)) {
            throw GBKMediaUploadException(appContext.getString(R.string.error_media_file_type_not_allowed))
        }

        when {
            // Re-encoding would flatten the animation.
            resolvedMimeType == MIME_GIF -> ProcessedProxyFile.Original
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> processVideo(file, resolvedMimeType, filename)
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> processImage(file, resolvedMimeType, filename)
            else -> ProcessedProxyFile.Original
        }
    }

    /**
     * Rejects audio and documents on free WordPress.com plans, a plan entitlement the app knows
     * reliably, so the user gets a message in the app's language. Images and videos are left to
     * the server, since the app's `MimeTypes` table lags what WordPress accepts (e.g. AVIF).
     */
    private fun isRejectedByFreePlan(resolvedMimeType: String): Boolean {
        if (!site.isWPCom || !siteUtilsWrapper.onFreePlan(site)) return false

        // octet-stream means the type couldn't be resolved, not that it's disallowed.
        val isPlanRestrictedType = resolvedMimeType != MIME_OCTET_STREAM &&
                (mediaUtilsWrapper.isAudioMimeType(resolvedMimeType) ||
                        mediaUtilsWrapper.isApplicationMimeType(resolvedMimeType))

        return isPlanRestrictedType && !mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(site, resolvedMimeType)
    }

    @Suppress("ReturnCount")
    private suspend fun processVideo(file: File, mimeType: String, filename: String): ProcessedProxyFile {
        if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, file, mimeType)) {
            throw GBKMediaUploadException(
                appContext.getString(R.string.error_media_video_duration_exceeds_limit)
            )
        }

        // Matches the legacy pipeline, where video optimization is opt-in.
        if (!appPrefsWrapper.isVideoOptimize) return ProcessedProxyFile.Original

        val output = transcodeMutex.withLock { transcodeVideo(file) } ?: return ProcessedProxyFile.Original

        // Matches VideoOptimizer: keep the transcode only when it's smaller.
        if (output.length() >= file.length()) {
            output.delete()
            return ProcessedProxyFile.Original
        }

        return ProcessedProxyFile.Processed(
            file = output,
            mimeType = MIME_MP4,
            filename = "${filename.substringBeforeLast('.')}.mp4"
        )
    }

    /**
     * Transcodes per the user's video settings, resolving to null on any failure so the original
     * uploads instead, as [org.wordpress.android.ui.uploads.VideoOptimizer] does.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun transcodeVideo(input: File): File? = suspendCancellableCoroutine { continuation ->
        val output = File.createTempFile("wp-", ".mp4", appContext.cacheDir)
        val startTimeMs = System.currentTimeMillis()
        val listener = object : IProgressListener {
            override fun onMediaStart() = Unit
            override fun onMediaProgress(progress: Float) = Unit
            override fun onMediaPause() = Unit

            // Also fires before onMediaDone, so only onMediaDone and onError complete the coroutine.
            override fun onMediaStop() = Unit

            override fun onMediaDone() {
                trackTranscodeFinished(input, output, startTimeMs, null)
                if (continuation.isActive) continuation.resume(output)
            }

            override fun onError(exception: Exception) {
                AppLog.e(AppLog.T.MEDIA, "GBKMediaProcessor > video transcode failed", exception)
                trackTranscodeFinished(input, output, startTimeMs, exception)
                output.delete()
                if (continuation.isActive) continuation.resume(null)
            }
        }

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
        } catch (npe: NullPointerException) {
            // m4m throws NPEs on some malformed inputs.
            AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > NPE getting composer: ${npe.message}")
            wasNpeDetected = true
            null
        }

        if (composer == null) {
            trackCantOptimize(input, wasNpeDetected)
            output.delete()
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation {
            try {
                composer.stop()
            } catch (e: Exception) {
                AppLog.w(AppLog.T.MEDIA, "GBKMediaProcessor > error stopping composer: ${e.message}")
            }
            output.delete()
        }

        // m4m throws when it can't set up the codec; upload the original rather than fail.
        try {
            composer.start()
        } catch (e: IllegalStateException) {
            AppLog.e(AppLog.T.MEDIA, "GBKMediaProcessor > failed to start composer", e)
            output.delete()
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /** Mirrors VideoOptimizer's null-composer event so the two pipelines compare in telemetry. */
    private fun trackCantOptimize(input: File, wasNpeDetected: Boolean) {
        val properties = analyticsUtilsWrapper.getMediaProperties(true, null, input.absolutePath)
        properties["was_npe_detected"] = wasNpeDetected
        properties[PROPERTY_OPTIMIZER_LIB] = OPTIMIZER_LIB_M4M
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE, properties)
    }

    /** Mirrors VideoOptimizer's events, including its `input_video_`/`output_video_` prefixes. */
    private fun trackTranscodeFinished(input: File, output: File, startTimeMs: Long, exception: Exception?) {
        val properties = mutableMapOf<String, Any?>()
        analyticsUtilsWrapper.getMediaProperties(true, null, input.absolutePath)
            .forEach { (key, value) -> properties["input_video_$key"] = value }

        if (exception == null) {
            analyticsUtilsWrapper.getMediaProperties(true, null, output.absolutePath)
                .forEach { (key, value) -> properties["output_video_$key"] = value }
            properties["saved_megabytes"] = ((input.length() - output.length()) / BYTES_PER_MEGABYTE).toString()
        } else {
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

        // WP.com rotates sideways images server-side; self-hosted sites don't (issue #5737).
        if (!site.isWPCom) {
            val rotated = mediaUtilsWrapper.fixOrientationIssue(file.absolutePath, false)
                .toDistinctExistingFile(file)
            if (rotated != null) {
                return processedImage(rotated, filename)
            }
        }

        if (appPrefsWrapper.isStripImageLocation && mimeType in EXIF_MIME_TYPES) {
            // Strip a copy: an in-place edit is lost when the original body is forwarded.
            val copy = File.createTempFile("gbk-media", ".${file.extension}", appContext.cacheDir)
            runCatching { file.copyTo(copy, overwrite = true) }.onFailure {
                copy.delete()
                throw it
            }
            mediaUtilsWrapper.stripImageLocation(copy.absolutePath)
            return ProcessedProxyFile.Processed(copy, mimeType, filename)
        }

        // Nothing to do, so skip a needless lossy re-encode.
        return ProcessedProxyFile.Original
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
            mediaUtilsWrapper.stripImageLocation(output.absolutePath)
        }

        val basename = filename.substringBeforeLast('.')
        return if (output.extension.lowercase() == EXTENSION_PNG) {
            ProcessedProxyFile.Processed(output, MIME_PNG, "$basename.png")
        } else {
            ProcessedProxyFile.Processed(output, MIME_JPEG, "$basename.jpg")
        }
    }

    /**
     * Normalizes the declared type (parameters, casing) and falls back to the file extension for
     * placeholders, including the `text/plain` a multipart part without a `Content-Type` defaults to.
     */
    private fun resolveMimeType(mimeType: String, filename: String): String {
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        if (normalized.isNotBlank() && normalized !in PLACEHOLDER_MIME_TYPES) return normalized

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

        private const val VIDEOPRESS_MODULE = "videopress"

        private const val PROPERTY_OPTIMIZER_LIB = "optimizer_lib"
        private const val OPTIMIZER_LIB_M4M = "m4m"
        private const val BYTES_PER_MEGABYTE = 1024 * 1024

        private const val MIME_IMAGE_PREFIX = "image/"
        private const val MIME_GIF = "image/gif"
        private const val MIME_PNG = "image/png"
        private const val MIME_JPEG = "image/jpeg"
        private const val MIME_MP4 = "video/mp4"
        private const val MIME_OCTET_STREAM = "application/octet-stream"
        private const val MIME_TEXT_PLAIN = "text/plain"

        private const val EXTENSION_PNG = "png"

        private val PLACEHOLDER_MIME_TYPES = setOf(MIME_OCTET_STREAM, MIME_TEXT_PLAIN)

        /** Formats androidx ExifInterface can rewrite; HEIC/HEIF would fail and keep their GPS. */
        private val EXIF_MIME_TYPES = setOf(MIME_JPEG, MIME_PNG, "image/webp")
    }
}

/** Rejects an upload; GutenbergKit shows [message] as an editor notice, so it must be localized. */
class GBKMediaUploadException(message: String) : Exception(message)
