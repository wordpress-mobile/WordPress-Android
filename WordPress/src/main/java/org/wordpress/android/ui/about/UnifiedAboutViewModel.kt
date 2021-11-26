package org.wordpress.android.ui.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.automattic.about.model.AboutConfig
import com.automattic.about.model.HeaderConfig
import com.automattic.about.model.LegalConfig
import com.automattic.about.model.RateUsConfig
import com.automattic.about.model.ShareConfig
import com.automattic.about.model.SocialsConfig
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.Dismiss
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class UnifiedAboutViewModel @Inject constructor(
    private val contextProvider: ContextProvider
) : ViewModel() {
    private val _onNavigation = MutableLiveData<Event<UnifiedAboutNavigationAction>>()
    val onNavigation: LiveData<Event<UnifiedAboutNavigationAction>> = _onNavigation

    fun getAboutConfig() = AboutConfig(
            headerConfig = HeaderConfig.fromContext(contextProvider.getContext()),
            shareConfigFactory = ::createShareConfig,
            rateUsConfig = RateUsConfig.fromContext(contextProvider.getContext()),
            socialsConfig = SocialsConfig(
                    instagramUsername = WP_SOCIAL_HANDLE,
                    twitterUsername = WP_SOCIAL_HANDLE
            ),
            legalConfig = LegalConfig(
                    tosUrl = "https://wordpress.com/tos/",
                    privacyPolicyUrl = "https://automattic.com/privacy/",
                    acknowledgementsUrl = "file:///android_asset/licenses.html"
            ),
            onDismiss = ::onDismiss
    )

    private fun createShareConfig() = ShareConfig(
            subject = "WordPress",
            message = "Hey! Here is a link to download the WordPress app. " +
                    "I'm really enjoying it and thought you might too!\n" +
                    "https://apps.wordpress.com/get?campaign=app_share_link"
    )

    private fun onDismiss() {
        _onNavigation.postValue(Event(Dismiss))
    }

    companion object {
        private const val WP_SOCIAL_HANDLE = "wordpressdotcom" // CHECKSTYLE IGNORE
    }
}
