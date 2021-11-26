package org.wordpress.android.ui.about

import android.os.Bundle
import com.automattic.about.model.AboutConfig
import com.automattic.about.model.AboutConfigProvider
import com.automattic.about.model.HeaderConfig
import com.automattic.about.model.LegalConfig
import com.automattic.about.model.RateUsConfig
import com.automattic.about.model.ShareConfig
import com.automattic.about.model.SocialsConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.LocaleAwareActivity
import javax.inject.Inject

class UnifiedAboutActivity : LocaleAwareActivity(), AboutConfigProvider {
    @Inject lateinit var viewModel: UnifiedAboutViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(R.layout.unified_about_activity)
    }

    override fun getAboutConfig(): AboutConfig {
        return AboutConfig(
                headerConfig = HeaderConfig.fromContext(this),
                shareConfigFactory = ::createShareConfig,
                rateUsConfig = RateUsConfig.fromContext(this),
                socialsConfig = SocialsConfig(
                        instagramUsername = WP_SOCIAL_HANDLE,
                        twitterUsername = WP_SOCIAL_HANDLE
                ),
                legalConfig = LegalConfig(
                        tosUrl = "https://wordpress.com/tos/",
                        privacyPolicyUrl = "https://automattic.com/privacy/",
                        acknowledgementsUrl = "file:///android_asset/licenses.html"
                ),
                onDismiss = { onBackPressed() }
        )
    }

    private fun createShareConfig() = ShareConfig(
            subject = "WordPress",
            message = "Hey! Here is a link to download the WordPress app. " +
                    "I'm really enjoying it and thought you might too!\n" +
                    "https://apps.wordpress.com/get?campaign=app_share_link"
    )

    companion object {
        private const val WP_SOCIAL_HANDLE = "wordpressdotcom" // CHECKSTYLE IGNORE
    }
}
