package org.wordpress.android.ui.domains

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.DomainSuggestionsActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.ScrollableViewInitializedListener
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.FinishDomainRegistration
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationDetails
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainRegistrationResult
import org.wordpress.android.ui.domains.DomainRegistrationNavigationAction.OpenDomainSuggestions
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

class DomainRegistrationActivity : LocaleAwareActivity(), ScrollableViewInitializedListener {
    enum class DomainRegistrationPurpose {
        AUTOMATED_TRANSFER,
        CTA_DOMAIN_CREDIT_REDEMPTION,
        DOMAIN_PURCHASE
    }

    companion object {
        const val DOMAIN_REGISTRATION_PURPOSE_KEY = "DOMAIN_REGISTRATION_PURPOSE_KEY"
    }

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: DomainRegistrationMainViewModel
    private lateinit var binding: DomainSuggestionsActivityBinding
    private lateinit var site: SiteModel
    private lateinit var domainRegistrationPurpose: DomainRegistrationPurpose

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        with(DomainSuggestionsActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            binding = this

            site = intent.getSerializableExtra(WordPress.SITE) as SiteModel

            domainRegistrationPurpose = intent.getSerializableExtra(DOMAIN_REGISTRATION_PURPOSE_KEY)
                    as DomainRegistrationPurpose

            setupToolbar()
            setupViewModel(site, domainRegistrationPurpose)
            setupObservers()
        }
    }

    private fun DomainSuggestionsActivityBinding.setupToolbar() {
        setSupportActionBar(toolbarDomain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViewModel(site: SiteModel, domainRegistrationPurpose: DomainRegistrationPurpose) {
        viewModel = ViewModelProvider(this, viewModelFactory).get(DomainRegistrationMainViewModel::class.java)
        viewModel.start(site, domainRegistrationPurpose)
    }

    private fun setupObservers() {
        viewModel.onNavigation.observeEvent(this) {
            when (it) {
                is OpenDomainSuggestions -> showDomainSuggestions()
                is OpenDomainRegistrationDetails -> showDomainRegistrationDetails(it.details)
                is OpenDomainRegistrationResult -> showDomainRegistrationResult(it.event)
                is FinishDomainRegistration -> finishDomainRegistration(it.event)
            }
        }
    }

    private fun showDomainSuggestions() {
        var fragment = supportFragmentManager.findFragmentByTag(DomainSuggestionsFragment.TAG)
        if (fragment == null) {
            fragment = DomainSuggestionsFragment.newInstance()
            showFragment(
                    fragment,
                    DomainSuggestionsFragment.TAG,
                    slideIn = false,
                    isRootFragment = true
            )
        }
    }

    private fun showDomainRegistrationDetails(details: DomainProductDetails) {
        var fragment = supportFragmentManager.findFragmentByTag(DomainRegistrationDetailsFragment.TAG)
        if (fragment == null) {
            fragment = DomainRegistrationDetailsFragment.newInstance(details)
            showFragment(fragment, DomainRegistrationDetailsFragment.TAG)
        }
    }

    private fun showDomainRegistrationResult(event: DomainRegistrationCompletedEvent) {
        var fragment = supportFragmentManager.findFragmentByTag(DomainRegistrationResultFragment.TAG)
        if (fragment == null) {
            fragment = DomainRegistrationResultFragment.newInstance(event.domainName, event.email)
            showFragment(fragment, DomainRegistrationResultFragment.TAG)
        }
    }

    private fun finishDomainRegistration(event: DomainRegistrationCompletedEvent) {
        val intent = Intent()
        intent.putExtra(DomainRegistrationResultFragment.RESULT_REGISTERED_DOMAIN_EMAIL, event.email)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun showFragment(
        fragment: Fragment,
        tag: String,
        slideIn: Boolean = true,
        isRootFragment: Boolean = false
    ) {
        val transaction = supportFragmentManager.beginTransaction()

        if (slideIn) {
            transaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
            )
        }
        if (!isRootFragment) {
            transaction.addToBackStack(null)
        }

        transaction.replace(R.id.fragment_container, fragment, tag).commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onScrollableViewInitialized(containerId: Int) {
        binding.appbarMain.apply {
            if (containerId == R.id.domain_suggestions_list) {
                post {
                    isLiftOnScroll = false
                    setLifted(false)
                    elevation = 0F
                    requestLayout()
                }
            } else {
                post {
                    isLiftOnScroll = true
                    liftOnScrollTargetViewId = containerId
                    requestLayout()
                }
            }
        }
    }
}
