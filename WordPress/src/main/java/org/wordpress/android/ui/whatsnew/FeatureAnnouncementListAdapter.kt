package org.wordpress.android.ui.whatsnew

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType
import javax.inject.Inject

class FeatureAnnouncementListAdapter(
    val activity: FragmentActivity
) : Adapter<RecyclerView.ViewHolder>() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private var viewModel: FeatureAnnouncementViewModel
    private val list = mutableListOf<FeatureAnnouncementItem>()

    init {
        (activity.applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(activity, viewModelFactory)
                .get(FeatureAnnouncementViewModel::class.java)
    }

    companion object {
        private const val VIEW_TYPE_FEATURE = 0
        private const val VIEW_TYPE_FOOTER = 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == itemCount - 1) {
            return VIEW_TYPE_FOOTER
        }
        return VIEW_TYPE_FEATURE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_FEATURE) {
            FeatureAnnouncementItemViewHolder(parent, imageManager)
        } else {
            FeatureAnnouncementFooterViewHolder(parent, viewModel)
        }
    }

    override fun getItemCount(): Int {
        if (list.isNotEmpty()) {
            return list.size + 1
        }

        return 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FeatureAnnouncementItemViewHolder) {
            holder.bind(list[position])
        } else if (holder is FeatureAnnouncementFooterViewHolder) {
            holder.bind()
        }
    }

    internal fun updateList(items: List<FeatureAnnouncementItem>) {
        list.clear()
        list.addAll(items)
        notifyDataSetChanged()
    }

    class FeatureAnnouncementItemViewHolder(
        parent: ViewGroup,
        val imageManager: ImageManager
    ) : RecyclerView.ViewHolder(
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.feature_announcement_list_item, parent, false)
    ) {
        private val featureIcon: ImageView = itemView.findViewById(R.id.feature_item_icon)
        private val title: TextView = itemView.findViewById(R.id.feature_title)
        private val subtitle: TextView = itemView.findViewById(R.id.feature_subtitle)

        fun bind(featureAnnouncementItem: FeatureAnnouncementItem) {
            title.text = featureAnnouncementItem.title
            subtitle.text = featureAnnouncementItem.subtitle

            imageManager.loadIntoCircle(
                    featureIcon, ImageType.PLAN,
                    StringUtils.notNullStr(featureAnnouncementItem.iconUrl)
            )
        }
    }

    class FeatureAnnouncementFooterViewHolder(
        parent: ViewGroup,
        val viewModel: FeatureAnnouncementViewModel
    ) :
            RecyclerView.ViewHolder(
                    LayoutInflater.from(parent.context)
                            .inflate(R.layout.feature_announcement_list_footer, parent, false)
            ) {
        private val findMoreButton: TextView = itemView.findViewById(R.id.feature_announcement_find_mode_button)

        fun bind() {
            findMoreButton.setOnClickListener {
                viewModel.onFindMoreButtonPressedPressed()
            }
        }
    }
}
