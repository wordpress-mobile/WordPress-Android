package org.wordpress.android.ui.reader

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the Reader is available, and with it the bottom nav (and so the Me tab rather than the
 * My Site Me row). The Jetpack app always has it; the WordPress app has it only for WordPress.com users
 * when [ReaderInWordPressAppFeatureConfig] is on.
 *
 * The flag is read on the first check made while signed in to WordPress.com and then kept, so a value
 * fetched mid-session never changes the nav under the user. Signing out forgets it, so the next sign-in
 * reads it afresh.
 */
@Singleton
class ReaderAvailability @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountStore: AccountStore,
    private val readerInWordPressAppFeatureConfig: ReaderInWordPressAppFeatureConfig,
) {
    @Volatile
    private var cachedFlag: Boolean? = null

    fun isReaderAvailable(): Boolean {
        if (buildConfigWrapper.isJetpackApp) return true
        if (!accountStore.hasAccessToken()) {
            cachedFlag = null
            return false
        }
        return cachedFlag ?: readerInWordPressAppFeatureConfig.isEnabled().also { cachedFlag = it }
    }
}
