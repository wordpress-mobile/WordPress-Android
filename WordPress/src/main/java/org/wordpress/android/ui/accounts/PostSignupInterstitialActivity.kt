package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.post_signup_interstitial_default.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

class PostSignupInterstitialActivity : AppCompatActivity() {
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.post_signup_interstitial_activity)

        appPrefsWrapper.shouldShowPostSignupInterstitial = false

        create_new_site_button.setOnClickListener {
            ActivityLauncher.newBlogForResult(this)
            finish()
        }

        add_self_hosted_site_button.setOnClickListener {
            ActivityLauncher.addSelfHostedSiteForResult(this)
            finish()
        }

        dismiss_button.setOnClickListener {
            ActivityLauncher.viewReader(this)
            finish()
        }
    }
}
