package org.wordpress.android.ui.reader

import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.ReaderInWordPressAppFeatureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether the Reader (and with it the bottom nav) is available. The Jetpack app always has it;
 * the WordPress app has it only for WordPress.com users when [ReaderInWordPressAppFeatureConfig] is on.
 *
 * The flag is read once per process so a value fetched mid-session never changes the nav under the
 * user, and re-read after a WordPress.com sign-in so that sign-in can pick up a newly fetched value.
 */
@Singleton
class ReaderAvailability @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val accountStore: AccountStore,
    private val readerInWordPressAppFeatureConfig: ReaderInWordPressAppFeatureConfig,
) {
    @Volatile
    private var isFlagEnabled: Boolean? = null

    fun isReaderAvailable(): Boolean =
        buildConfigWrapper.isJetpackApp || (isFlagEnabled() && accountStore.hasAccessToken())

    fun refreshFlagAfterSignIn() {
        isFlagEnabled = readerInWordPressAppFeatureConfig.isEnabled()
    }

    private fun isFlagEnabled(): Boolean =
        isFlagEnabled ?: readerInWordPressAppFeatureConfig.isEnabled().also { isFlagEnabled = it }
}
