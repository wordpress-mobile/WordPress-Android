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
import org.wordpress.android.ui.posts.PrepublishingScreen.ADD_CATEGORY
import org.wordpress.android.ui.posts.PrepublishingScreen.CATEGORIES
import org.wordpress.android.ui.posts.PrepublishingScreen.HOME
import org.wordpress.android.ui.posts.PrepublishingScreen.PUBLISH
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.Event
import java.io.Serializable
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

    private val _dismissKeyboard = MutableLiveData<Event<Unit>>()
    val dismissKeyboard: LiveData<Event<Unit>> = _dismissKeyboard

    private val _triggerOnSubmitButtonClickedListener = MutableLiveData<Event<PublishPost>>()
    val triggerOnSubmitButtonClickedListener: LiveData<Event<PublishPost>> = _triggerOnSubmitButtonClickedListener

    private val _triggerOnDeviceBackPressed = MutableLiveData<Event<PrepublishingScreen>>()
    val triggerOnDeviceBackPressed: LiveData<Event<PrepublishingScreen>> = _triggerOnDeviceBackPressed

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
        this.currentScreen = currentScreenFromSavedState ?: HOME

        currentScreen?.let { screen ->
            navigateToScreen(screen)
        }
        fetchTags()
    }

    private fun navigateToScreen(prepublishingScreen: PrepublishingScreen, bundle: Bundle? = null) {
        // Note: given we know both the HOME, TAGS and ADD_CATEGORY screens have an EditText, we can ask to send the
        // dismissKeyboard signal only when we're not either in one of these nor navigating towards one of these.
        // At this point in code we only know where we want to navigate to, but it's ok since landing on any of these
        // two we'll want the keyboard to stay up if it was already up ;) (i.e. don't dismiss it).
        // For the case where this is not a story and hence there's no EditText in the HOME screen, we're ok too,
        // because there wouldn't have been a keyboard up anyway.
        if (prepublishingScreen == PUBLISH ||
                prepublishingScreen == CATEGORIES) {
            _dismissKeyboard.postValue(Event(Unit))
        }
        updateNavigationTarget(PrepublishingNavigationTarget(site, prepublishingScreen, bundle))
    }

    // Send this back out to the current screen and so it can determine if it needs to save
    // any data before accepting a backPress - in our case, the only view that needs this today
    // is the Categories selection & AddCategory
    fun onDeviceBackPressed() {
        if (currentScreen == CATEGORIES || currentScreen == ADD_CATEGORY) {
            _triggerOnDeviceBackPressed.value = Event(currentScreen as PrepublishingScreen)
        } else {
            onBackClicked()
        }
    }

    fun onBackClicked(bundle: Bundle? = null) {
        when {
            currentScreen == ADD_CATEGORY -> {
                currentScreen = CATEGORIES
                navigateToScreen(currentScreen as PrepublishingScreen, bundle)
            }
            currentScreen != HOME -> {
                currentScreen = HOME
                navigateToScreen(currentScreen as PrepublishingScreen)
            }
            else -> {
                _dismissBottomSheet.postValue(Event(Unit))
            }
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

    fun onActionClicked(actionType: ActionType, bundle: Bundle? = null) {
        val screen = PrepublishingScreen.valueOf(actionType.name)
        currentScreen = screen
        navigateToScreen(screen, bundle)
    }

    fun onSubmitButtonClicked(publishPost: PublishPost) {
        onCloseClicked()
        _triggerOnSubmitButtonClickedListener.postValue(Event(publishPost))
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
            AppLog.e(
                    T.POSTS,
                    "An error occurred while updating taxonomy with type: " + event.error.type
            )
        }
    }
}

@Parcelize
enum class PrepublishingScreen : Parcelable {
    HOME,
    PUBLISH,
    TAGS,
    CATEGORIES,
    ADD_CATEGORY
}

data class PrepublishingNavigationTarget(
    val site: SiteModel,
    val targetScreen: PrepublishingScreen,
    val bundle: Bundle? = null
)

data class PrepublishingAddCategoryRequest(
    val categoryText: String,
    val categoryParentId: Long
) : Serializable
