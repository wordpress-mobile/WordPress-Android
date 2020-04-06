package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import kotlinx.android.synthetic.main.post_signup_interstitial_default.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW
import javax.inject.Inject

class PostSignupInterstitialActivity : LocaleAwareActivity() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PostSignupInterstitialViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.post_signup_interstitial_activity)

        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(PostSignupInterstitialViewModel::class.java)

        viewModel.onInterstitialShown()

        create_new_site_button.setOnClickListener { viewModel.onCreateNewSiteButtonPressed() }
        add_self_hosted_site_button.setOnClickListener { viewModel.onAddSelfHostedSiteButtonPressed() }
        dismiss_button.setOnClickListener { viewModel.onDismissButtonPressed() }

        viewModel.navigationAction.observe(this, Observer { executeAction(it) })
    }

    override fun onBackPressed() {
        viewModel.onBackButtonPressed()
    }

    private fun executeAction(navigationAction: NavigationAction) = when (navigationAction) {
        START_SITE_CREATION_FLOW -> startSiteCreationFlow()
        START_SITE_CONNECTION_FLOW -> startSiteConnectionFlow()
        DISMISS -> dismiss()
    }

    private fun startSiteCreationFlow() {
        ActivityLauncher.newBlogForResult(this)
        finish()
    }

    private fun startSiteConnectionFlow() {
        ActivityLauncher.addSelfHostedSiteForResult(this)
        finish()
    }

    private fun dismiss() {
        ActivityLauncher.viewReader(this)
        finish()
    }
}
