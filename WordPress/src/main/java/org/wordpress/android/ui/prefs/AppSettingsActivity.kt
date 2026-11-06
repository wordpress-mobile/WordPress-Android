package org.wordpress.android.ui.prefs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.debug.DebugSettingsActivity
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.prefs.appsettings.AppSettingsEvent
import org.wordpress.android.ui.prefs.appsettings.AppSettingsScreen
import org.wordpress.android.ui.prefs.appsettings.AppSettingsViewModel
import org.wordpress.android.ui.prefs.language.LocalePickerBottomSheet
import org.wordpress.android.ui.prefs.language.LocalePickerBottomSheet.LocalePickerCallback
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementDialogFragment
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppThemeUtils
import org.wordpress.android.util.PerAppLocaleManager
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.setContent
import javax.inject.Inject

@AndroidEntryPoint
class AppSettingsActivity : BaseAppCompatActivity(), LocalePickerCallback {
    @Inject lateinit var perAppLocaleManager: PerAppLocaleManager

    private val viewModel: AppSettingsViewModel by viewModels()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (supportFragmentManager.findFragmentByTag(LocalePickerBottomSheet.TAG) as? LocalePickerBottomSheet)
            ?.setLocalePickerCallback(this)
        // flush gathered events (if any)
        AnalyticsTracker.flush()

        setContent {
            AppThemeM3 {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                // The locale can change while this screen is showing, which updates the configuration
                val configuration = LocalConfiguration.current
                val languageSummary = remember(configuration) {
                    perAppLocaleManager.getCurrentLocaleDisplayName()
                }
                AppSettingsScreen(
                    uiState = uiState,
                    languageSummary = languageSummary,
                    onNavigateBack = onBackPressedDispatcher::onBackPressed,
                    onLanguageClick = ::openLanguagePicker,
                    onThemeSelected = viewModel::onThemeSelected,
                    onOpenLinksInJetpackChanged = viewModel::onOpenLinksInJetpackChanged,
                    onDeviceSettingsClick = viewModel::onDeviceSettingsClick,
                    onPrivacySettingsClick = viewModel::onPrivacySettingsClick,
                    onPrivacySettingsClosed = viewModel::onPrivacySettingsClosed,
                    onImageOptimizeChanged = viewModel::onImageOptimizeChanged,
                    onStripImageLocationChanged = viewModel::onStripImageLocationChanged,
                    onVideoOptimizeChanged = viewModel::onVideoOptimizeChanged,
                    onMediaChoiceSelected = viewModel::onMediaChoiceSelected,
                    onWhatsNewClick = viewModel::onWhatsNewClick,
                    onExperimentalFeaturesClick = { ActivityLauncher.viewExperimentalFeatures(this) },
                    onDebugSettingsClick = { startActivity(Intent(this, DebugSettingsActivity::class.java)) },
                    onSendUsageStatsChanged = viewModel::onSendUsageStatsChanged,
                    onSendCrashReportsChanged = viewModel::onSendCrashReportsChanged,
                    onLearnMoreClick = viewModel::onLearnMoreClick,
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleEvent)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onStop() {
        viewModel.onStop()
        super.onStop()
    }

    private fun handleEvent(event: AppSettingsEvent) {
        when (event) {
            AppSettingsEvent.OpenDeviceSettings -> openDeviceSettings()
            AppSettingsEvent.ShowWhatsNew -> FeatureAnnouncementDialogFragment()
                .show(supportFragmentManager, FeatureAnnouncementDialogFragment.TAG)
            is AppSettingsEvent.OpenUrl -> openUrl(event.url)
            is AppSettingsEvent.ShowToast -> ToastUtils.showToast(this, event.messageRes, ToastUtils.Duration.LONG)
            is AppSettingsEvent.ApplyTheme -> {
                AppThemeUtils.setAppTheme(this, event.themeValue)
                recreate()
            }
        }
    }

    private fun openLanguagePicker() {
        // On API 33+ the app language is changed in the system's per-app language settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perAppLocaleManager.openAppLanguageSettings(this)
        } else {
            LocalePickerBottomSheet.newInstance().also {
                it.setLocalePickerCallback(this)
                it.show(supportFragmentManager, LocalePickerBottomSheet.TAG)
            }
        }
    }

    override fun onLocaleSelected(languageCode: String) {
        perAppLocaleManager.onLanguageChanged(languageCode)
    }

    private fun openDeviceSettings() {
        try {
            // open specific app info screen
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData("package:$packageName".toUri())
            )
        } catch (e: ActivityNotFoundException) {
            AppLog.w(AppLog.T.SETTINGS, e.message)
            // open generic apps screen
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    private fun openUrl(url: String) {
        @Suppress("UnsafeImplicitIntentLaunch")
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
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
        const val LANGUAGE_CHANGED = 1000
        const val EXTRA_SHOW_PRIVACY_SETTINGS = "extra_show_privacy_settings"
    }
}
