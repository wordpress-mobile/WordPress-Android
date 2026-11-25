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
 *
 * Whenever the answer differs from the previous one, [availabilityChanged] fires so the nav and My Site
 * can catch up, whether the cause is a sign-in, a sign-out or a stored flag that loaded after the first check.
 */
@Singleton
class ReaderAvailability @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountStore: AccountStore,
    private val readerInWordPressAppFeatureConfig: ReaderInWordPressAppFeatureConfig,
) {
    @Volatile
    private var cachedFlag: Boolean? = null

    @Volatile
    private var lastAnswer: Boolean? = null

    private val _availabilityChanged = MutableLiveData<Event<Unit>>()

    /** Fires when the answer from [isReaderAvailable] changes. */
    val availabilityChanged: LiveData<Event<Unit>> = _availabilityChanged

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

    /** Call once the feature flags fetched after a WordPress.com sign-in have been stored. */
    fun refreshFlagAfterSignIn() {
        if (buildConfigWrapper.isJetpackApp || !accountStore.hasAccessToken()) return
        cachedFlag = null
        isReaderAvailable()
    }

    private fun readFlag(): Boolean {
        val state = readerInWordPressAppFeatureConfig.featureState()
        if (state is FeatureState.RemoteValue || state is FeatureState.ManuallyOverriden) {
            cachedFlag = state.isEnabled
        }
        return state.isEnabled
    }
}
