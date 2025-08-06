package org.wordpress.android.ui.accounts.applicationpassword

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.ApplicationPasswordUuid
import uniffi.wp_api.ApplicationPasswordAppId
import uniffi.wp_api.IpAddress

@AndroidEntryPoint
class ApplicationPasswordsListActivity : BaseAppCompatActivity() {
    private val viewModel: ApplicationPasswordsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                ApplicationPasswordsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onItemClick = { applicationPassword ->
                        val intent = ApplicationPasswordDetailsActivity.createIntent(this, applicationPassword)
                        startActivity(intent)
                    }
                )
            }
        }

        viewModel.loadApplicationPasswords()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPasswordsScreen(
    viewModel: ApplicationPasswordsListViewModel,
    onNavigateBack: () -> Unit,
    onItemClick: (ApplicationPasswordWithViewContext) -> Unit
) {
    val applicationPasswords by viewModel.applicationPasswords.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.application_password_info_title))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(applicationPasswords) { applicationPassword ->
                    ApplicationPasswordItem(
                        applicationPassword = applicationPassword,
                        onItemClick = onItemClick
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationPasswordItem(
    applicationPassword: ApplicationPasswordWithViewContext,
    onItemClick: ((ApplicationPasswordWithViewContext) -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            Text(
                text = applicationPassword.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        supportingContent = {
            Text(
                text = formatLastUsed(applicationPassword.lastUsed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        },
        trailingContent = if (onItemClick != null) {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.application_password_info_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else null,
        modifier = Modifier.clickable(enabled = onItemClick != null) {
            onItemClick?.invoke(applicationPassword)
        }
    )
}

@Composable
private fun formatLastUsed(lastUsed: String?): String {
    return if (lastUsed.isNullOrEmpty()) {
        stringResource(R.string.application_password_never_used)
    } else {
        lastUsed
    }
}

@Preview(showBackground = true, name = "Application Passwords Screen")
@Composable
fun ApplicationPasswordsScreenPreview() {
    AppThemeM3 {
        ApplicationPasswordsScreenContent(
            applicationPasswords = createSampleApplicationPasswords(),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Application Password Item")
@Composable
fun ApplicationPasswordItemPreview() {
    AppThemeM3 {
        ApplicationPasswordItem(
            applicationPassword = ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-1"),
                name = "WordPress Mobile App",
                appId = ApplicationPasswordAppId("wordpress-mobile"),
                created = "2024-01-01T00:00:00Z",
                lastUsed = "Used yesterday",
                lastIp = IpAddress("192.168.1.1")
            )
        )
    }
}

@Preview(showBackground = true, name = "Application Password Item - Never Used")
@Composable
fun ApplicationPasswordItemNeverUsedPreview() {
    AppThemeM3 {
        ApplicationPasswordItem(
            applicationPassword = ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-2"),
                name = "Third Party Integration Tool",
                appId = ApplicationPasswordAppId("third-party-tool"),
                created = "2024-01-02T00:00:00Z",
                lastUsed = null,
                lastIp = null
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationPasswordsScreenContent(
    applicationPasswords: List<ApplicationPasswordWithViewContext>,
    onNavigateBack: () -> Unit,
    onItemClick: (ApplicationPasswordWithViewContext) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Application Passwords")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(applicationPasswords) { password ->
                    ApplicationPasswordItem(
                        applicationPassword = password,
                        onItemClick = onItemClick
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

private fun createSampleApplicationPasswords(): List<ApplicationPasswordWithViewContext> {
    return (1..5).map {
        ApplicationPasswordWithViewContext(
            uuid = ApplicationPasswordUuid("uuid-$it"),
            name = "WordPress Mobile App - $it",
            appId = ApplicationPasswordAppId("wordpress-mobile"),
            created = "2024-01-01T00:00:00Z",
            lastUsed = "Used yesterday",
            lastIp = IpAddress("IP")
        )
    }
}
