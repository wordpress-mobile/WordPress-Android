package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.post_signup_interstitial_default.*
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher

class PostSignupInterstitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_signup_interstitial_activity)

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
