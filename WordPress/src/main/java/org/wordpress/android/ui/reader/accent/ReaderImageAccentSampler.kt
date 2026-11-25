package org.wordpress.android.ui.reader.accent

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    // sample URL (sized small) by the stored image URL, waiting to be sampled
    private val queue = LinkedHashMap<String, String>()
    private val queueLock = Any()
    private val draining = Mutex()

    /**
     * Queues [imageUrl] for sampling unless it's been sampled, or failed recently. [sampleUrl] is the URL to load,
     * small and uncropped.
     */
    fun enqueue(imageUrl: String, sampleUrl: String) {
        val existing = store.get(imageUrl)
        if (existing != null && !existing.isRetryDue()) return
        val isNew = synchronized(queueLock) { queue.put(imageUrl, sampleUrl) == null }
        if (isNew) applicationScope.launch(bgDispatcher) { drain() }
    }

    private suspend fun drain() {
        // lets a page of cards queue their images, so they're sampled together
        delay(BATCH_DELAY_MS)
        draining.withLock {
            while (true) {
                val batch = synchronized(queueLock) {
                    queue.entries.take(PARALLEL).map { it.key to it.value }.also { taken ->
                        taken.forEach { queue.remove(it.first) }
                    }
                }
                if (batch.isEmpty()) return
                val results = coroutineScope {
                    batch.map { (imageUrl, sampleUrl) ->
                        async { imageUrl to (sample(sampleUrl) ?: failed()) }
                    }.awaitAll()
                }
                store.putAll(results.toMap())
            }
        }
    }

    private suspend fun sample(url: String): ReaderImageMeta? = try {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(SAMPLE_SIZE_PX)
            .allowHardware(false)
            .build()
        val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        bitmap?.let { sample(it) }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        AppLog.w(T.READER, "Couldn't sample $url: $e")
        null
    }

    private suspend fun sample(bitmap: Bitmap): ReaderImageMeta? = withContext(bgDispatcher) {
        if (bitmap.width == 0 || bitmap.height == 0) return@withContext null
        val palette = Palette.from(bitmap).maximumColorCount(PALETTE_COLORS).generate()
        val swatch = palette.darkMutedSwatch ?: palette.mutedSwatch ?: palette.darkVibrantSwatch
            ?: palette.dominantSwatch
        ReaderImageMeta(
            accent = swatch?.rgb?.let(::darkenForWhiteText),
            ratio = bitmap.width.toFloat() / bitmap.height,
        )
    }

    private fun failed() = ReaderImageMeta(accent = null, ratio = null, failedAt = System.currentTimeMillis())

    private fun ReaderImageMeta.isRetryDue() =
        failedAt != null && System.currentTimeMillis() - failedAt > RETRY_AFTER_MS

    companion object {
        private const val PARALLEL = 4
        private const val BATCH_DELAY_MS = 300L
        private const val SAMPLE_SIZE_PX = 96
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

        /** Text sits in white on the accent, so it's kept dark enough for contrast */
        fun darkenForWhiteText(color: Int): Int {
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
