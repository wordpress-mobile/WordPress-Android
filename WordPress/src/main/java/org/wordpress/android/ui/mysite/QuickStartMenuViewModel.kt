package org.wordpress.android.ui.mysite

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Hide
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Pin
import org.wordpress.android.ui.mysite.QuickStartMenuViewModel.QuickStartMenuInteraction.Remove
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class QuickStartMenuViewModel
@Inject constructor() : ViewModel() {
    private val _onInteraction = MutableLiveData<Event<QuickStartMenuInteraction>>()
    val onInteraction = _onInteraction as LiveData<Event<QuickStartMenuInteraction>>

    var id: String? = null

    fun onPinActionClicked() {
        id?.let { _onInteraction.postValue(Event(Pin(it))) }
    }

    fun onHideActionClicked() {
        id?.let { _onInteraction.postValue(Event(Hide(it))) }
    }

    fun onRemoveActionClicked() {
        id?.let { _onInteraction.postValue(Event(Remove(it))) }
    }

    sealed class QuickStartMenuInteraction(open val id: String) {
        data class Pin(override val id: String) : QuickStartMenuInteraction(id)
        data class Hide(override val id: String) : QuickStartMenuInteraction(id)
        data class Remove(override val id: String) : QuickStartMenuInteraction(id)
    }
}
