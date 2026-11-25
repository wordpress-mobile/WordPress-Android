package org.wordpress.android.ui.reader.accent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Samples featured images for their accent color and aspect ratio, a few at a time in the background, and saves
 * what it finds in [ReaderImageMetaStore]. A failure is retried once [RETRY_AFTER_MS] has passed, rather than
 * being stored as a fallback color forever.
 *
 * Ported from WPFolio's AccentSampler.
 */
@Singleton
class ReaderImageAccentSampler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: ReaderImageMetaStore,
    @Named(APPLICATION_SCOPE) private val applicationScope: CoroutineScope,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    // guarded by itself: the sample URL (small, uncropped) by the image URL, waiting to be sampled
    private val queue = LinkedHashMap<String, String>()

    // guarded by queue: the images being sampled now, and the one job draining the queue
    private val inFlight = HashSet<String>()
    private var drainJob: Job? = null

    /**
     * Queues [imageUrl] for sampling unless it's been sampled, failed recently, or is already queued. [sampleUrl]
     * is only built when it's needed.
     */
    fun enqueue(imageUrl: String, sampleUrl: () -> String) {
        if (store.get(imageUrl)?.isRetryDue() == false) return
        synchronized(queue) {
            if (imageUrl in inFlight || imageUrl in queue) return
            queue[imageUrl] = sampleUrl()
            if (drainJob == null) drainJob = applicationScope.launch(bgDispatcher) { drain() }
        }
    }

    private suspend fun drain() {
        // lets a page of cards queue their images, so they're sampled together
        delay(BATCH_DELAY_MS)
        while (true) {
            val batch = synchronized(queue) {
                queue.entries.take(PARALLEL).map { it.key to it.value }.also { taken ->
                    taken.forEach { (imageUrl, _) ->
                        queue.remove(imageUrl)
                        inFlight.add(imageUrl)
                    }
                    if (taken.isEmpty()) drainJob = null
                }
            }
            if (batch.isEmpty()) return
            val results = coroutineScope {
                batch.map { (imageUrl, sampleUrl) ->
                    async { imageUrl to (sample(sampleUrl) ?: ReaderImageMeta(accent = null, ratio = null)) }
                }.awaitAll()
            }
            store.putAll(results.toMap())
            synchronized(queue) { inFlight.removeAll(results.map { it.first }.toSet()) }
        }
    }

    private suspend fun sample(url: String): ReaderImageMeta? = try {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(SAMPLE_SIZE_PX)
            .allowHardware(false)
            // never shown, so kept out of the caches the cards use
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.let(::sample)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        AppLog.w(T.READER, "Couldn't sample $url: $e")
        null
    }

    private fun sample(bitmap: Bitmap): ReaderImageMeta? {
        if (bitmap.width == 0 || bitmap.height == 0) return null
        val palette = Palette.from(bitmap).maximumColorCount(PALETTE_COLORS).generate()
        val swatch = palette.darkMutedSwatch ?: palette.mutedSwatch ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        return ReaderImageMeta(
            accent = swatch?.rgb?.let(::darkenForWhiteText),
            ratio = bitmap.width.toFloat() / bitmap.height,
        )
    }

    private fun ReaderImageMeta.isRetryDue() =
        ratio == null && System.currentTimeMillis() - sampledAt > RETRY_AFTER_MS

    companion object {
        /** The width to ask Photon for, which is all the sample needs */
        const val SAMPLE_SIZE_PX = 96
        private const val PARALLEL = 4
        private const val BATCH_DELAY_MS = 300L
        private const val PALETTE_COLORS = 16
        private const val MAX_LUMINANCE = 0.12
        private const val DARKEN_STEP = 0.15f
        private const val MAX_DARKEN_STEPS = 10
        private const val RETRY_AFTER_MS = 6L * 60 * 60 * 1000

        /** Accents from WPFolio's design, for an image that hasn't been or can't be sampled */
        private val FALLBACKS = intArrayOf(
            0xFF3D4F6B.toInt(), 0xFF7A3B1F.toInt(), 0xFF5B3A24.toInt(), 0xFF6B5A3E.toInt(),
            0xFF2F4A36.toInt(), 0xFF1F2A4A.toInt(), 0xFF2E3440.toInt(), 0xFF5A2430.toInt(),
            0xFF6B4A52.toInt(), 0xFF2C5560.toInt(), 0xFF3F5A2A.toInt(),
        )

        /** A stable accent for [key], such as a post with no image */
        fun fallbackAccent(key: String): Int = FALLBACKS[Math.floorMod(key.hashCode(), FALLBACKS.size)]

        /* text sits in white on the accent, so it's kept dark enough for contrast */
        private fun darkenForWhiteText(color: Int): Int {
            var darkened = color
            var steps = 0
            while (ColorUtils.calculateLuminance(darkened) > MAX_LUMINANCE && steps < MAX_DARKEN_STEPS) {
                darkened = ColorUtils.blendARGB(darkened, android.graphics.Color.BLACK, DARKEN_STEP)
                steps++
            }
            return darkened
        }
    }
}
