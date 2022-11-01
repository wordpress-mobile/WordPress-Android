package org.wordpress.android.ui.accounts.login.jetpack

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.accounts.LoginNavigationEvents
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowInstructions
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSignInForResultJetpackOnly
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.NoUser
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesViewModel.State.ShowUser
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Named

const val KEY_STATE = "key_state"

@HiltViewModel
class LoginNoSitesViewModel @Inject constructor(
    private val unifiedLoginTracker: UnifiedLoginTracker,
    private val accountStore: AccountStore,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    private val _uiModel = MediatorLiveData<UiModel>()
    val uiModel: LiveData<UiModel> = _uiModel

    private var isStarted = false

    fun start(application: WordPress, savedInstanceState: Bundle?) {
        if (isStarted) return
        isStarted = true

        init(savedInstanceState)

        signOutWordPress(application)
    }

    private fun init(savedInstanceState: Bundle?) {
        val state = if (savedInstanceState != null) {
            buildStateFromSavedInstanceState(savedInstanceState)
        } else {
            buildStateFromAccountStore()
        }
        _uiModel.postValue(UiModel(state = state))
    }

    private fun buildStateFromSavedInstanceState(savedInstanceState: Bundle) =
            savedInstanceState.getSerializable(KEY_STATE) as State

    private fun buildStateFromAccountStore() =
            accountStore.account?.let {
                ShowUser(
                        it.avatarUrl.orEmpty(),
                        it.userName,
                        it.displayName
                )
            } ?: NoUser

    private fun signOutWordPress(application: WordPress) {
        launch {
            withContext(bgDispatcher) {
                application.wordPressComSignOut()
            }
        }
    }

    fun onSeeInstructionsPressed() {
        _navigationEvents.postValue(Event(ShowInstructions()))
    }

    fun onTryAnotherAccountPressed() {
        _navigationEvents.postValue(Event(ShowSignInForResultJetpackOnly))
    }

    fun onFragmentResume() {
        unifiedLoginTracker.track(step = Step.NO_JETPACK_SITES)
    }

    fun onBackPressed() {
        _navigationEvents.postValue(Event(ShowSignInForResultJetpackOnly))
    }

    fun writeToBundle(outState: Bundle) {
        requireNotNull(outState.putSerializable(KEY_STATE, uiModel.value?.state))
    }

    data class UiModel(
        val state: State
    )

    @Suppress("SerialVersionUIDInSerializableClass")
    sealed class State : Serializable {
        object NoUser : State()
        data class ShowUser(
            val accountAvatarUrl: String,
            val userName: String,
            val displayName: String
        ) : State()
    }
}
