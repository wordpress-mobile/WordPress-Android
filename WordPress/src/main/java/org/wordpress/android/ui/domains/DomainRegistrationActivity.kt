package org.wordpress.android.ui.domains

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel

class DomainRegistrationActivity : AppCompatActivity(),
        OnDomainSelectedListener,
        OnDomainRegisteredListener {
    private var site: SiteModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_domain_suggestions_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null) {
            site = intent.getSerializableExtra(WordPress.SITE) as SiteModel
            showFragment(DomainSuggestionsFragment.newInstance(), "domain_suggestion_fragment")
        } else {
            site = savedInstanceState.getSerializable(WordPress.SITE) as SiteModel
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
        val whoisFragment = DomainRegistrationDetailsFragment.newInstance(
                domainProductDetails
        )
        showFragment(whoisFragment, "whois")
    }

    private fun showFragment(fragment: Fragment, tag: kotlin.String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
        fragmentTransaction.commit()
    }

    override fun onDomainRegistered(domainName: String) {
        showFragment(DomainRegistrationResultFragment.newInstance(domainName), "domain_success")
    }
}

interface OnDomainSelectedListener {
    fun onDomainSelected(domainProductDetails: DomainProductDetails)
}

interface OnDomainRegisteredListener {
    fun onDomainRegistered(domainName: String)
}
