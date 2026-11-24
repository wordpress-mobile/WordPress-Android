package org.wordpress.android.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.ActivityLauncherWrapper
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.CAMPAIGN_STATIC_POSTER
import org.wordpress.android.ui.ActivityLauncherWrapper.Companion.JETPACK_PACKAGE_NAME
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

/**
 * Shown in the WordPress app's Notifications tab, which exists only because the Reader brought the bottom
 * nav back for WordPress.com users. Notifications themselves remain Jetpack-only.
 */
@AndroidEntryPoint
class NotificationsInJetpackFragment : Fragment() {
    @Inject
    lateinit var activityLauncherWrapper: ActivityLauncherWrapper

    @Inject
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppThemeM3 {
                NotificationsInJetpackScreen(onButtonClick = ::openJetpack)
            }
        }
    }

    private fun openJetpack() {
        analyticsTrackerWrapper.track(Stat.NOTIFICATIONS_IN_JETPACK_BUTTON_TAPPED)
        val activity = requireActivity()
        // Launch Jetpack directly when it's installed: openPlayStoreLink would also finish this app's task,
        // which is right for the "switch to Jetpack" flows but not for a tab the user may come back to.
        val launchIntent = activity.packageManager.getLaunchIntentForPackage(JETPACK_PACKAGE_NAME)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            activityLauncherWrapper.openPlayStoreLink(activity, JETPACK_PACKAGE_NAME, CAMPAIGN_STATIC_POSTER)
        }
    }

    companion object {
        fun newInstance() = NotificationsInJetpackFragment()
    }
}

@Composable
private fun NotificationsInJetpackScreen(onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_jetpack_logo_green_24dp),
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.notifications_in_jetpack_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.notifications_in_jetpack_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onButtonClick) {
            Text(stringResource(R.string.notifications_in_jetpack_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsInJetpackScreenPreview() {
    AppThemeM3 {
        NotificationsInJetpackScreen(onButtonClick = {})
    }
}
