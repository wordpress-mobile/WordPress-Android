package org.wordpress.android.ui.accounts.applicationpassword

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import uniffi.wp_api.ApplicationPasswordWithViewContext
import uniffi.wp_api.IpAddress

@AndroidEntryPoint
class ApplicationPasswordsListActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<ApplicationPasswordsViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NavigableContent()
                }
            }
        )
    }

    private enum class ApplicationPasswordScreen {
        List,
        Detail
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavigableContent() {
        navController = rememberNavController()
        val listTitle = stringResource(R.string.application_password_info_title)
        val titleState = remember { mutableStateOf(listTitle) }

        AppThemeM3 {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(titleState.value) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (navController.previousBackStackEntry != null) {
                                    navController.navigateUp()
                                } else {
                                    finish()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        }
                    )
                },
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ApplicationPasswordScreen.List.name
                ) {
                    composable(route = ApplicationPasswordScreen.List.name) {
                        titleState.value = listTitle
                        ShowListScreen(
                            navController,
                            modifier = Modifier.padding(contentPadding)
                        )
                    }

                    composable(route = ApplicationPasswordScreen.Detail.name) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            val uuid = handle.get<String>(KEY_APP_PASSWORD_UUID)
                            if (uuid != null) {
                                viewModel.getApplicationPassword(uuid)?.let { applicationPassword ->
                                    titleState.value = applicationPassword.name
                                    ShowApplicationPasswordDetailScreen(
                                        applicationPassword = applicationPassword,
                                        modifier = Modifier.padding(contentPadding)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ShowListScreen(
        navController: NavHostController,
        modifier: Modifier
    ) {
        DataViewScreen(
            uiState = viewModel.uiState.collectAsState(),
            supportedFilters = viewModel.getSupportedFilters(),
            supportedSorts = viewModel.getSupportedSorts(),
            onRefresh = {
                viewModel.onRefreshData()
            },
            onFetchMore = {
                viewModel.onFetchMoreData()
            },
            onSearchQueryChange = { query ->
                viewModel.onSearchQueryChange(query)
            },
            onItemClick = { item ->
                viewModel.onItemClick(item)
                (item.data as? ApplicationPasswordWithViewContext)?.let { applicationPassword ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        key = KEY_APP_PASSWORD_UUID,
                        value = applicationPassword.uuid.uuid
                    )
                    navController.navigate(route = ApplicationPasswordScreen.Detail.name)
                }
            },
            onFilterClick = { filter ->
                viewModel.onFilterClick(filter)
            },
            onSortClick = { sort ->
                viewModel.onSortClick(sort)
            },
            onSortOrderClick = { order ->
                viewModel.onSortOrderClick(order)
            },
            modifier = modifier
        )
    }

    @Composable
    private fun ShowApplicationPasswordDetailScreen(
        applicationPassword: ApplicationPasswordWithViewContext,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ApplicationPasswordDetailsCard(applicationPassword)
        }
    }

    @Composable
    private fun ApplicationPasswordDetailsCard(applicationPassword: ApplicationPasswordWithViewContext) {
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

    companion object {
        private const val KEY_APP_PASSWORD_UUID = "applicationPasswordUuid"
    }
}
