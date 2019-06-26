package org.wordpress.android.ui.domains

import android.app.Activity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.ui.domains.DomainRegistrationActivity.DomainRegistrationPurpose.CTA_DOMAIN_CREDIT_REDEMPTION

class DomainRegistrationActivity : AppCompatActivity(), DomainRegistrationStepsListener {
    enum class DomainRegistrationPurpose {
        AUTOMATED_TRANSFER,
        CTA_DOMAIN_CREDIT_REDEMPTION
    }

    companion object {
        const val DOMAIN_REGISTRATION_PURPOSE_KEY = "DOMAIN_REGISTRATION_PURPOSE_KEY"
    }

    var domainRegistrationPurpose: DomainRegistrationPurpose? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_domain_suggestions_activity)

        domainRegistrationPurpose = intent.getSerializableExtra(DOMAIN_REGISTRATION_PURPOSE_KEY)
                as DomainRegistrationPurpose

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            DomainSuggestionsFragment.newInstance()
                    )
                    .commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDomainSelected(domainProductDetails: DomainProductDetails) {
        supportFragmentManager.beginTransaction().setCustomAnimations(
                R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
        )
                .replace(
                        R.id.fragment_container,
                        DomainRegistrationDetailsFragment.newInstance(domainProductDetails)
                )
                .addToBackStack(null)
                .commit()
    }

    override fun onDomainRegistered(domainName: String) {
        if (domainRegistrationPurpose == null || domainRegistrationPurpose == CTA_DOMAIN_CREDIT_REDEMPTION) {
            supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                            R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                            R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
                    )
                    .replace(
                            R.id.fragment_container,
                            DomainRegistrationResultFragment.newInstance(domainName)
                    )
                    .commit()
        } else {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}
