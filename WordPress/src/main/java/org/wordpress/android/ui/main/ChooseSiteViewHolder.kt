package org.wordpress.android.ui.main

import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ItemChooseSiteBinding
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.extensions.getColorFromAttribute
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
        imageManager.loadImageWithCorners(
            binding.avatar, site.blavatarType, site.blavatarUrl,
            itemView.context.resources.getDimensionPixelSize(R.dimen.blavatar_sz) / 2
        )

        when {
            previousSite == null && site.isPinned() -> {
                binding.header.text = itemView.context.getString(R.string.pinned_sites)
                binding.header.isVisible = true
            }

            (previousSite == null || previousSite.isPinned()) && site.isPinned().not() -> {
                binding.header.text = itemView.context.getString(R.string.all_sites)
                binding.header.isVisible = true
            }

            else -> {
                binding.header.isVisible = false
            }
        }

        binding.textTitle.text = site.blogNameOrHomeURL
        binding.textDomain.text = site.homeURL
        binding.pin.isVisible = mode is ActionMode.Pin

        val isPinned = site.isPinned()
        binding.pin.setImageResource(if (isPinned) R.drawable.pin_filled else R.drawable.pin)
        binding.pin.setOnClickListener {
            if (isPinned) {
                appPrefs.pinnedSiteLocalIds = appPrefs.pinnedSiteLocalIds.apply { remove(site.localId) }
            } else {
                appPrefs.pinnedSiteLocalIds = appPrefs.pinnedSiteLocalIds.apply { add(site.localId) }
            }
            onPinUpdated(site)
        }

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
            binding.textTitle.setTypeface(null, Typeface.BOLD)
        } else {
            // clear the highlight
            binding.layoutContainer.background = null
            binding.textTitle.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun SiteRecord?.isPinned(): Boolean = when (this) {
        null -> false
        else -> appPrefs.pinnedSiteLocalIds.contains(localId)
    }
}
