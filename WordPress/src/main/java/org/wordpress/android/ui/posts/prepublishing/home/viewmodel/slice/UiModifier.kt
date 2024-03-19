package org.wordpress.android.ui.posts.prepublishing.home.viewmodel.slice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope

/**
 * An abstract class designed to modify UI state and post events within a ViewModel context.
 * This class should be extended by specific modifiers that implement UI state changes
 * and event handling tailored to particular needs.
 *
 * @param T The type of data used to modify the UI state. This could be any type that represents
 * the data model your UI will consume.
 * @param E The type of events that this modifier can emit. This is useful for communicating
 * actions or changes from the UI modifier back to the ViewModel or UI layer.
 *
 */
abstract class UiModifier<T : Any, E: Any> {
    /**
     * LiveData holding the current state of the UI. This is protected so only
     * subclasses can modify it directly.
     */
    protected var uiState: MutableLiveData<T>? = null

    /**
     * A CoroutineScope that the UiModifier can use to launch coroutines.
     * This should be provided during initialization and is typically tied to a ViewModel's lifecycle.
     */
    private var scope: CoroutineScope? = null

    /**
     * Internal MutableLiveData used to post events. Subclasses can post events
     * to this LiveData, which can be observed by the ViewModel or UI layers.
     */
    protected val _event = MutableLiveData<E>()

    /**
     * Publicly accessible LiveData for observing events emitted by this UiModifier.
     */
    val event: LiveData<E> = _event

    /**
     * Initializes the UiModifier with the necessary MutableLiveData for UI state
     * and a CoroutineScope. This method should be called by the ViewModel
     * that owns this UiModifier.
     *
     * @param uiState The MutableLiveData that represents the UI state.
     * @param scope The CoroutineScope in which coroutines will be launched.
     */
    fun initialize(uiState: MutableLiveData<T>, scope: CoroutineScope) {
        this.uiState = uiState
        this.scope = scope
    }

    /**
     * Method to be called to clear resources when the UiModifier is no longer needed.
     * This is typically called from a ViewModel's onCleared() method.
     */
    open fun onCleared() {}

    /**
     * Updates the UI state LiveData with a new state. This method should be called
     * on the main thread.
     *
     * @param newState The new state to be applied to the UI.
     */
    protected fun updateUiState(newState: T) {
        // Directly update the UI state, assuming this is always called on the main thread
        uiState?.value = newState
    }

    /**
     * Posts a new event to the event LiveData. This can be observed by the ViewModel
     * or UI to react to changes or actions.
     *
     * @param event The event to be emitted.
     */
    protected fun postEvent(event: E) {
        _event.postValue(event)
    }
}
