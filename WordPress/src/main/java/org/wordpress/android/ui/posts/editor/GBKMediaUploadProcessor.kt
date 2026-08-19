package org.wordpress.android.ui.posts.editor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.m4m.IProgressListener
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.util.MediaUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.WPVideoUtils
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.gutenberg.MediaUploadDelegate
import org.wordpress.gutenberg.ProcessedProxyFile
import java.io.File
import kotlin.coroutines.resume

/**
 * Processes device media picked in the GutenbergKit editor before upload, honoring the app's
 * media settings (image optimization, quality, EXIF location stripping, video optimization) the
 * same way the legacy editor's upload pipeline does.
 *
 * Set as [org.wordpress.gutenberg.GutenbergView.mediaUploadDelegate]; GutenbergKit invokes
 * [processFile] for every editor upload and uploads the result itself (this class deliberately
 * does not override `uploadFile`, so GutenbergKit's default uploader posts to `/wp/v2/media`
 * and relays WordPress's raw response to the editor).
 *
 * Contract notes (see GutenbergKit's MediaUploadServer):
 * - Returning [ProcessedProxyFile.Original] makes GutenbergKit forward the original request body
 *   byte-for-byte — mutations to the staged [File] are NOT uploaded. Any change intended for
 *   WordPress must be returned as [ProcessedProxyFile.Processed].
 * - Processed output files are deleted by GutenbergKit after the upload, so they are written to
 *   the cache dir and never registered in the app's media store.
 * - Thrown exceptions are relayed to the editor as an error notice showing the exception message,
 *   so messages must be localized and user-facing.
 */
