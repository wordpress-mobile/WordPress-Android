package org.wordpress.android.ui.posts

import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.DateUtilsWrapper
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class PostResolutionOverlayViewModel @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper,
    private val dateUtilsWrapper: DateUtilsWrapper
) : ViewModel() {
    private val _uiState = MutableLiveData<PostResolutionOverlayUiState>()
    val uiState: LiveData<PostResolutionOverlayUiState> = _uiState

    private val _triggerListeners = MutableLiveData<PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent>()
    val triggerListeners: MutableLiveData<PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent> =
        _triggerListeners

    private val _dismissDialog = SingleLiveEvent<Boolean>()
    val dismissDialog = _dismissDialog as LiveData<Boolean>

    private var isStarted = false
    private lateinit var resolutionType: PostResolutionType

    fun start(postModel: PostModel?, postResolutionType: PostResolutionType?) {
        if (isStarted) return

        if (postModel == null || postResolutionType == null) {
            _dismissDialog.postValue(true)
            return
        }

        resolutionType = postResolutionType

        val uiState = when (resolutionType) {
            PostResolutionType.SYNC_CONFLICT -> getUiStateForSyncConflict(postModel)
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT -> getUiStateForAutosaveRevisionConflict(postModel)
        }

        _uiState.postValue(uiState)
    }

    private fun getUiStateForSyncConflict(post: PostModel): PostResolutionOverlayUiState {
        return PostResolutionOverlayUiState(
            titleResId = R.string.dialog_post_conflict_title,
            bodyResId = if (post.isPage) R.string.dialog_post_conflict_body_for_page else R.string.dialog_post_conflict_body,
            content = buildContentItemsForVersionSync(post),
            confirmedClick = ::onConfirmClick,
            cancelClick = ::onCancelClick,
            closeClick = ::onCloseClick,
            onSelected = ::onItemSelected
        )
    }

    private fun getUiStateForAutosaveRevisionConflict(post: PostModel): PostResolutionOverlayUiState {
        return PostResolutionOverlayUiState(
            titleResId = R.string.dialog_post_autosave_title,
            bodyResId = if (post.isPage) R.string.dialog_post_autosave_body_for_page else R.string.dialog_post_autosave_body,
            content = buildContentItemsForAutosaveSync(post),
            confirmedClick = ::onConfirmClick,
            cancelClick = ::onCancelClick,
            closeClick = ::onCloseClick,
            onSelected = ::onItemSelected
        )
    }

    private fun buildContentItemsForVersionSync(post: PostModel): List<ContentItem> {
        // todo: annmarie - make this reusable and testable - use "Wrappers" not static methods
        val localLastModifiedString =
            if (TextUtils.isEmpty(post.dateLocallyChanged)) post.lastModified else post.dateLocallyChanged
        val remoteLastModifiedString = post.remoteLastModified
        val localLastModifiedAsLong = dateTimeUtilsWrapper.timestampFromIso8601Millis(localLastModifiedString)
        val remoteLastModifiedAsLong = dateTimeUtilsWrapper.timestampFromIso8601Millis(remoteLastModifiedString)

        val flags = (DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_RELATIVE)

        val localModifiedDateTime = dateUtilsWrapper.formatDateTime(localLastModifiedAsLong, flags )

        val remoteModifiedDateTime = dateUtilsWrapper.formatDateTime(remoteLastModifiedAsLong, flags )

        return listOf(
            ContentItem(headerResId = R.string.dialog_post_conflict_current_device,
                dateLine = UiString.UiStringText(localModifiedDateTime),
                isSelected = false,
                id = ContentItemType.LOCAL_DEVICE),
            ContentItem(headerResId = R.string.dialog_post_conflict_another_device,
                dateLine = UiString.UiStringText(remoteModifiedDateTime),
                isSelected = false,
                id = ContentItemType.OTHER_DEVICE)
        )
    }

    private fun buildContentItemsForAutosaveSync(post: PostModel): List<ContentItem> {
        val localLastModifiedString =
            if (TextUtils.isEmpty(post.dateLocallyChanged)) post.lastModified else post.dateLocallyChanged
        val autoSaveModifiedString = post.autoSaveModified as String // todo: annmarie
        val localLastModifiedAsLong = dateTimeUtilsWrapper.timestampFromIso8601Millis(localLastModifiedString)
        val autoSaveModifiedAsLong = dateTimeUtilsWrapper.timestampFromIso8601Millis(autoSaveModifiedString)

        val flags = (DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_RELATIVE)

        val localModifiedDateTime = dateUtilsWrapper.formatDateTime(localLastModifiedAsLong, flags )

        val remoteModifiedDateTime = dateUtilsWrapper.formatDateTime(autoSaveModifiedAsLong, flags )

        return listOf(
            ContentItem(headerResId = R.string.dialog_post_autosave_current_device,
                dateLine = UiString.UiStringText(localModifiedDateTime),
                isSelected = false,
                id = ContentItemType.LOCAL_DEVICE),
            ContentItem(headerResId = R.string.dialog_post_autosave_another_device,
                dateLine = UiString.UiStringText(remoteModifiedDateTime),
                isSelected = false,
                id = ContentItemType.OTHER_DEVICE)
        )
    }

    private fun onConfirmClick() {
        Log.i(javaClass.simpleName, "***=> onConfirmClick")
        // todo: add logging
        _uiState.value?.selectedContentItem?.let {
            _triggerListeners.value = PostResolutionOverlayActionEvent.PostResolutionConfirmationEvent(resolutionType,
                it.id.toPostResolutionConfirmationType())
        }
        _dismissDialog.value = true
    }

    private fun onCloseClick() {
        Log.i(javaClass.simpleName, "***=> onCloseClick")
        // todo: add logging
        _dismissDialog.value = true
    }

    private fun onCancelClick() {
        Log.i(javaClass.simpleName, "***=> onCancelClick")
        // todo: add logging
        _dismissDialog.value = true
    }

    fun onDialogDismissed() {
        Log.i(javaClass.simpleName, "***=> onDialogDismissed")
        _dismissDialog.value = true
        // todo: add logging
    }

    private fun onItemSelected(selectedItem: ContentItem) {
        Log.i(javaClass.simpleName, "***=> onItemSelected $selectedItem")
        val selectedState = selectedItem.isSelected

        // Update the isSelected property of the selected item within the content list
        val updatedContent = _uiState.value?.content?.map { contentItem ->
            contentItem.copy(isSelected = selectedState &&  contentItem.id == selectedItem.id )
        } ?: return

        val currentUiState = _uiState.value ?: return // Return if UiState is null
        val updatedUiState = currentUiState.copy(
            selectedContentItem = selectedItem,
            content = updatedContent,
            actionEnabled = selectedState
        )
        Log.i(javaClass.simpleName, "***=> updatedState $updatedUiState")
        _uiState.postValue(updatedUiState)
    }
}

