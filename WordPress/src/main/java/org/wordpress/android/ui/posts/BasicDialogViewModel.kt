package org.wordpress.android.ui.posts

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Dismissed
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Negative
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction.Positive
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class BasicDialogViewModel
@Inject constructor() : ViewModel() {
    private val _onInteraction = MutableLiveData<Event<DialogInteraction>>()
    val onInteraction = _onInteraction as LiveData<Event<DialogInteraction>>
    fun showDialog(manager: FragmentManager, model: BasicDialogModel) {
        val dialog = BasicDialog()
        dialog.initialize(model)
        dialog.show(manager, model.tag)
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
    @SuppressLint("ParcelCreator")
    data class BasicDialogModel(
        val tag: String,
        val title: String? = null,
        val message: String,
        val positiveButtonLabel: String,
        val negativeButtonLabel: String? = null,
        val cancelButtonLabel: String? = null
    ) : Parcelable

    sealed class DialogInteraction(open val tag: String) {
        data class Positive(override val tag: String) : DialogInteraction(tag)
        data class Negative(override val tag: String) : DialogInteraction(tag)
        data class Dismissed(override val tag: String) : DialogInteraction(tag)
    }
}
