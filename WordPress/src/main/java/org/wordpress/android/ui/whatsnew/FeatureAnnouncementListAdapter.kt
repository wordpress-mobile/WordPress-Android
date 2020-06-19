package org.wordpress.android.ui.whatsnew

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
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
    val fragment: Fragment
) : Adapter<RecyclerView.ViewHolder>() {
    @Inject lateinit var imageManager: ImageManager
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private var viewModel: FeatureAnnouncementViewModel
    private val list = mutableListOf<FeatureAnnouncementItem>()
    private var isFindOutMoreVisible = true

    init {
        (fragment.requireActivity().applicationContext as WordPress).component().inject(this)
        viewModel = ViewModelProviders.of(fragment, viewModelFactory)
                .get(FeatureAnnouncementViewModel::class.java)
    }

    companion object {
        private const val VIEW_TYPE_FEATURE = 0
        private const val VIEW_TYPE_FOOTER = 1
    }

    override fun getItemViewType(position: Int): Int {
        if (isFindOutMoreVisible && position == itemCount - 1) {
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

    fun toggleFooterVisibility(isVisible: Boolean) {
        isFindOutMoreVisible = isVisible
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        if (list.isNotEmpty() && isFindOutMoreVisible) {
            return list.size + 1
        } else if (list.isNotEmpty() && !isFindOutMoreVisible) {
            return list.size
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

            if (!TextUtils.isEmpty(featureAnnouncementItem.iconUrl)) {
                imageManager.loadIntoCircle(
                        featureIcon, ImageType.PLAN,
                        StringUtils.notNullStr(featureAnnouncementItem.iconUrl)
                )
            } else {
                val originalBase64String = StringUtils.notNullStr(featureAnnouncementItem.iconBase64)
                val sanitizedBase64String = originalBase64String.replace("data:image/png;base64,", "")

                imageManager.loadBase64IntoCircle(featureIcon, ImageType.PLAN, sanitizedBase64String)
            }
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
                viewModel.onFindMoreButtonPressed()
            }
        }
    }
}
