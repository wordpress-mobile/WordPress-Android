package org.wordpress.android.ui.media

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.MediaSettingsActivityBinding
import org.wordpress.android.editor.EditorImageMetaData
import org.wordpress.android.editor.EditorImageMetaData.ARG_EDITOR_IMAGE_METADATA
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.RequestCodes
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.media.MediaPreviewActivity.MediaPreviewSwiped
import org.wordpress.android.util.MediaUtils
import org.wordpress.android.util.extensions.getParcelableExtraCompat
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import org.wordpress.android.util.extensions.onBackPressedCompat

/**
 * Hosts [MediaSettingsFragment] pages: one per item in the media library's list, so the user can swipe between
 * them, or a single page for an image in the editor.
 */
@AndroidEntryPoint
class MediaSettingsActivity : BaseAppCompatActivity() {
    val site: SiteModel by lazy { requireNotNull(intent.getSerializableExtraCompat(WordPress.SITE)) }

    private val mediaIdList: ArrayList<String>? by lazy { intent.getStringArrayListExtra(ARG_ID_LIST) }

    private val editorImageMetaData by lazy {
        intent.getParcelableExtraCompat<EditorImageMetaData>(ARG_EDITOR_IMAGE_METADATA)
    }

    private val mediaId by lazy { intent.getIntExtra(ARG_MEDIA_LOCAL_ID, 0) }

    // a single page in editor mode, since no ID list is passed
    private val pageIds: List<Int> by lazy {
        mediaIdList?.mapNotNull { it.toIntOrNull() }?.takeIf { mediaId in it } ?: listOf(mediaId)
    }

    /** The pages' IDs, for the full-screen preview to page through the same items */
    val previewIdList: ArrayList<String> by lazy { ArrayList(pageIds.map { it.toString() }) }

    private lateinit var binding: MediaSettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MediaSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = PageAdapter()
        if (savedInstanceState == null) {
            binding.viewPager.setCurrentItem(pageIds.indexOf(mediaId), false)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // returns an editor image's settings, which have to be set as the result before finishing
                currentPage()?.saveChanges()
                onBackPressedDispatcher.onBackPressedCompat(this)
            }
        })

        // registered until onDestroy so swipes in the full-screen preview, which covers this, still arrive
        EventBus.getDefault().register(this)
        enableEdgeToEdge()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    /**
     * Enable edge-to-edge API 33+, but only hide the status bar and not the navigation bar
     */
    private fun enableEdgeToEdge() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter
    // requires SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.do_nothing, R.anim.activity_slide_out_to_bottom)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        currentPage()?.onWindowFocusChanged(hasFocus)
    }

    /** FragmentStateAdapter tags each page "f" + its item ID, which is the position by default */
    private fun currentPage() =
        supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}") as? MediaSettingsFragment

    /**
     * The user swiped to another item in the full-screen preview, so show the same item here
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMediaPreviewSwiped(event: MediaPreviewSwiped) {
        val index = pageIds.indexOf(event.mediaId)
        if (index >= 0) {
            binding.viewPager.setCurrentItem(index, false)
        }
    }

    private inner class PageAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = pageIds.size

        override fun createFragment(position: Int): Fragment = editorImageMetaData
            ?.let { MediaSettingsFragment.newInstance(it) }
            ?: MediaSettingsFragment.newInstance(pageIds[position])
    }

    companion object {
        const val RESULT_MEDIA_DELETED = Activity.RESULT_FIRST_USER
        private const val ARG_MEDIA_LOCAL_ID = "media_local_id"
        private const val ARG_ID_LIST = "id_list"

        /** [mediaIdList] is the optional list of local media IDs to swipe through */
        fun showForResult(activity: Activity, site: SiteModel, media: MediaModel, mediaIdList: ArrayList<String>?) {
            // go directly to preview for local images, videos and audio (do nothing for local documents)
            if (MediaUtils.isLocalFile(media.uploadState)) {
                if (MediaUtils.isValidImage(media.filePath) || MediaUtils.isAudio(media.filePath) || media.isVideo) {
                    MediaPreviewActivity.showPreview(activity, site, media.filePath)
                }
                return
            }

            val intent = Intent(activity, MediaSettingsActivity::class.java)
                .putExtra(ARG_MEDIA_LOCAL_ID, media.id)
                .putExtra(WordPress.SITE, site)
            mediaIdList?.let { intent.putStringArrayListExtra(ARG_ID_LIST, it) }
            startForResult(activity, intent)
        }

        fun showForResult(activity: Activity, site: SiteModel, editorMedia: EditorImageMetaData) {
            val intent = Intent(activity, MediaSettingsActivity::class.java)
                .putExtra(WordPress.SITE, site)
                .putExtra(ARG_EDITOR_IMAGE_METADATA, editorMedia)
            startForResult(activity, intent)
        }

        private fun startForResult(activity: Activity, intent: Intent) {
            val options = ActivityOptionsCompat.makeCustomAnimation(
                activity, R.anim.activity_slide_up_from_bottom, R.anim.do_nothing
            )
            ActivityCompat.startActivityForResult(activity, intent, RequestCodes.MEDIA_SETTINGS, options.toBundle())
        }
    }
}
