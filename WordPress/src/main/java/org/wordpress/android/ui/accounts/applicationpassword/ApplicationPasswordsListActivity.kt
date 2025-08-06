package org.wordpress.android.ui.accounts.applicationpassword

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.BaseAppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class ApplicationPasswordsListActivity : BaseAppCompatActivity() {
    private val viewModel: ApplicationPasswordsListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                ApplicationPasswordsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() }
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
    onNavigateBack: () -> Unit
) {
    val applicationPasswords by viewModel.applicationPasswords.observeAsState(emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.application_passwords))
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
                    ApplicationPasswordItem(password = password)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun ApplicationPasswordItem(password: ApplicationPasswordWithViewContext) {
    ListItem(
        headlineContent = {
            Text(
                text = password.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = formatLastUsed(password.lastUsed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun formatLastUsed(lastUsed: Date?): String {
    if (lastUsed == null) {
        return stringResource(R.string.application_password_never_used)
    }

    val now = Calendar.getInstance().time
    val diffInMillis = now.time - lastUsed.time
    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        diffInDays == 0L -> stringResource(R.string.application_password_used_today)
        diffInDays == 1L -> stringResource(R.string.application_password_used_yesterday)
        diffInDays < 7 -> stringResource(R.string.application_password_used_days_ago, diffInDays)
        diffInDays < 30 -> {
            val weeks = diffInDays / 7
            if (weeks == 1L) {
                stringResource(R.string.application_password_used_week_ago)
            } else {
                stringResource(R.string.application_password_used_weeks_ago, weeks)
            }
        }
        diffInDays < 365 -> {
            val months = diffInDays / 30
            if (months == 1L) {
                stringResource(R.string.application_password_used_month_ago)
            } else {
                stringResource(R.string.application_password_used_months_ago, months)
            }
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            stringResource(R.string.application_password_used_on_date, dateFormat.format(lastUsed))
        }
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
            password = ApplicationPasswordWithViewContext(
                uuid = "uuid-1",
                name = "WordPress Mobile App",
                lastUsed = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time
            )
        )
    }
}

@Preview(showBackground = true, name = "Application Password Item - Never Used")
@Composable
fun ApplicationPasswordItemNeverUsedPreview() {
    AppThemeM3 {
        ApplicationPasswordItem(
            password = ApplicationPasswordWithViewContext(
                uuid = "uuid-2",
                name = "Third Party Integration Tool",
                lastUsed = null
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationPasswordsScreenContent(
    applicationPasswords: List<ApplicationPasswordWithViewContext>,
    onNavigateBack: () -> Unit
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
                            contentDescription = "Back"
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
                    ApplicationPasswordItem(password = password)
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun createSampleApplicationPasswords(): List<ApplicationPasswordWithViewContext> {
    return listOf(
        ApplicationPasswordWithViewContext(
            uuid = "uuid-1",
            name = "WordPress Mobile App",
            lastUsed = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -1)
            }.time
        ),
        ApplicationPasswordWithViewContext(
            uuid = "uuid-2",
            name = "Jetpack Mobile App",
            lastUsed = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -3)
            }.time
        ),
        ApplicationPasswordWithViewContext(
            uuid = "uuid-3",
            name = "Desktop Publisher",
            lastUsed = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -2)
            }.time
        ),
        ApplicationPasswordWithViewContext(
            uuid = "uuid-4",
            name = "Third Party Integration",
            lastUsed = null
        ),
        ApplicationPasswordWithViewContext(
            uuid = "uuid-5",
            name = "Legacy API Client",
            lastUsed = Calendar.getInstance().apply {
                add(Calendar.MONTH, -6)
            }.time
        )
    )
}
