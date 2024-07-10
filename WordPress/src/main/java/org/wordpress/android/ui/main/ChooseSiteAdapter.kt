package org.wordpress.android.ui.main

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.databinding.ItemChooseSiteBinding

class ChooseSiteAdapter : RecyclerView.Adapter<ChooseSiteViewHolder>() {
    private val sites = ArrayList<SiteRecord>()

    var mode: ActionMode = ActionMode.None
        private set

    var onReload = {}
    var onSiteClicked: (SiteRecord) -> Unit = {}
    var onSiteLongClicked: (SiteRecord) -> Unit = {}
    var selectedSiteId: Int? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChooseSiteViewHolder =
        ChooseSiteViewHolder(ItemChooseSiteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = sites.size

    override fun onBindViewHolder(holder: ChooseSiteViewHolder, position: Int) {
        holder.bind(mode, sites.getOrNull(position - 1), sites[position], selectedSiteId)
        holder.onPinUpdated = { onReload() }
        holder.onClicked = { onSiteClicked(it) }
        holder.onLongClicked = { onSiteLongClicked(it) }
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

/**
 * For displaying the UI of "Edit Pins"
 */
sealed class ActionMode(val value: String) {
    data object None : ActionMode(NONE)
    data object Pin : ActionMode(PIN)

    companion object {
        fun from(value: String): ActionMode = when (value) {
            PIN -> Pin
            NONE -> None
            else -> throw IllegalArgumentException("Unknown value: $value")
        }

        private const val PIN = "pin"
        private const val NONE = "none"
    }
}
