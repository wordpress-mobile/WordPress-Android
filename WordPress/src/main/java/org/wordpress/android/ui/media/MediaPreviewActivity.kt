package org.wordpress.android.ui.media

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityOptionsCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.media.preview.MediaPreviewItem
import org.wordpress.android.ui.media.preview.MediaPreviewKind
import org.wordpress.android.ui.media.preview.MediaPreviewScreen
import org.wordpress.android.ui.media.preview.MediaPreviewSource
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPMediaUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject
import kotlin.math.max

/**
 * Full-screen preview of an image, video or audio file, which can also page through a list of local media IDs
 */
@AndroidEntryPoint
class MediaPreviewActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var mediaStore: MediaStore

    @Inject
    lateinit var exoPlayerUtils: ExoPlayerUtils

    private var site: SiteModel? = null
    private var maxImageWidth = 0

    /** posted when the user swipes to another item, so the media details screen can follow along */
    class MediaPreviewSwiped(@JvmField val mediaId: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val contentUri = intent.getStringExtra(ARG_MEDIA_CONTENT_URI)
        if (contentUri.isNullOrEmpty()) {
            ToastUtils.showToast(this, R.string.error_media_not_found)
            finish()
            return
        }

        site = intent.getSerializableExtraCompat(WordPress.SITE)
        // use the larger window dimension so the image is cached the same regardless of orientation
        maxImageWidth = max(DisplayUtils.getWindowPixelWidth(this), DisplayUtils.getWindowPixelHeight(this))

        val mediaId = intent.getIntExtra(ARG_MEDIA_ID, 0)
        val mediaIds = intent.getStringArrayListExtra(ARG_ID_LIST)?.mapNotNull { it.toIntOrNull() }
        val sources = if (mediaIds != null && mediaIds.size > 1) {
            mediaIds.map { MediaPreviewSource(mediaId = it, fallbackUri = null) }
        } else {
            listOf(MediaPreviewSource(mediaId = mediaId, fallbackUri = contentUri))
        }
        val initialPage = sources.indexOfFirst { it.mediaId == mediaId }.coerceAtLeast(0)

        setContent {
            AppThemeM3(isDarkTheme = true) {
                MediaPreviewScreen(
                    sources = sources,
                    initialPage = initialPage,
                    resolve = ::resolve,
                    buildMediaSource = exoPlayerUtils::buildMediaSource,
                    onPageChanged = { page ->
                        if (sources.size > 1) {
                            EventBus.getDefault().post(MediaPreviewSwiped(sources[page].mediaId))
                        }
                    },
                    onBack = onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter
    // requires SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * Looks up the media model and works out whether this is an image, a video or an audio file. Runs off the main
     * thread since it reads the database and may ask a content provider for the MIME type.
     */
    private suspend fun resolve(source: MediaPreviewSource): MediaPreviewItem = withContext(Dispatchers.IO) {
        val media = source.mediaId.takeIf { it != 0 }?.let { mediaStore.getMediaWithLocalId(it) }
        val uri = media?.url?.takeIf { it.isNotEmpty() } ?: source.fallbackUri.orEmpty()
        val kind = resolveKind(uri)
        MediaPreviewItem(
            uri = if (kind == MediaPreviewKind.IMAGE) getImageUrl(uri) else uri,
            kind = kind,
            title = media?.title
        )
    }

    /**
     * The MediaUtils checks look at the file extension, which a content:// URI doesn't have, so ask the content
     * provider for the MIME type first. Only a type we recognize is trusted: anything else (application/octet-stream
     * and the like) falls through to the extension checks rather than silently counting as an image.
     */
    private fun resolveKind(uri: String): MediaPreviewKind {
        val mimeType = WPMediaUtils.resolveMimeType(applicationContext, uri)
        return when {
            mimeType?.startsWith("image/") == true -> MediaPreviewKind.IMAGE
            mimeType?.startsWith("video/") == true -> MediaPreviewKind.VIDEO
            mimeType?.startsWith("audio/") == true -> MediaPreviewKind.AUDIO
            MediaUtils.isVideo(uri) -> MediaPreviewKind.VIDEO
            MediaUtils.isAudio(uri) -> MediaPreviewKind.AUDIO
            else -> MediaPreviewKind.IMAGE
        }
    }

    private fun getImageUrl(uri: String): String {
        val site = site
        val canUsePhoton = site == null || SiteUtils.isPhotonCapable(site)
        return if (uri.isNotEmpty() && canUsePhoton && !UrlUtils.isContentUri(uri)) {
            PhotonUtils.getPhotonImageUrl(uri, maxImageWidth, 0, site?.isPrivateWPComAtomic == true)
        } else {
            uri
        }
    }

    companion object {
        private const val ARG_MEDIA_CONTENT_URI = "content_uri"
        private const val ARG_MEDIA_ID = "media_id"
        private const val ARG_ID_LIST = "id_list"

        /**
         * @param site optional site this media is associated with
         * @param contentUri URI of the media - can be local or remote
         */
        @JvmStatic
        fun showPreview(context: Context, site: SiteModel?, contentUri: String) {
            val intent = Intent(context, MediaPreviewActivity::class.java)
                .putExtra(ARG_MEDIA_CONTENT_URI, contentUri)
            site?.let { intent.putExtra(WordPress.SITE, it) }
            startIntent(context, intent)
        }

        /**
         * @param site optional site this media is associated with
         * @param media initial media model to show
         * @param mediaIdList optional list of local media IDs to page through
         */
        @JvmStatic
        fun showPreview(context: Context, site: SiteModel?, media: MediaModel, mediaIdList: ArrayList<String>?) {
            val intent = Intent(context, MediaPreviewActivity::class.java)
                .putExtra(ARG_MEDIA_ID, media.id)
                .putExtra(ARG_MEDIA_CONTENT_URI, media.url)
            site?.let { intent.putExtra(WordPress.SITE, it) }
            if (mediaIdList != null && mediaIdList.size > 1) {
                intent.putStringArrayListExtra(ARG_ID_LIST, mediaIdList)
            }
            startIntent(context, intent)
        }

        private fun startIntent(context: Context, intent: Intent) {
            val options = ActivityOptionsCompat.makeCustomAnimation(context, R.anim.fade_in, R.anim.fade_out)
            context.startActivity(intent, options.toBundle())
        }
    }
}
