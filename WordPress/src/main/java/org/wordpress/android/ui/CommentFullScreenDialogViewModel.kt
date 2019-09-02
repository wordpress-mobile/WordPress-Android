package org.wordpress.android.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class CommentFullScreenDialogViewModel
@Inject constructor() : ViewModel() {
    private val _onKeyboardOpened = MutableLiveData<Event<Unit>>()
    val onKeyboardOpened: LiveData<Event<Unit>> = _onKeyboardOpened

    fun init() {
        _onKeyboardOpened.postValue(Event(Unit))
    }
}
