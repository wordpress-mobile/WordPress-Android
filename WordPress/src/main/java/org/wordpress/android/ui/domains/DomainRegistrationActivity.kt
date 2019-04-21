package org.wordpress.android.ui.domains

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R
import org.wordpress.android.ui.domains.DomainSuggestionsFragment.OnDomainSelectedListener

class DomainRegistrationActivity : AppCompatActivity(), OnDomainSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_domain_suggestions_activity)

        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }

        if (savedInstanceState == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(
                    R.id.fragment_container,
                    DomainSuggestionsFragment.newInstance()
            )
            fragmentTransaction.commit()
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
        // TODO show Domain registration details
    }
}
