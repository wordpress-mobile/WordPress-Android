package org.wordpress.android.ui.reader

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.extensions.setContent

/*
 * Serves as an intermediate screen where the user is informed that a site is needed for the reblog action
 */
class NoSiteToReblogActivity : BaseAppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM3 {
                NoSiteToReblogScreen(
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onViewMySiteClick = ::onViewMySiteClicked
                )
            }
        }
    }

    // overridePendingTransition is deprecated in SDK 34 in favor of overrideActivityTransition, but the latter requires
    // SDK 34. overridePendingTransition still works on Android 14 so using it should be safe for now.
    @Suppress("DEPRECATION")
    private fun onViewMySiteClicked() {
        ActivityLauncher.viewMySiteInNewStack(this)
        setResult(RESULT_OK)
        finish()
        overridePendingTransition(0, 0)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoSiteToReblogScreen(onBack: () -> Unit, onViewMySiteClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { contentPadding ->
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(Margin.ExtraMediumLarge.value)
        ) {
            if (!isLandscape) {
                Image(
                    painter = painterResource(R.drawable.illustration_reader_empty),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = Margin.ExtraLarge.value)
                )
            }
            Text(
                text = stringResource(R.string.reader_no_blog_to_reblog_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.reader_no_blog_to_reblog_detail),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Margin.Medium.value)
            )
            Button(onClick = onViewMySiteClick, modifier = Modifier.padding(top = Margin.ExtraMediumLarge.value)) {
                Text(stringResource(R.string.reader_no_blog_to_reblog_action))
            }
        }
    }
}
