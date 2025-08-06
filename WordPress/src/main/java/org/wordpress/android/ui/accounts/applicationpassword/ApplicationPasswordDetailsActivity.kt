package org.wordpress.android.ui.accounts.applicationpassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.ApplicationPasswordUuid
import uniffi.wp_api.ApplicationPasswordAppId
import uniffi.wp_api.IpAddress

class ApplicationPasswordDetailsActivity : BaseAppCompatActivity() {
    companion object {
        private const val EXTRA_NAME = "extra_name"
        private const val EXTRA_UUID = "extra_uuid"
        private const val EXTRA_APP_ID = "extra_app_id"
        private const val EXTRA_CREATED = "extra_created"
        private const val EXTRA_LAST_USED = "extra_last_used"
        private const val EXTRA_LAST_IP = "extra_last_ip"

        fun createIntent(context: Context, applicationPassword: ApplicationPasswordWithViewContext): Intent {
            return Intent(context, ApplicationPasswordDetailsActivity::class.java).apply {
                putExtra(EXTRA_NAME, applicationPassword.name)
                putExtra(EXTRA_UUID, applicationPassword.uuid.uuid)
                putExtra(EXTRA_APP_ID, applicationPassword.appId.appId)
                putExtra(EXTRA_CREATED, applicationPassword.created)
                putExtra(EXTRA_LAST_USED, applicationPassword.lastUsed)
                putExtra(EXTRA_LAST_IP, applicationPassword.lastIp?.value)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val name = intent.getStringExtra(EXTRA_NAME)
        val uuid = intent.getStringExtra(EXTRA_UUID)
        val appId = intent.getStringExtra(EXTRA_APP_ID)
        val created = intent.getStringExtra(EXTRA_CREATED)
        val lastUsed = intent.getStringExtra(EXTRA_LAST_USED)
        val lastIp = intent.getStringExtra(EXTRA_LAST_IP)

        // Recreate the ApplicationPasswordWithViewContext object for display
        val applicationPassword = ApplicationPasswordWithViewContext(
            uuid = ApplicationPasswordUuid(uuid.orEmpty()),
            name = name.orEmpty(),
            appId = ApplicationPasswordAppId(appId.orEmpty()),
            created = created.orEmpty(),
            lastUsed = lastUsed,
            lastIp = lastIp?.let { IpAddress(it) }
        )

        setContent {
            AppThemeM3 {
                ApplicationPasswordDetailsScreen(
                    applicationPassword = applicationPassword,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPasswordDetailsScreen(
    applicationPassword: ApplicationPasswordWithViewContext,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = applicationPassword.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ApplicationPasswordDetailsCard(applicationPassword)
        }
    }
}

@Composable
fun ApplicationPasswordDetailsCard(applicationPassword: ApplicationPasswordWithViewContext) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailRow(
                label = stringResource(R.string.application_password_name_label),
                value = applicationPassword.name
            )

            DetailRow(
                label = stringResource(R.string.application_password_created_label),
                value = applicationPassword.created
            )

            DetailRow(
                label = stringResource(R.string.application_password_last_used_label),
                value = formatLastUsedDetails(applicationPassword.lastUsed)
            )

            DetailRow(
                label = stringResource(R.string.application_password_last_ip_label),
                value = formatLastIp(applicationPassword.lastIp)
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.3f)
        )


        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.7f)
        )
    }
}

@Composable
private fun formatLastUsedDetails(lastUsed: String?): String {
    return if (lastUsed.isNullOrEmpty()) {
        stringResource(R.string.application_password_never_used)
    } else {
        lastUsed
    }
}

@Composable
private fun formatLastIp(lastIp: IpAddress?): String {
    return lastIp?.value ?: stringResource(R.string.application_password_no_ip)
}

@Preview(showBackground = true, name = "Application Password Details Screen")
@Composable
fun ApplicationPasswordDetailsScreenPreview() {
    AppThemeM3 {
        ApplicationPasswordDetailsScreen(
            applicationPassword = ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-1"),
                name = "WordPress Mobile App",
                appId = ApplicationPasswordAppId("wordpress-mobile"),
                created = "2024-01-01T00:00:00Z",
                lastUsed = "Used yesterday",
                lastIp = IpAddress("IP")
            ),
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Application Password Details - Never Used")
@Composable
fun ApplicationPasswordDetailsNeverUsedPreview() {
    AppThemeM3 {
        ApplicationPasswordDetailsScreen(
            applicationPassword = ApplicationPasswordWithViewContext(
                uuid = ApplicationPasswordUuid("uuid-2"),
                name = "Third Party Integration Tool",
                appId = ApplicationPasswordAppId("third-party-tool"),
                created = "2024-01-02T00:00:00Z",
                lastUsed = null,
                lastIp = null
            ),
            onNavigateBack = {}
        )
    }
}
