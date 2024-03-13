package org.wordpress.android.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.ItemChooseSiteBinding
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ChooseSiteAdapter : RecyclerView.Adapter<ChooseSiteViewHolder>() {
    private val sites = ArrayList<SiteRecord>()
    private var mode: ActionMode = ActionMode.None
    var onSiteClicked = { _: SiteRecord -> }
    var onSitePinned = { _: Boolean, _: SiteRecord -> }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseSiteViewHolder =
        ChooseSiteViewHolder(ItemChooseSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = sites.size

    override fun onBindViewHolder(holder: ChooseSiteViewHolder, position: Int) {
        holder.bind(mode, sites.getOrNull(position - 1), sites[position])
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSites(newSites: List<SiteRecord>) {
        sites.clear()
        sites.addAll(newSites)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setActionMode(actionMode: ActionMode) {
        mode = actionMode
        notifyDataSetChanged()
    }
}

class ChooseSiteViewHolder(private val binding: ItemChooseSiteBinding) : RecyclerView.ViewHolder(binding.root) {
    @Inject
    lateinit var imageManager: ImageManager

    @Inject
    lateinit var appPrefs: AppPrefsWrapper

    init {
        (itemView.context.applicationContext as WordPress).component().inject(this)
    }

    fun bind(mode: ActionMode, previousSite: SiteRecord?, site: SiteRecord) {
        imageManager.loadImageWithCorners(
            binding.avatar, site.blavatarType, site.blavatarUrl, DisplayUtils.dpToPx(itemView.context, 20)
        )

        when {
            previousSite == null && site.isPinned() -> {
                binding.header.text = "Pinned Sites"
                binding.header.isVisible = true
            }

            (previousSite == null || previousSite.isPinned()) && site.isPinned().not() -> {
                binding.header.text = "All Sites"
                binding.header.isVisible = true
            }

            else -> {
                binding.header.isVisible = false
            }
        }

        binding.textTitle.text = site.blogNameOrHomeURL
        binding.textDomain.text = site.homeURL
        binding.pin.isVisible = mode == ActionMode.Pin

        val isPinned = site.isPinned()
        binding.pin.setImageResource(if (isPinned) R.drawable.pin_filled else R.drawable.pin)
        binding.pin.setOnClickListener {
            if (isPinned) {
                appPrefs.pinnedSiteLocalIds.remove(site.localId)
            } else {
                appPrefs.pinnedSiteLocalIds.add(site.localId)
            }
        }
    }

    private fun SiteRecord?.isPinned(): Boolean = when (this) {
        null -> false
        else -> appPrefs.pinnedSiteLocalIds.contains(localId)
    }
}

sealed class ActionMode {
    object None : ActionMode()
    object Pin : ActionMode()
}
