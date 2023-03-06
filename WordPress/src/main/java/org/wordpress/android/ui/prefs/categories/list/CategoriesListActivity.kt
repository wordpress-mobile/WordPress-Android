package org.wordpress.android.ui.prefs.categories.list

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.databinding.SiteSettingsCategoriesListActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class CategoriesListActivity : LocaleAwareActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        with(SiteSettingsCategoriesListActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            setSupportActionBar(toolbarMain)
            supportActionBar?.let {
                it.setHomeButtonEnabled(true)
                it.setDisplayHomeAsUpEnabled(true)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
