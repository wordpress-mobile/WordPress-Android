package org.wordpress.android.ui.reader

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
 * Once a definitive value has been read while signed in to WordPress.com, it's kept, so a value fetched
 * mid-session never changes the nav under the user. Definitive means fetched from the server, set manually or
 * by build config, or a stored default that this launch's fetch has already had the chance to replace. Until
 * then the stored flags may still be loading at cold start, or may hold only a default, so every flag update
 * is re-checked.
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

    /** Set once a deep link has waited for this launch's flag fetch, so later ones don't wait again. */
    @Volatile
    private var hasWaitedForFetch = false

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
     * Runs [action] right away if availability is known, otherwise on the main thread once it is and [owner]
     * is started. For one-off decisions such as where a Reader deep link goes, which mustn't be made on a
     * default: at cold start the stored flags may still be loading, and if the server only just added the
     * flag there's no fetched value until this launch's refresh lands, so that's given [FETCH_WAIT_MS].
     */
    fun whenAvailabilityKnown(owner: LifecycleOwner, action: Runnable) {
        if (isAvailabilityKnown()) {
            action.run()
            return
        }
        owner.lifecycleScope.launch {
            appConfig.awaitStoredFeatureFlags()
            if (!hasWaitedForFetch) {
                withTimeoutOrNull(FETCH_WAIT_MS) {
                    appConfig.featureFlagUpdates.first { isAvailabilityKnown() }
                }
                hasWaitedForFetch = true
            }
            owner.withStarted { action.run() }
        }
    }

    private fun isAvailabilityKnown(): Boolean = when {
        buildConfigWrapper.isJetpackApp || !accountStore.hasAccessToken() -> true
        !appConfig.hasLoadedStoredFeatureFlags() -> false
        else -> {
            isReaderAvailable() // keeps the value if it was fetched or set manually
            cachedFlag != null
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
        val isDefinitive = when (state) {
            // The key isn't in the stored flags: they haven't loaded yet, or it's the first launch since it was added
            is FeatureState.DefaultValue -> false
            // A stored default only counts once this launch's fetch has completed without replacing it, e.g.
            // because the server doesn't have the key yet
            is FeatureState.RemoteValue ->
                readerInWordPressAppFeatureConfig.hasFetchedValue() || appConfig.hasRefreshedFeatureFlags()
            // Manual overrides and build config values (the release switch) are final
            else -> true
        }
        if (isDefinitive) {
            cachedFlag = state.isEnabled
        }
        return state.isEnabled
    }

    companion object {
        private const val FETCH_WAIT_MS = 2000L
    }
}
