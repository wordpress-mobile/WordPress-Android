package org.wordpress.android.ui.main

import android.content.res.ColorStateList
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.databinding.ItemChooseSiteBinding
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.isDarkTheme
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ChooseSiteViewHolder(private val binding: ItemChooseSiteBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var appPrefs: AppPrefsWrapper

    var onPinUpdated = { _: SiteRecord -> }
    var onClicked = { _: SiteRecord -> }
    var onLongClicked = { _: SiteRecord -> }

    init {
        (itemView.context.applicationContext as WordPress).component().inject(this)
    }

    fun bind(mode: ActionMode, previousSite: SiteRecord?, site: SiteRecord, selectedId: Int?) {
        handleAvatar(site)

        handleHeader(previousSite, site)

        binding.textTitle.text = site.blogNameOrHomeURL
        binding.textDomain.text = site.homeURL
        binding.pin.isVisible = mode is ActionMode.Pin

        handlePinButton(site)

        if (mode is ActionMode.Pin) {
            binding.layoutContainer.setOnClickListener(null)
            binding.layoutContainer.setOnLongClickListener(null)
        } else {
            binding.layoutContainer.setOnClickListener { onClicked(site) }
            binding.layoutContainer.setOnLongClickListener {
                onLongClicked(site)
                true
            }
        }

        handleHighlight(site, selectedId)
    }

    private fun handlePinButton(site: SiteRecord) {
        val isPinned = site.isPinned()
        binding.pin.setImageResource(if (isPinned) R.drawable.pin_filled else R.drawable.pin)
        binding.pin.setOnClickListener {
            if (isPinned) {
                appPrefs.pinnedSiteLocalIds = appPrefs.pinnedSiteLocalIds.apply { remove(site.localId) }
                AnalyticsTracker.track(
                    AnalyticsTracker.Stat.SITE_SWITCHER_PIN_UPDATED,
                    mapOf(
                        TRACK_PROPERTY_BLOG_ID to site.siteId,
                        TRACK_PROPERTY_PINNED to false
                    )
                )
            } else {
                appPrefs.pinnedSiteLocalIds = appPrefs.pinnedSiteLocalIds.apply { add(site.localId) }
                AnalyticsTracker.track(
                    AnalyticsTracker.Stat.SITE_SWITCHER_PIN_UPDATED,
                    mapOf(
                        TRACK_PROPERTY_BLOG_ID to site.siteId,
                        TRACK_PROPERTY_PINNED to true
                    )
                )
            }
            onPinUpdated(site)
        }
        val color = if (isPinned) binding.root.context.getColor(R.color.inline_action_filled)
        else binding.root.context.getColorFromAttribute(R.attr.wpColorOnSurfaceMedium)
        ImageViewCompat.setImageTintList(binding.pin, ColorStateList.valueOf(color))
    }

    private fun handleAvatar(site: SiteRecord) {
        imageManager.load(binding.avatar, site.blavatarType, site.blavatarUrl)
        val isDarkTheme = itemView.resources.configuration.isDarkTheme()
        val borderColor = ContextCompat.getColor(
            itemView.context,
            if (isDarkTheme) R.color.white_translucent_10
            else R.color.black_translucent_10
        )
        binding.avatar.strokeColor = ColorStateList.valueOf(borderColor)
    }

    private fun handleHeader(previousSite: SiteRecord?, site: SiteRecord) {
        when {
            previousSite == null && site.isPinned() -> {
                binding.header.text = itemView.context.getString(R.string.site_picker_pinned_sites)
                binding.header.isVisible = true
                setHeaderTopMargin(previousSite)
            }

            (previousSite == null || previousSite.isPinned()) && site.isPinned().not() -> {
                binding.header.text = itemView.context.getString(R.string.site_picker_recent_sites)
                binding.header.isVisible = true
                setHeaderTopMargin(previousSite)
            }

            (previousSite == null || previousSite.isRecent() || previousSite.isPinned()) &&
                    site.isPinned().not() &&
                    site.isRecent().not() -> {
                binding.header.text = itemView.context.getString(R.string.site_picker_all_sites)
                binding.header.isVisible = true
                setHeaderTopMargin(previousSite)
            }

            else -> {
                binding.header.isVisible = false
            }
        }
    }

    private fun setHeaderTopMargin(previousSite: SiteRecord?) {
        val resId = previousSite?.let { R.dimen.margin_extra_large } ?: R.dimen.margin_small
        (binding.header.layoutParams as MarginLayoutParams).apply {
            setMargins(
                leftMargin,
                itemView.context.resources.getDimensionPixelSize(resId),
                rightMargin,
                bottomMargin
            )
        }.let { binding.header.layoutParams = it }
    }

    private fun handleHighlight(site: SiteRecord, selectedId: Int?) {
        val isSelected = site.localId == (selectedId ?: appPrefs.getSelectedSite())
        if (isSelected) {
            // highlight the selected site
            ColorUtils.setAlphaComponent(
                itemView.context.getColorFromAttribute(com.google.android.material.R.attr.colorOnSurface),
                itemView.context.resources.getInteger(R.integer.selected_list_item_opacity)
            ).let { color ->
                binding.layoutContainer.setBackgroundColor(color)
            }
        } else {
            // clear the highlight
            binding.layoutContainer.background = null
        }
    }

    private fun SiteRecord?.isPinned(): Boolean = when (this) {
        null -> false
        else -> appPrefs.pinnedSiteLocalIds.contains(localId)
    }

    private fun SiteRecord?.isRecent(): Boolean = when (this) {
        null -> false
        else -> appPrefs.getRecentSiteLocalIds().contains(localId)
    }

    companion object {
        private const val TRACK_PROPERTY_BLOG_ID = "blog_id"
        private const val TRACK_PROPERTY_PINNED = "pinned"
    }
}
