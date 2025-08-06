package org.wordpress.android.ui.accounts.applicationpassword

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import uniffi.wp_api.ApplicationPasswordWithViewContext

@AndroidEntryPoint
class ApplicationPasswordsListActivity : BaseAppCompatActivity() {
    private val viewModel: ApplicationPasswordsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppThemeM3 {
                ApplicationPasswordsDataViewScreen(
                    viewModel = viewModel,
                    onNavigateBack = { onBackPressedDispatcher.onBackPressed() },
                    onItemClick = { applicationPassword ->
                        val intent = ApplicationPasswordDetailsActivity.createIntent(this, applicationPassword)
                        startActivity(intent)
                    }
                )
            }
        }

        viewModel.onRefreshData()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationPasswordsDataViewScreen(
    viewModel: ApplicationPasswordsViewModel,
    onNavigateBack: () -> Unit,
    onItemClick: (ApplicationPasswordWithViewContext) -> Unit
) {
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
        DataViewScreen(
            uiState = viewModel.uiState.collectAsState(),
            supportedFilters = viewModel.getSupportedFilters(),
            supportedSorts = viewModel.getSupportedSorts(),
            onSearchQueryChange = { query -> viewModel.onSearchQueryChange(query) },
            onItemClick = { dataViewItem ->
                // Extract ApplicationPasswordWithViewContext from DataViewItem
                val applicationPassword = dataViewItem.data as? ApplicationPasswordWithViewContext
                applicationPassword?.let { onItemClick(it) }
                viewModel.onItemClick(dataViewItem)
            },
            onFilterClick = { filter -> viewModel.onFilterClick(filter) },
            onSortClick = { sortItem -> viewModel.onSortClick(sortItem) },
            onSortOrderClick = { order -> viewModel.onSortOrderClick(order) },
            onRefresh = { viewModel.onRefreshData() },
            onFetchMore = { viewModel.onFetchMoreData() },
            modifier = Modifier.padding(innerPadding)
        )
    }
}