@Suppress("LongParameterList")
class GBKMediaUploadProcessor(
    private val site: SiteModel,
    private val appContext: Context,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MediaUploadDelegate {
    /**
     * Metadata-only gate GutenbergKit consults before copying an upload to a temp file. Declining
     * makes it relay the original request body straight to WordPress, skipping a copy this
     * delegate would not have used — GIFs, non-media, and any media whose processing is switched
     * off would otherwise pay a full byte-for-byte copy only to be passed through.
     *
     * The media branches mirror the decisions [processVideo] and [processImage] actually make
     * (see [needsVideoFile] / [needsImageFile]) rather than claiming every image and video, so a
     * site with optimization off does not copy uploads it will never touch.
     *
     * This is an optimization hint, never the enforcement point. It sees only the client-supplied
     * mime type and filename, which can disagree with the file's actual bytes, so the free-plan
     * check inside [processFile] stays authoritative — the copy here is a fast path, not a
     * replacement.
     */
    @Suppress("ReturnCount")
    override fun handlesFile(mimeType: String, filename: String): Boolean {
        val resolvedMimeType = resolveMimeType(mimeType, filename)

        // Claim plan-rejected types so processFile still runs and throws the localized rejection.
        // Declining would forward them to WordPress instead, spending a full upload on a file the
        // site's plan won't accept and surfacing the server's untranslated error in place of ours.
        if (isRejectedByFreePlan(resolvedMimeType)) return true

        return when {
            // Never re-encoded; processFile always returns Original.
            resolvedMimeType == MIME_GIF -> false
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> needsVideoFile()
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> needsImageFile(resolvedMimeType)
            // Non-media files (documents, archives, audio on paid plans) upload unchanged.
            else -> false
        }
    }

    /**
     * Whether [processVideo] would actually read the staged file, mirroring its two exits:
     * the duration check (which only measures on a free plan without VideoPress — see
     * [MediaUtilsWrapper.isProhibitedVideoDuration]) and the transcode.
     *
     * With video optimization off on a paid or self-hosted site — a common configuration, since
     * optimization is opt-in — the method returns [ProcessedProxyFile.Original] without touching
     * the file, so claiming it would make GutenbergKit write a full byte-for-byte copy of the
     * upload to the cache dir for nothing. For a multi-gigabyte video that is gigabytes of I/O
     * and a plausible ENOSPC.
     */
    private fun needsVideoFile(): Boolean {
        val durationCheckApplies = site.hasFreePlan && !site.isActiveModuleEnabled(VIDEOPRESS_MODULE)
        return durationCheckApplies || appPrefsWrapper.isVideoOptimize
    }

    /**
     * Whether [processImage] would actually read the staged file, mirroring its three exits:
     * optimization ([WPMediaUtils.getOptimizedMedia] short-circuits when the pref is off),
     * the self-hosted rotation fallback for issue #5737, and the EXIF location strip (which only
     * copies for the formats androidx ExifInterface can rewrite — see [EXIF_MIME_TYPES]).
     *
     * All three must be inapplicable to decline. Dropping the rotation term would regress #5737
     * parity on self-hosted sites, where the server does not rotate for us.
     */
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
            // Never re-encode GIFs — it would flatten animation. Passthrough skips even a copy.
            resolvedMimeType == MIME_GIF -> ProcessedProxyFile.Original
            mediaUtilsWrapper.isVideoMimeType(resolvedMimeType) -> processVideo(file, resolvedMimeType, filename)
            resolvedMimeType.startsWith(MIME_IMAGE_PREFIX) -> processImage(file, resolvedMimeType, filename)
            // Non-media files (documents, archives, audio on paid plans) upload unchanged.
            else -> ProcessedProxyFile.Original
        }
    }

    /**
     * Rejects the one upload class the app can judge better than the server: audio and documents
     * on a free WordPress.com plan, where the restriction is a plan entitlement rather than a
     * format question, so a localized message beats the server's untranslated error.
     *
     * Deliberately narrow. [MediaUtilsWrapper.isMimeTypeSupportedBySitePlan] matches against a
     * closed, hand-maintained table ([org.wordpress.android.fluxc.utils.MimeTypes]) that has
     * drifted from what WordPress accepts — it has no `image/avif` (core-supported since 6.5), no
     * `image/svg+xml`, and no text types at all, and it maps self-hosted to the same document set
     * as WP.com paid. Applying it to every upload therefore rejects files the server would store.
     * GutenbergKit already validates against the site's real `allowedMimeTypes` from
     * `/wp-block-editor/v1/settings` before this delegate runs, so images and videos are left to
     * that check and to the server, which are both authoritative where this table is not.
     *
     * The free-plan test mirrors [WPMediaUtils.getSitePlanForMimeTypes], which selects
     * `WP_COM_FREE` from [SiteUtilsWrapper.onFreePlan] — using `hasFreePlan` here instead would
     * let the gate and the allowlist it guards disagree.
     *
     * [MIME_OCTET_STREAM] is excluded because [resolveMimeType] emits it for uploads whose type
     * could not be resolved at all; it counts as an `application` type but means "unknown bytes",
     * and rejecting it would answer "we could not identify this file" with "this file type is not
     * allowed".
     */
    private fun isRejectedByFreePlan(resolvedMimeType: String): Boolean {
        if (!site.isWPCom || !siteUtilsWrapper.onFreePlan(site)) return false

        val isPlanRestrictedType = resolvedMimeType != MIME_OCTET_STREAM &&
                (mediaUtilsWrapper.isAudioMimeType(resolvedMimeType) ||
                        mediaUtilsWrapper.isApplicationMimeType(resolvedMimeType))

        return isPlanRestrictedType && !mediaUtilsWrapper.isMimeTypeSupportedBySitePlan(site, resolvedMimeType)
    }

    @Suppress("ReturnCount")
    private suspend fun processVideo(file: File, mimeType: String, filename: String): ProcessedProxyFile {
        // Pass the resolved mime type rather than letting the check re-derive "is this a video"
        // from the staged file: that path is an extension-only test, and GutenbergKit names the
        // staged copy after the client-supplied filename, which need not carry one.
        if (mediaUtilsWrapper.isProhibitedVideoDuration(appContext, site, file, mimeType)) {
            throw GBKMediaUploadException(
                appContext.getString(R.string.error_media_video_duration_exceeds_limit)
            )
        }

        // Match the legacy pipeline: transcode only when the user enabled video optimization.
        if (!appPrefsWrapper.isVideoOptimize) return ProcessedProxyFile.Original

        val output = transcodeMutex.withLock { transcodeVideo(file) } ?: return ProcessedProxyFile.Original

        // Match VideoOptimizer: only use the transcoded file when it is actually smaller.
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
     * Transcodes the video per the user's optimization settings, mirroring the legacy
     * [org.wordpress.android.ui.uploads.VideoOptimizer] semantics: any failure (no composer,
     * m4m error) resolves to null so the caller falls back to uploading the original.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun transcodeVideo(input: File): File? = suspendCancellableCoroutine { continuation ->
        // createTempFile rather than MediaUtils.generateTimeStampedFileName, which is only
        // "wp-{currentTimeMillis}.mp4" and collides for two transcodes started in the same
        // millisecond. Uniqueness comes from the filesystem, with no check-then-create race.
        val output = File.createTempFile("wp-", ".mp4", appContext.cacheDir)
        val startTimeMs = System.currentTimeMillis()
        val listener = object : IProgressListener {
            override fun onMediaStart() = Unit
            override fun onMediaProgress(progress: Float) = Unit
            override fun onMediaPause() = Unit

            // onMediaStop fires both on completion (before onMediaDone) and on manual stop, so
            // only onMediaDone/onError complete the coroutine, guarded against double-resume.
            override fun onMediaStop() = Unit

            override fun onMediaDone() {
                trackTranscodeFinished(input, output, startTimeMs, null)
                if (continuation.isActive) continuation.resume(output)
            }

            override fun onError(exception: Exception) {
                AppLog.e(AppLog.T.MEDIA, "GBKMediaUploadProcessor > video transcode failed", exception)
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
            // m4m throws NPEs on some malformed inputs; the legacy pipeline guards this too.
            AppLog.w(AppLog.T.MEDIA, "GBKMediaUploadProcessor > NPE getting composer: ${npe.message}")
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
                AppLog.w(AppLog.T.MEDIA, "GBKMediaUploadProcessor > error stopping composer: ${e.message}")
            }
            output.delete()
        }

        // m4m throws IllegalStateException from start() when it cannot set up the codec (codec
        // unavailable, memory pressure, unsupported track config). Without this guard the
        // exception escapes the coroutine and GutenbergKit's catch-all turns it into a 500 whose
        // raw, untranslated m4m message is shown to the user — losing an upload the legacy
        // pipeline would have completed with the original file. Guard it as VideoOptimizer does.
        try {
            composer.start()
        } catch (e: IllegalStateException) {
            AppLog.e(AppLog.T.MEDIA, "GBKMediaUploadProcessor > failed to start composer", e)
            output.delete()
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /**
     * Mirrors [org.wordpress.android.ui.uploads.VideoOptimizer]'s null-composer event so a
     * GutenbergKit transcode that never starts is as visible in telemetry as a legacy one.
     */
    private fun trackCantOptimize(input: File, wasNpeDetected: Boolean) {
        val properties = analyticsUtilsWrapper.getMediaProperties(true, null, input.absolutePath)
        properties["was_npe_detected"] = wasNpeDetected
        properties[PROPERTY_OPTIMIZER_LIB] = OPTIMIZER_LIB_M4M
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.MEDIA_VIDEO_CANT_OPTIMIZE, properties)
    }

    /**
     * Mirrors [org.wordpress.android.ui.uploads.VideoOptimizer.trackVideoProcessingEvents],
     * including its `input_video_`/`output_video_` property prefixes, so GutenbergKit transcodes
     * are comparable with legacy ones while the rollout measures parity.
     *
     * Output properties are only attached on success: on failure the output file is deleted
     * moments later and its size would be meaningless.
     */
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
        // getOptimizedMedia returns null when optimization is disabled or a no-op. It can also
        // return the *input* path unchanged (GIF-like skips, decode failures inside
        // ImageUtils.optimizeImage) — treat that as "not optimized" too, otherwise the original
        // file would be mislabeled with a corrected JPEG mime type below.
        val optimized = mediaUtilsWrapper.getOptimizedMedia(file.absolutePath, false)
            .toDistinctExistingFile(file)

        if (optimized != null) {
            return processedImage(optimized, filename)
        }

        // With optimization off, WP.com rotates sideways-captured images server-side but
        // self-hosted sites don't, so rotate physically (legacy parity — see issue #5737).
        // Returns null when no rotation is needed.
        if (!site.isWPCom) {
            val rotated = mediaUtilsWrapper.fixOrientationIssue(file.absolutePath, false)
                .toDistinctExistingFile(file)
            if (rotated != null) {
                return processedImage(rotated, filename)
            }
        }

        if (appPrefsWrapper.isStripImageLocation && mimeType in EXIF_MIME_TYPES) {
            // A copy is required: returning Original makes GutenbergKit forward the original
            // request body byte-for-byte, so stripping EXIF from the staged file in place would
            // silently upload the un-stripped bytes.
            val copy = File.createTempFile("gbk-media", ".${file.extension}", appContext.cacheDir)
            // A failed copy (a full disk being the likely cause) would otherwise leave the
            // just-created temp file behind in the cache dir: GutenbergKit only deletes the files
            // it is handed back, and this one never gets returned.
            runCatching { file.copyTo(copy, overwrite = true) }.onFailure {
                copy.delete()
                throw it
            }
            mediaUtilsWrapper.stripImageLocation(copy.absolutePath)
            return ProcessedProxyFile.Processed(copy, mimeType, filename)
        }

        // No-op: optimization off/unneeded, no rotation, no location strip. Passing the original
        // through avoids the needless lossy re-encode the legacy pipeline never did either.
        return ProcessedProxyFile.Original
    }

    /**
     * Resolves an image-processing result to a usable output file, or null to fall back to the
     * original upload.
     *
     * Both [WPMediaUtils.getOptimizedMedia] and [WPMediaUtils.fixOrientationIssue] return the
     * *input* path unchanged in their no-op cases (GIF-like skips, decode failures inside
     * ImageUtils, no rotation needed), which must be read as "not processed" — otherwise the
     * original file would be relabeled with a corrected JPEG mime type by [processedImage].
     *
     * The existence check is the backstop: GutenbergKit names the staged file after the
     * client-supplied multipart filename, which is far less constrained than the MediaStore paths
     * the legacy callers pass in, so a path that cannot be resolved degrades to a clean
     * passthrough instead of an upload of a file that is not there.
     */
    @Suppress("ReturnCount")
    private fun Uri?.toDistinctExistingFile(input: File): File? {
        val resolvedPath = this?.path ?: return null
        if (resolvedPath == input.absolutePath) return null

        val output = File(resolvedPath)
        if (!output.exists()) {
            AppLog.w(
                AppLog.T.MEDIA,
                "GBKMediaUploadProcessor > processed image path does not exist, using the original"
            )
            return null
        }
        return output
    }

    /**
     * Wraps an optimized/rotated image file, stripping GPS EXIF when enabled and correcting the
     * reported mime type and filename: ImageUtils re-encodes PNG to PNG and everything else
     * (including HEIC/WebP) to JPEG bytes, so the metadata sent to WordPress must reflect the
     * actual output format.
     *
     * The format is read off the *output* file rather than predicted from the declared input mime
     * type. ImageUtils picks its encoder from the extension it derives for the output
     * (`resizeImageAndWriteToStream` writes PNG only when that extension is literally "png") and
     * names the output file with the same extension, so the written name is a faithful record of
     * the encode decision. The declared input mime is not: for an extensionless upload
     * `MediaUtils.getMediaFileName` supplies an extension sniffed from the bytes, which can
     * disagree with what the client declared. Labeling from the input therefore produced JPEG
     * bytes tagged `image/png` (and the reverse) — a mislabel WordPress then stores permanently.
     */
    private fun processedImage(output: File, filename: String): ProcessedProxyFile {
        if (appPrefsWrapper.isStripImageLocation) {
            // getOptimizedMedia copies the original's EXIF (including GPS) onto its output, so
            // the strip must run on the output — matching the legacy strip-at-upload behavior.
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
     * Normalizes the client-supplied mime type, falling back to the filename extension when it
     * carries no usable information.
     *
     * The result feeds [isRejectedByFreePlan] and the type routing below. The plan check it
     * performs is an exact, case-sensitive match against a closed allowlist, so anything but a
     * bare lowercase `type/subtype` is rejected outright. Two shapes reach us that it would miss:
     * - Parameters and casing: `Content-Type` may legitimately carry parameters
     *   (`image/jpeg; charset=binary`) and its casing is not significant (RFC 9110 §8.3).
     * - Missing header: GutenbergKit's multipart parser defaults a part with no `Content-Type`
     *   to `text/plain` (RFC 7578 §4.4), and it picks the file part by the presence of a
     *   `filename` parameter, not by content type — so a real image can arrive labeled
     *   `text/plain`. Treat that like the other placeholders and fall back to the extension.
     */
    private fun resolveMimeType(mimeType: String, filename: String): String {
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        if (normalized.isNotBlank() && normalized !in PLACEHOLDER_MIME_TYPES) return normalized

        val extension = filename.substringAfterLast('.', "").lowercase()
        // Fall back to the declared type when the extension resolves to nothing: it is a
        // placeholder, but a placeholder the plan check can still reject coherently, whereas an
        // empty string is neither. getSingleton() is @NonNull on device but null under the unit
        // test stubs, so it is treated as an unresolvable lookup rather than dereferenced.
        val fromExtension = MimeTypeMap.getSingleton()?.getMimeTypeFromExtension(extension)
        return fromExtension ?: normalized.ifBlank { MIME_OCTET_STREAM }
    }

    companion object {
        /**
         * Serializes video transcodes. GutenbergKit's upload server handles requests concurrently,
         * but parallel m4m hardware transcodes are memory/codec-heavy; the legacy pipeline
         * effectively serialized them through the upload queue.
         *
         * Process-wide rather than per-instance: a new processor is constructed for every editor
         * fragment (see GutenbergKitActivity's SectionsPagerAdapter), and GutenbergKitActivity has
         * no launchMode, so instances stack. A per-instance mutex would let two editors transcode
         * in parallel — exactly what this exists to prevent.
         */
        private val transcodeMutex = Mutex()

        /** Site module that lifts the free-plan video duration limit. */
        private const val VIDEOPRESS_MODULE = "videopress"

        /** Matches VideoOptimizer's analytics so GutenbergKit and legacy transcodes compare. */
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

        /** The one extension ImageUtils treats as "encode as PNG"; everything else becomes JPEG. */
        private const val EXTENSION_PNG = "png"

        /**
         * Mime types that carry no usable type information for an upload, so [resolveMimeType]
         * prefers the filename extension over them. `application/octet-stream` is the generic
         * "unknown bytes" type; `text/plain` is the multipart default for a part that sent no
         * `Content-Type` header at all.
         */
        private val PLACEHOLDER_MIME_TYPES = setOf(MIME_OCTET_STREAM, MIME_TEXT_PLAIN)

        /**
         * Formats androidx ExifInterface can actually strip GPS from: saveAttributes() supports
         * only JPEG, PNG, and WebP. HEIC/HEIF are deliberately excluded — the library throws an
         * IOException (swallowed by stripLocation), so listing them would make a doomed copy and
         * upload a still-geotagged file while appearing to honor the strip-location setting.
         */
        private val EXIF_MIME_TYPES = setOf(MIME_JPEG, MIME_PNG, "image/webp")
    }
}

/**
 * Thrown to reject an upload; GutenbergKit relays [message] to the editor as an error notice,
 * so it must be localized and user-facing.
 */
class GBKMediaUploadException(message: String) : Exception(message)
