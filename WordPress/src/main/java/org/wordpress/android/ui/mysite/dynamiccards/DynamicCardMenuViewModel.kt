package org.wordpress.android.ui.mysite.dynamiccards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.model.DynamicCardType
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Hide
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Pin
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Remove
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel.DynamicCardMenuInteraction.Unpin
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class DynamicCardMenuViewModel
@Inject constructor() : ViewModel() {
    private val _onInteraction = MutableLiveData<Event<DynamicCardMenuInteraction>>()
    val onInteraction = _onInteraction as LiveData<Event<DynamicCardMenuInteraction>>

    private lateinit var cardType: DynamicCardType
    var isPinned: Boolean = false

    fun onPinActionClicked() {
        if (isPinned) {
            _onInteraction.postValue(Event(Unpin(cardType)))
        } else {
            _onInteraction.postValue(Event(Pin(cardType)))
        }
    }

    fun onHideActionClicked() {
        _onInteraction.postValue(Event(Hide(cardType)))
    }

    fun onRemoveActionClicked() {
        _onInteraction.postValue(Event(Remove(cardType)))
    }

    fun start(cardType: String, isPinned: Boolean) {
        this.cardType = DynamicCardType.valueOf(cardType)
        this.isPinned = isPinned
    }

    sealed class DynamicCardMenuInteraction(open val cardType: DynamicCardType) {
        data class Pin(override val cardType: DynamicCardType) : DynamicCardMenuInteraction(cardType)
        data class Unpin(override val cardType: DynamicCardType) : DynamicCardMenuInteraction(cardType)
        data class Hide(override val cardType: DynamicCardType) : DynamicCardMenuInteraction(cardType)
        data class Remove(override val cardType: DynamicCardType) : DynamicCardMenuInteraction(cardType)
    }
}
