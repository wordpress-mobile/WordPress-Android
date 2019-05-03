package org.wordpress.android.ui.domains

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.toolbar.*
import org.wordpress.android.R

class DomainRegistrationActivity : AppCompatActivity(), DomainRegistrationStepsListener {
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
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
                R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
        )
        fragmentTransaction.replace(
                R.id.fragment_container,
                DomainRegistrationDetailsFragment.newInstance(domainProductDetails)
        )
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }

    override fun onDomainRegistered(domainName: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(
                R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right
        )
        fragmentTransaction.replace(
                R.id.fragment_container,
                DomainRegistrationResultFragment.newInstance(domainName)
        )
        fragmentTransaction.commit()
    }
}
