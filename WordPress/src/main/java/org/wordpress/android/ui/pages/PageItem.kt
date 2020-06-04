package org.wordpress.android.ui.pages

import androidx.annotation.ColorRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.ui.pages.PageItem.Type.DIVIDER
import org.wordpress.android.ui.pages.PageItem.Type.EMPTY
import org.wordpress.android.ui.pages.PageItem.Type.PAGE
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.Date

sealed class PageItem(open val type: Type) {
    abstract class Page(
        open val remoteId: Long,
        open val localId: Int,
        open val title: String,
        open val date: Date,
        open val labels: List<UiString>,
        @ColorRes open val labelsColor: Int?,
        open var indent: Int,
        open var imageUrl: String?,
        open val actions: Set<Action>,
        open var actionsEnabled: Boolean,
        open val tapActionEnabled: Boolean,
        open val progressBarUiState: ProgressBarUiState,
        open val showOverlay: Boolean
    ) : PageItem(PAGE)

    data class PublishedPage(
        override val remoteId: Long,
        override val localId: Int,
        override val title: String,
        override val date: Date,
        override val labels: List<UiString> = emptyList(),
        override val labelsColor: Int? = null,
        override var indent: Int = 0,
        override var imageUrl: String? = null,
        override val actions: Set<Action>,
        override var actionsEnabled: Boolean = true,
        override val progressBarUiState: ProgressBarUiState,
        override val showOverlay: Boolean
    ) : Page(
            remoteId = remoteId,
            localId = localId,
            title = title,
            date = date,
            labels = labels,
            labelsColor = labelsColor,
            indent = indent,
            imageUrl = imageUrl,
            actions = actions,
            actionsEnabled = actionsEnabled,
            tapActionEnabled = true,
            progressBarUiState = progressBarUiState,
            showOverlay = showOverlay
    )

    data class DraftPage(
        override val remoteId: Long,
        override val localId: Int,
        override val title: String,
        override val date: Date,
        override val labels: List<UiString> = emptyList(),
        override val labelsColor: Int? = null,
        override var imageUrl: String? = null,
        override val actions: Set<Action>,
        override var actionsEnabled: Boolean = true,
        override val progressBarUiState: ProgressBarUiState,
        override val showOverlay: Boolean
    ) : Page(
            remoteId = remoteId,
            localId = localId,
            title = title,
            date = date,
            labels = labels,
            labelsColor = labelsColor,
            indent = 0,
            imageUrl = imageUrl,
            actions = actions,
            actionsEnabled = actionsEnabled,
            tapActionEnabled = true,
            progressBarUiState = progressBarUiState,
            showOverlay = showOverlay
    )

    data class ScheduledPage(
        override val remoteId: Long,
        override val localId: Int,
        override val title: String,
        override val date: Date,
        override val labels: List<UiString> = emptyList(),
        override val labelsColor: Int? = null,
        override var imageUrl: String? = null,
        override val actions: Set<Action>,
        override var actionsEnabled: Boolean = true,
        override val progressBarUiState: ProgressBarUiState,
        override val showOverlay: Boolean
    ) : Page(
            remoteId = remoteId,
            localId = localId,
            title = title,
            date = date,
            labels = labels,
            labelsColor = labelsColor,
            indent = 0,
            imageUrl = imageUrl,
            actions = actions,
            actionsEnabled = actionsEnabled,
            tapActionEnabled = true,
            progressBarUiState = progressBarUiState,
            showOverlay = showOverlay
    )

    data class TrashedPage(
        override val remoteId: Long,
        override val localId: Int,
        override val title: String,
        override val date: Date,
        override val labels: List<UiString> = emptyList(),
        override val labelsColor: Int? = null,
        override var imageUrl: String? = null,
        override val actions: Set<Action>,
        override var actionsEnabled: Boolean = true,
        override val progressBarUiState: ProgressBarUiState,
        override val showOverlay: Boolean
    ) : Page(
            remoteId = remoteId,
            localId = localId,
            title = title,
            date = date,
            labels = labels,
            labelsColor = labelsColor,
            indent = 0,
            imageUrl = imageUrl,
            actions = actions,
            actionsEnabled = actionsEnabled,
            tapActionEnabled = false,
            progressBarUiState = progressBarUiState,
            showOverlay = showOverlay
    )

    data class ParentPage(
        val id: Long,
        val title: String,
        var isSelected: Boolean,
        override val type: Type
    ) : PageItem(type)

    data class Divider(val title: String = "") : PageItem(DIVIDER)

    data class Empty(
        @StringRes val textResource: Int = R.string.empty_list_default,
        val isSearching: Boolean = false,
        val isButtonVisible: Boolean = true,
        val isImageVisible: Boolean = true
    ) : PageItem(EMPTY)

    enum class Type(val viewType: Int) {
        PAGE(1),
        DIVIDER(2),
        EMPTY(3),
        PARENT(4),
        TOP_LEVEL_PARENT(5)
    }

    enum class Action(@IdRes val itemId: Int) {
        VIEW_PAGE(R.id.view_page),
        CANCEL_AUTO_UPLOAD(R.id.cancel_auto_upload),
        SET_PARENT(R.id.set_parent),
        SET_AS_HOMEPAGE(R.id.set_as_homepage),
        SET_AS_POSTS_PAGE(R.id.set_as_posts_page),
        PUBLISH_NOW(R.id.publish_now),
        MOVE_TO_DRAFT(R.id.move_to_draft),
        DELETE_PERMANENTLY(R.id.delete_permanently),
        MOVE_TO_TRASH(R.id.move_to_trash);

        companion object {
            fun fromItemId(itemId: Int): Action {
                return values().firstOrNull { it.itemId == itemId }
                        ?: throw IllegalArgumentException("Unexpected item ID in context menu")
            }
        }
    }
}
