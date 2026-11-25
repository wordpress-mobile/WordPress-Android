package org.wordpress.android.ui.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.AppConfig.FeatureState
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the Reader is available, and with it the bottom nav (and so the Me tab rather than the
 * My Site Me row). The Jetpack app always has it; the WordPress app has it only for WordPress.com users
 * when [ReaderInWordPressAppFeatureConfig] is on.
 *
 * The flag is read on the first check made while signed in to WordPress.com and then kept, so a value
 * fetched mid-session never changes the nav under the user. Only a stored or manually set value is kept:
 * at cold start the stored flags load asynchronously, and the default returned before then mustn't stick.
 *
 * Signing out forgets the value. A sign-in reads it again once that account's flags have been fetched
 * ([refreshFlagAfterSignIn]), as iOS does: until then only the pre-sign-in value, or the default on a
 * fresh install, is stored.
 */
@Singleton
class ReaderAvailability @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountStore: AccountStore,
    private val readerInWordPressAppFeatureConfig: ReaderInWordPressAppFeatureConfig,
) {
    @Volatile
    private var cachedFlag: Boolean? = null

    private val _availabilityChanged = MutableLiveData<Event<Unit>>()

    /** Fires when [refreshFlagAfterSignIn] changes availability while the app UI is already showing. */
    val availabilityChanged: LiveData<Event<Unit>> = _availabilityChanged

    fun isReaderAvailable(): Boolean = when {
        buildConfigWrapper.isJetpackApp -> true
        !accountStore.hasAccessToken() -> {
            cachedFlag = null
            false
        }
        else -> cachedFlag ?: readFlag()
    }

    /** Call once the feature flags fetched after a WordPress.com sign-in have been stored. */
    fun refreshFlagAfterSignIn() {
        if (buildConfigWrapper.isJetpackApp || !accountStore.hasAccessToken()) return
        val previousFlag = cachedFlag
        cachedFlag = null
        if (readFlag() != previousFlag) {
            _availabilityChanged.postValue(Event(Unit))
        }
    }

    private fun readFlag(): Boolean {
        val state = readerInWordPressAppFeatureConfig.featureState()
        if (state is FeatureState.RemoteValue || state is FeatureState.ManuallyOverriden) {
            cachedFlag = state.isEnabled
        }
        return state.isEnabled
    }
}
