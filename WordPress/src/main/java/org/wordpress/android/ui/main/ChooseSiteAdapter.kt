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
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class ChooseSiteAdapter : RecyclerView.Adapter<ChooseSiteViewHolder>() {
    private val sites = ArrayList<SiteRecord>()

    var mode: ActionMode = ActionMode.None
        private set

    var onReload = {}
    var onSiteClicked: (SiteRecord) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseSiteViewHolder =
        ChooseSiteViewHolder(ItemChooseSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = sites.size

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ChooseSiteViewHolder, position: Int) {
        holder.bind(mode, sites.getOrNull(position - 1), sites[position])
        holder.onPinUpdated = {
            onReload()
        }
        holder.onSiteClicked = {
            onSiteClicked(it)
        }
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

    var onPinUpdated = { _: SiteRecord -> }
    var onSiteClicked = { _: SiteRecord -> }

    init {
        (itemView.context.applicationContext as WordPress).component().inject(this)
    }

    fun bind(mode: ActionMode, previousSite: SiteRecord?, site: SiteRecord) {
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
        } else {
            binding.layoutContainer.setOnClickListener { onSiteClicked(site) }
        }
    }

    private fun SiteRecord?.isPinned(): Boolean = when (this) {
        null -> false
        else -> appPrefs.pinnedSiteLocalIds.contains(localId)
    }
}

sealed class ActionMode {
    data object None : ActionMode()
    data object Pin : ActionMode()
}
