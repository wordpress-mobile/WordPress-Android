package org.wordpress.android.ui.debug

import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class DebugSettingsActivity : LocaleAwareActivity() {

    private var binding: DebugSettingsActivityBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DebugSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.setUpToolbar()
        binding?.setUpViewPager()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun DebugSettingsActivityBinding.setUpToolbar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    fun DebugSettingsActivityBinding.setUpViewPager() {
        if (viewPager.adapter == null) {
            viewPager.adapter = DebugSettingsTabAdapter(this@DebugSettingsActivity)
        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setText(
                when (position) {
                    0 -> R.string.debug_settings_remote_features
                    1 -> R.string.debug_settings_remote_field_configs
                    2 -> R.string.debug_settings_features_in_development
                    else -> throw IllegalArgumentException("Invalid position: $position")
                }
            )
        }.attach()
    }
}
