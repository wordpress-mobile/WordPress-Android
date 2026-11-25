package org.wordpress.android.ui.reader

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.AppConfig
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Decides whether the Reader is available, and with it the bottom nav (and so the Me tab rather than the
 * My Site Me row). The Jetpack app always has it; the WordPress app has it only for WordPress.com users
 * when [ReaderInWordPressAppFeatureConfig] is on.
 *
 * Once a value fetched from the server (or set manually) has been read while signed in to WordPress.com,
 * it's kept, so a value fetched mid-session never changes the nav under the user. Until then the stored
 * flags may still be loading at cold start, or may hold only a default, so every flag update is re-checked.
 *
 * Signing out forgets the value. A sign-in reads it again once that account's flags have been fetched
 * ([refreshFlagAfterSignIn]), as iOS does.
 *
 * Whenever the answer differs from the previous one, [availabilityChanged] fires so the nav and My Site
 * can catch up.
 */
@Singleton
class ReaderAvailability @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountStore: AccountStore,
    private val readerInWordPressAppFeatureConfig: ReaderInWordPressAppFeatureConfig,
    private val appConfig: AppConfig,
    @Named(APPLICATION_SCOPE) appScope: CoroutineScope,
) {
    @Volatile
    private var cachedFlag: Boolean? = null

    @Volatile
    private var lastAnswer: Boolean? = null

    private val _availabilityChanged = MutableLiveData<Event<Unit>>()

    /** Fires when the answer from [isReaderAvailable] changes. */
    val availabilityChanged: LiveData<Event<Unit>> = _availabilityChanged

    init {
        if (!buildConfigWrapper.isJetpackApp) {
            appScope.launch {
                appConfig.featureFlagUpdates.collect {
                    if (cachedFlag == null) isReaderAvailable()
                }
            }
        }
    }

    fun isReaderAvailable(): Boolean {
        val answer = when {
            buildConfigWrapper.isJetpackApp -> true
            !accountStore.hasAccessToken() -> {
                cachedFlag = null
                false
            }
            else -> cachedFlag ?: readFlag()
        }
        val previousAnswer = lastAnswer
        lastAnswer = answer
        if (previousAnswer != null && previousAnswer != answer) {
            _availabilityChanged.postValue(Event(Unit))
        }
        return answer
    }

    /**
     * Runs [action] on the main thread once the stored flags have loaded, or right away if they have. For
     * one-off decisions such as where a deep link goes, which mustn't be made on a default at cold start.
     */
    fun whenStoredFlagsLoaded(owner: LifecycleOwner, action: Runnable) {
        if (buildConfigWrapper.isJetpackApp || appConfig.hasLoadedStoredFeatureFlags()) {
            action.run()
        } else {
            owner.lifecycleScope.launch {
                appConfig.awaitStoredFeatureFlags()
                action.run()
            }
        }
    }

    /** Call once the feature flags fetched after a WordPress.com sign-in have been stored. */
    fun refreshFlagAfterSignIn() {
        if (buildConfigWrapper.isJetpackApp || !accountStore.hasAccessToken()) return
        cachedFlag = null
        isReaderAvailable()
    }

    private fun readFlag(): Boolean {
        val state = readerInWordPressAppFeatureConfig.featureState()
        val isDefinitive = state is FeatureState.ManuallyOverriden ||
            (state is FeatureState.RemoteValue && readerInWordPressAppFeatureConfig.hasFetchedValue())
        if (isDefinitive) {
            cachedFlag = state.isEnabled
        }
        return state.isEnabled
    }
}
