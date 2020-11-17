package org.wordpress.android.ui.posts

import android.os.Parcelable
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.viewmodel.Event

class BasicDialogViewModel : ViewModel() {
    private val _showDialog = MutableLiveData<Event<((FragmentManager) -> Unit)>>()
    private val _onInteraction = MutableLiveData<Event<DialogInteraction>>()
    val showDialog = _showDialog as LiveData<Event<((FragmentManager) -> Unit)>>
    val onInteraction = _onInteraction as LiveData<Event<DialogInteraction>>
    fun showDialog(model: BasicDialogModel) {
        _showDialog.value = Event { manager ->
            val dialog = BasicDialog()
            dialog.initialize(model)
            dialog.show(manager, model.tag)
        }
    }

    fun onPositiveClicked(tag: String) {
        _onInteraction.postValue(Event(Positive(tag)))
    }

    fun onNegativeButtonClicked(tag: String) {
        _onInteraction.postValue(Event(Negative(tag)))
    }

    fun onDismissByOutsideTouch(tag: String) {
        _onInteraction.postValue(Event(Dismissed(tag)))
    }

    @Parcelize
    data class BasicDialogModel(
        val tag: String,
        val title: String?,
        val message: String,
        val positiveButtonLabel: String,
        val negativeButtonLabel: String? = null,
        val cancelButtonLabel: String? = null
    ) : Parcelable

    sealed class DialogInteraction(open val tag: String) {
        data class Positive(override val tag: String): DialogInteraction(tag)
        data class Negative(override val tag: String): DialogInteraction(tag)
        data class Dismissed(override val tag: String): DialogInteraction(tag)
    }
}
