package org.wordpress.android.ui.debug

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.DebugSettingsActivityBinding
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.debug.previews.PreviewFragmentActivity.Companion.previewFragmentInActivity
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject
import android.R as AndroidR

@AndroidEntryPoint
class DebugSettingsActivity : LocaleAwareActivity() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var viewModel: DebugSettingsViewModel

    private var binding: DebugSettingsActivityBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DebugSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.setUpToolbar()
        binding?.setUpViewPager()
        setUpViewModel()
    }

    private fun setUpViewModel() {
        viewModel = ViewModelProvider(
            this@DebugSettingsActivity,
            viewModelFactory
        )[DebugSettingsViewModel::class.java]
        viewModel.onNavigation.observeEvent(this@DebugSettingsActivity) {
            when (it) {
                is DebugSettingsViewModel.NavigationAction.DebugCookies ->
                    ActivityLauncher.viewDebugCookies(this@DebugSettingsActivity)
                is DebugSettingsViewModel.NavigationAction.PreviewFragment -> {
                    previewFragmentInActivity(it.name)
                }
            }
        }
    }

    fun DebugSettingsActivityBinding.setUpToolbar() {
        setSupportActionBar(toolbarMain)
        supportActionBar?.let {
            it.setHomeButtonEnabled(true)
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.debug_settings_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            AndroidR.id.home -> onBackPressedDispatcher.onBackPressed()
            R.id.menu_debug_cookies -> viewModel.onDebugCookiesClick()
            R.id.menu_restart_app -> viewModel.onRestartAppClick()
            R.id.menu_show_weekly_notifications -> viewModel.onForceShowWeeklyRoundupClick()
        }
        return true
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
