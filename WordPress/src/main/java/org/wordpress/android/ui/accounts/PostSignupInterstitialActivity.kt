package org.wordpress.android.ui.accounts

import android.os.Bundle
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.PostSignupInterstitialActivityBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginFragment
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.DISMISS
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CONNECTION_FLOW
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel.NavigationAction.START_SITE_CREATION_FLOW
import javax.inject.Inject

@AndroidEntryPoint
class PostSignupInterstitialActivity : LocaleAwareActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: PostSignupInterstitialViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        LoginFlowThemeHelper.injectMissingCustomAttributes(theme)

        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(PostSignupInterstitialViewModel::class.java)
        val binding = PostSignupInterstitialActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        onBackPressedDispatcher.addCallback(this) { viewModel.onBackButtonPressed() }

        with(binding) {
            viewModel.onInterstitialShown()
            createNewSiteButton().setOnClickListener { viewModel.onCreateNewSiteButtonPressed() }
            addSelfHostedSiteButton().setOnClickListener { viewModel.onAddSelfHostedSiteButtonPressed() }
            dismissButton().setOnClickListener { viewModel.onDismissButtonPressed() }
        }

        viewModel.navigationAction.observe(this) { executeAction(it) }
    }

    private fun PostSignupInterstitialActivityBinding.createNewSiteButton() =
        root.findViewById<MaterialButton>(R.id.create_new_site_button)

    private fun PostSignupInterstitialActivityBinding.addSelfHostedSiteButton() =
        root.findViewById<MaterialButton>(R.id.add_self_hosted_site_button)

    private fun PostSignupInterstitialActivityBinding.dismissButton() =
        root.findViewById<MaterialButton>(R.id.dismiss_button)

    private fun executeAction(navigationAction: NavigationAction) = when (navigationAction) {
        START_SITE_CREATION_FLOW -> startSiteCreationFlow()
        START_SITE_CONNECTION_FLOW -> startSiteConnectionFlow()
        SHOW_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY -> showJetpackIndividualPluginOverlay()
        DISMISS -> dismiss()
    }

    private fun startSiteCreationFlow() {
        ActivityLauncher.showMainActivityAndSiteCreationActivity(this, SiteCreationSource.SIGNUP_EPILOGUE)
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

    private fun showJetpackIndividualPluginOverlay() {
        WPJetpackIndividualPluginFragment.show(supportFragmentManager)
    }
}
