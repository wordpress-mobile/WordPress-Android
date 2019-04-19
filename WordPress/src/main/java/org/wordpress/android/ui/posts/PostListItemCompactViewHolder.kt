package org.wordpress.android.ui.posts

import android.support.annotation.ColorRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.PopupMenu
import org.wordpress.android.R
import org.wordpress.android.ui.reader.utils.ReaderUtils
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.image.ImageType
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState
import org.wordpress.android.widgets.WPTextView

data class PostListItemCompactViewBinder(
    val title: UiString,
    val date: UiString,
    val status: UiString,
    @ColorRes val statusColor: Int,
    val image: ImageBundle,
    val moreMenuClickHandler: (View) -> Unit,
    val onItemClickHandler: () -> Unit
)

data class ImageBundle(
    val url: String?,
    val config: PostViewHolderConfig
)

class PostListItemCompactViewHolder(
    private val view: View,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(view) {
    private val background: View = view.findViewById(R.id.background)
    private val titleTextView: WPTextView = view.findViewById(R.id.title)
    private val dateTextView: WPTextView = view.findViewById(R.id.date)
    private val statusesTextView: WPTextView = view.findViewById(R.id.statuses_label)
    private val imageView: ImageView = view.findViewById(R.id.image_view)
    private val moreButton: ImageButton = view.findViewById(R.id.more_button)

    private var currentItem: PostListItemUiState? = null

    init {
        background.setOnClickListener { currentItem?.onSelected?.invoke() }
        moreButton.setOnClickListener { handleOpenMenuClick(it) }
    }

    fun bind(item: PostListItemUiState) {
        currentItem = item

        val binder = binderFromItem(item)
        bindBinder(binder)
    }

    private fun bindBinder(binder: PostListItemCompactViewBinder) {
        titleTextView.text = stringFromUiString(binder.title)
        dateTextView.text = stringFromUiString(binder.date)
        statusesTextView.text = stringFromUiString(binder.status)
        statusesTextView.setTextColor(view.context.resources.getColor(binder.statusColor))
        imageView.visibility = if (binder.image.url != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
        bindPostsImage(binder.image)
    }

    private fun stringFromUiString(uiString: UiString): String = when (uiString) {
        is UiString.UiStringRes -> view.context.getString(uiString.stringRes)
        is UiString.UiStringText -> uiString.text
    }

    private fun binderFromItem(item: PostListItemUiState): PostListItemCompactViewBinder {
        return PostListItemCompactViewBinder(
                item.compactData.title ?: UiStringText(""),
                item.compactData.date ?: UiStringText(""),
                statusTextFromItem(item),
                item.compactData.statusesColor ?: R.color.warning_500,
                ImageBundle(item.compactData.imageUrl, config),
                ::handleOpenMenuClick,
                item.onSelected
        )
    }

    fun bindPostsImage(bundle: ImageBundle) {
        bundle.url?.let { url ->
            val config = bundle.config
            if (url.startsWith("http")) {
                val photonUrl = ReaderUtils.getResizedImageUrl(
                        url, config.photonWidth, config.photonHeight, !config.isPhotonCapable
                )
                config.imageManager.load(imageView, ImageType.PHOTO, photonUrl, ScaleType.CENTER_CROP)
            } else {
                ImageUtils.getWPImageSpanThumbnailFromFilePath(
                        imageView.context, url, config.photonWidth
                )?.let {
                    config.imageManager.load(imageView, it)
                }
            }
        } ?: bundle.config.imageManager.cancelRequestAndClearImageView(imageView)
    }

    private fun handleOpenMenuClick(view: View) {
        val menu = PopupMenu(view.context, view)
        currentItem?.compactActions?.forEach { action ->
            val menuItem = menu.menu.add(
                    Menu.NONE,
                    action.buttonType.value,
                    Menu.NONE,
                    action.buttonType.textResId
            )
            menuItem.setOnMenuItemClickListener {
                action.onButtonClicked.invoke(action.buttonType)
                true
            }
        }
        menu.show()
    }

    private fun statusTextFromItem(item: PostListItemUiState): UiString {
        val context = view.context
        val separator = uiHelpers.getTextOfUiString(context, item.data.statusesDelimiter)
        val statusText = item.compactData.statuses.joinToString(separator) { uiHelpers.getTextOfUiString(context, it) }
        return UiString.UiStringText(statusText)
    }

    companion object {
        @JvmStatic
        fun create(
            inflater: LayoutInflater,
            parent: ViewGroup,
            config: PostViewHolderConfig,
            uiHelpers: UiHelpers
        ): PostListItemCompactViewHolder {
            val view = inflater.inflate(R.layout.post_list_item_compact, parent, false)
            return PostListItemCompactViewHolder(view, config, uiHelpers)
        }
    }
}
