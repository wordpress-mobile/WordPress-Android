package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.post_signup_interstitial_activity.*
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher

class PostSignupInterstitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.post_signup_interstitial_activity)

        /** TODO: Should we always show this option?
         * On SitePickerActivity.addSite() there's a check to see if the user has signed in with a WP.com account
         * to determine whether or not they are able to create a new site or just to add a self-hosted one.
         * I initially thought we would need to make a similar check here because SitePickerActivity.addSite() was
         * being called from the "Add Site" button that appears on MySiteFragment when the user doesn't have any sites.
         * That being said, the assumption here is that if the user haven't signed in with a WP.com account, then they
         * must have added a self-hosted site, which in turn means they won't need to see this screen.
         * I'm leaving this here until I get some confirmation on that.
         **/
        create_new_site_button.setOnClickListener {
            /** TODO: Should we listen for the result?
             * The assumption here is that an instance of WPMainActivity will be on the back stack, which may be true
             * depending on where this activity is initiated. If this proves to be hard to guarantee, then we can try
             * listening for the result and then pass it forward to WPMainActivity ourselves. If not,
             * WPMainActivity.onResume() will handle the new site for us automatically by calling
             * WPMainActivity.initSelectedSite().
             **/
            ActivityLauncher.newBlogForResult(this)
            finish()
        }

        add_self_hosted_site_button.setOnClickListener {
            /** TODO: Should we listen for the result?
             * See comment above.
             **/
            ActivityLauncher.addSelfHostedSiteForResult(this)
            finish()
        }

        dismiss_button.setOnClickListener {
            ActivityLauncher.viewReader(this)
            finish()
        }
    }
}
