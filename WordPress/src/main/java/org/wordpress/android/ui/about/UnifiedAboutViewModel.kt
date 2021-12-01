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
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.models.recommend.RecommendApiCallsProvider
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName.Jetpack
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendAppName.WordPress
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Failure
import org.wordpress.android.models.recommend.RecommendApiCallsProvider.RecommendCallResult.Success
import org.wordpress.android.ui.about.UnifiedAboutNavigationAction.Dismiss
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.WpUrlUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils.RecommendAppSource.ABOUT
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class UnifiedAboutViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val wpUrlUtils: WpUrlUtilsWrapper,
    private val recommendApiCallsProvider: RecommendApiCallsProvider,
    private val buildConfig: BuildConfigWrapper
) : ViewModel() {
    private val _onNavigation = MutableLiveData<Event<UnifiedAboutNavigationAction>>()
    val onNavigation: LiveData<Event<UnifiedAboutNavigationAction>> = _onNavigation

    fun getAboutConfig() = AboutConfig(
            headerConfig = HeaderConfig.fromContext(contextProvider.getContext()),
            shareConfigFactory = ::createShareConfig,
            rateUsConfig = RateUsConfig.fromContext(contextProvider.getContext()),
            socialsConfig = SocialsConfig(
                    twitterUsername = WP_SOCIAL_HANDLE
            ),
            legalConfig = LegalConfig(
                    tosUrl = wpUrlUtils.buildTermsOfServiceUrl(contextProvider.getContext()),
                    privacyPolicyUrl = Constants.URL_PRIVACY_POLICY,
                    acknowledgementsUrl = WP_ACKNOWLEDGEMENTS_URL
            ),
            onDismiss = ::onDismiss
    )

    private suspend fun createShareConfig(): ShareConfig {
        val app = if (buildConfig.isJetpackApp) Jetpack else WordPress
        val result = recommendApiCallsProvider.getRecommendTemplate(app.appName, ABOUT)
        return ShareConfig(
                subject = contextProvider.getContext().getString(R.string.recommend_app_subject),
                message = when (result) {
                    is Failure -> {
                        AppLog.e(T.MAIN, "Couldn't fetch recommend app template: ${result.error}")
                        WP_APPS_URL // Returning generic message containing only the apps page URL
                    }
                    is Success -> "${result.templateData.message}\n${result.templateData.link}"
                }
        )
    }

    private fun onDismiss() {
        _onNavigation.postValue(Event(Dismiss))
    }

    companion object {
        private const val WP_SOCIAL_HANDLE = "wordpress"
        private const val WP_ACKNOWLEDGEMENTS_URL = "file:///android_asset/licenses.html"
        private const val WP_APPS_URL = "https://apps.wordpress.com/"
    }
}
