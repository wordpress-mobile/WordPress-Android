package org.wordpress.android.ui.domains

import android.os.Bundle
import android.view.MenuItem
import org.wordpress.android.R
import org.wordpress.android.databinding.ActivityDomainsDashboardBinding
import org.wordpress.android.ui.LocaleAwareActivity

class DomainsDashboardActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(ActivityDomainsDashboardBinding.inflate(layoutInflater)) {
            setContentView(root)

            setSupportActionBar(toolbarDomains)
            supportActionBar?.let {
                it.setHomeButtonEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }

            val fm = supportFragmentManager
            var domainsDashboardFragment =
                    fm.findFragmentByTag(DomainsDashboardFragment.TAG) as? DomainsDashboardFragment

            if (domainsDashboardFragment == null) {
                domainsDashboardFragment = DomainsDashboardFragment.newInstance()
                fm.beginTransaction()
                        .add(R.id.fragment_container, domainsDashboardFragment, DomainsDashboardFragment.TAG)
                        .commit()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