// todo: annmarie - you can move all of these into individual classes if you want
data class PostResolutionOverlayUiState(
    @StringRes val titleResId: Int,
    @StringRes val bodyResId: Int,
    val actionEnabled: Boolean = false,
    val content: List<ContentItem>,
    val selectedContentItem: ContentItem? = null,
    val onSelected: (ContentItem) -> Unit,
    val closeClick: () -> Unit,
    val cancelClick: () -> Unit,
    val confirmedClick: () -> Unit
)

data class ContentItem(
    val id: ContentItemType,
    @DrawableRes val iconResId: Int = R.drawable.ic_pages_white_24dp,
    @StringRes val headerResId: Int,
    val dateLine: UiString,
    var isSelected: Boolean,
)

enum class ContentItemType {
    LOCAL_DEVICE,
    OTHER_DEVICE
}

fun ContentItemType.toPostResolutionConfirmationType(): PostResolutionConfirmationType {
    return when (this) {
        ContentItemType.LOCAL_DEVICE -> PostResolutionConfirmationType.CONFIRM_LOCAL
        ContentItemType.OTHER_DEVICE -> PostResolutionConfirmationType.CONFIRM_OTHER
    }
}

enum class PostResolutionType {
    SYNC_CONFLICT,
    AUTOSAVE_REVISION_CONFLICT
}

enum class PostResolutionConfirmationType {
    CONFIRM_LOCAL,
    CONFIRM_OTHER
}

sealed class PostResolutionOverlayActionEvent {
    data class ShowDialogAction(val postModel: PostModel, val postResolutionType: PostResolutionType)
    data class PostResolutionConfirmationEvent(
        val postResolutionType: PostResolutionType,
        val postResolutionConfirmationType: PostResolutionConfirmationType
    )
}
