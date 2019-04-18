package org.wordpress.android.ui.posts

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import org.wordpress.android.databinding.PostListItemCompactBinding
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.posts.PostListItemType.PostListItemUiState

data class PostListItemCompactViewBinder(
    val title: UiString,
    val date: UiString,
    val status: UiString,
    val image: ImageBundle,
    val moreMenuClickHandler: (View) -> Unit,
    val onItemClickHandler: () -> Unit
) {
    fun hasImage(): Boolean {
        return image.url != null
    }
}

data class ImageBundle(
    val url: String?,
    val config: PostViewHolderConfig
)

class PostListItemCompactViewHolder(
    private val binding: PostListItemCompactBinding,
    private val config: PostViewHolderConfig,
    private val uiHelpers: UiHelpers
) : RecyclerView.ViewHolder(binding.root) {
    var currentItem: PostListItemUiState? = null

    fun bind(item: PostListItemUiState) {
        currentItem = item
        binding.binder = binderFromItem(item)
    }

    private fun binderFromItem(item: PostListItemUiState): PostListItemCompactViewBinder {
        return PostListItemCompactViewBinder(
                item.compactData.title ?: UiStringText(""),
                item.compactData.date ?: UiStringText(""),
                statusTextFromItem(item),
                ImageBundle(item.compactData.imageUrl, config),
                ::handleOpenMenuClick,
                item.onSelected
        )
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

    //TODO: Use the status color too!
    private fun statusTextFromItem(item: PostListItemUiState): UiString {
        val context = binding.root.context
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
            val binding = PostListItemCompactBinding.inflate(inflater, parent, false)
            return PostListItemCompactViewHolder(binding, config, uiHelpers)
        }
    }
}
