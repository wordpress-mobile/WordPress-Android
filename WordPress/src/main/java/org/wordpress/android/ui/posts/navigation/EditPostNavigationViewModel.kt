package org.wordpress.android.ui.posts.navigation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

/**
 * ViewModel responsible for managing navigation state in the Edit Post flow.
 * Provides centralized navigation state management and type-safe navigation events.
 */
class EditPostNavigationViewModel @Inject constructor() : ViewModel() {
    private val _currentDestination = MutableLiveData(EditPostDestination.default())
    val currentDestination: LiveData<EditPostDestination> = _currentDestination

    private val _navigationEvents = MutableLiveData<Event<EditPostDestination>>()
    val navigationEvents: LiveData<Event<EditPostDestination>> = _navigationEvents

    fun navigateTo(destination: EditPostDestination) {
        AppLog.d(
            AppLog.T.POSTS,
            "EditPostNavigationViewModel: navigating from ${_currentDestination.value} to $destination"
        )
        _currentDestination.value = destination
        _navigationEvents.value = Event(destination)
    }

    fun canNavigateBack(): Boolean {
        val canNavigate = (_currentDestination.value ?: EditPostDestination.default()) != EditPostDestination.Editor
        AppLog.d(AppLog.T.POSTS, "EditPostNavigationViewModel: canNavigateBack() = $canNavigate")
        return canNavigate
    }

    fun handleBackNavigation(): Boolean {
        // Use Elvis operator since _currentDestination is initialized and should never be null,
        // but LiveData.value is nullable by design. Use default destination as fallback.
        val currentDest = _currentDestination.value ?: EditPostDestination.default()
        AppLog.d(AppLog.T.POSTS, "EditPostNavigationViewModel: handleBackNavigation() from $currentDest")

        return when (currentDest) {
            EditPostDestination.PublishSettings -> {
                navigateTo(EditPostDestination.Settings)
                true
            }

            EditPostDestination.Settings, EditPostDestination.History -> {
                navigateTo(EditPostDestination.Editor)
                true
            }

            EditPostDestination.Editor -> {
                // Let activity handle back press (exit)
                false
            }
        }
    }
}
