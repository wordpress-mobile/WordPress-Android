package org.wordpress.android.ui.prefs

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.AppSettingsActivityBinding
import org.wordpress.android.ui.LocaleAwareActivity

@AndroidEntryPoint
class AppSettingsActivity : LocaleAwareActivity() {
    private lateinit var binding: AppSettingsActivityBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AppSettingsActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarMain)
        supportActionBar?.run {
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setTitle(R.string.me_btn_app_settings)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter requires
    // SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    override fun recreate() {
        startActivity(
            Intent(this, this::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    companion object {
        const val EXTRA_SHOW_PRIVACY_SETTINGS = "extra_show_privacy_settings"
        const val EXTRA_REQUESTED_ANALYTICS_VALUE_FROM_ERROR =
            "extra_requested_analytics_value_from_error"
    }
}
