package org.wordpress.android.ui.posts

import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.android.parcel.Parcelize
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

const val KEY_SCREEN_STATE = "key_screen_state"

class PrepublishingViewModel @Inject constructor(private val dispatcher: Dispatcher) : ViewModel() {
    private var isStarted = false
    private lateinit var site: SiteModel

    private val _navigationTarget = MutableLiveData<Event<PrepublishingNavigationTarget>>()
    val navigationTarget: LiveData<Event<PrepublishingNavigationTarget>> = _navigationTarget

    private var currentScreen: PrepublishingScreen? = null

    private val _dismissBottomSheet = MutableLiveData<Event<Unit>>()
    val dismissBottomSheet: LiveData<Event<Unit>> = _dismissBottomSheet

    private val _triggerOnPublishButtonClickedListener = MutableLiveData<Event<Unit>>()
    val triggerOnPublishButtonClickedListener: LiveData<Event<Unit>> = _triggerOnPublishButtonClickedListener

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun start(
        site: SiteModel,
        currentScreenFromSavedState: PrepublishingScreen?
    ) {
        if (isStarted) return
        isStarted = true

        this.site = site
        this.currentScreen = currentScreenFromSavedState

        currentScreen?.let { screen ->
            navigateToScreen(screen)
        } ?: run {
            navigateToScreen(HOME)
        }
        fetchTags()
    }

    private fun navigateToScreen(prepublishingScreen: PrepublishingScreen) {
        updateNavigationTarget(PrepublishingNavigationTarget(site, prepublishingScreen))
    }

    fun onBackClicked() {
        if (currentScreen != HOME) {
            currentScreen = HOME
            navigateToScreen(currentScreen as PrepublishingScreen)
        } else {
            _dismissBottomSheet.postValue(Event(Unit))
        }
    }

    fun onCloseClicked() {
        _dismissBottomSheet.postValue(Event(Unit))
    }

    private fun updateNavigationTarget(target: PrepublishingNavigationTarget) {
        _navigationTarget.postValue(Event(target))
    }

    fun writeToBundle(outState: Bundle) {
        outState.putParcelable(KEY_SCREEN_STATE, currentScreen)
    }

    fun onActionClicked(actionType: ActionType) {
        val screen = PrepublishingScreen.valueOf(actionType.name)
        currentScreen = screen
        navigateToScreen(screen)
    }

    fun onPublishButtonClicked() {
        onCloseClicked()
        _triggerOnPublishButtonClickedListener.postValue(Event(Unit))
    }

    /**
     * Fetches the tags so that they will be available when the Tags action is clicked
     */
    private fun fetchTags() {
        dispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(site))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) {
            AppLog.e(T.POSTS, "An error occurred while updating taxonomy with type: " + event.error.type)
        }
    }
}

@Parcelize
enum class PrepublishingScreen : Parcelable {
    HOME,
    PUBLISH,
    VISIBILITY,
    TAGS
}

data class PrepublishingNavigationTarget(
    val site: SiteModel,
    val targetScreen: PrepublishingScreen
)
