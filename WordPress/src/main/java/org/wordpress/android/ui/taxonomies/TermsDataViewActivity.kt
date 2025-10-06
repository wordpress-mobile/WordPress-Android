package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.Intent
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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.AppLog
import uniffi.wp_api.AnyTermWithEditContext
import javax.inject.Inject

@AndroidEntryPoint
class TermsDataViewActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var appLogWrapper: AppLogWrapper

    private val viewModel by viewModels<TermsViewModel>()

    private lateinit var composeView: ComposeView
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taxonomySlug = intent.getStringExtra(TAXONOMY_SLUG)
        val isHierarchical = intent.getBooleanExtra(IS_HIERARCHICAL, false)
        val taxonomyName = intent.getStringExtra(TAXONOMY_NAME) ?: ""
        if (taxonomySlug == null) {
            appLogWrapper.e(AppLog.T.API, "Error: No taxonomy selected")
            finish()
            return
        }

        viewModel.initialize(taxonomySlug, isHierarchical)

        composeView = ComposeView(this)
        setContentView(
            composeView.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    this.isForceDarkAllowed = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    NavigableContent(taxonomyName)
                }
            }
        )
    }

    private enum class TermScreen {
        List,
        Detail
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavigableContent(taxonomyName: String) {
        navController = rememberNavController()
        val listTitle = taxonomyName
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
                    startDestination = TermScreen.List.name
                ) {
                    composable(route = TermScreen.List.name) {
                        titleState.value = listTitle
                        ShowListScreen(
                            navController,
                            modifier = Modifier.padding(contentPadding)
                        )
                    }

                    composable(route = TermScreen.Detail.name) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            val termId = handle.get<Long>(KEY_TERM_ID)
                            if (termId != null) {
                                viewModel.getTerm(termId)?.let { term ->
                                    titleState.value = term.name
                                    ShowTermDetailScreen(
                                        allTerms = viewModel.getAllTerms(),
                                        term = term,
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
                (item.data as? AnyTermWithEditContext)?.let { term ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        key = KEY_TERM_ID,
                        value = term.id
                    )
                    navController.navigate(route = TermScreen.Detail.name)
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
            emptyView = viewModel.emptyView,
            modifier = modifier
        )
    }

    @Composable
    private fun ShowTermDetailScreen(
        allTerms: List<AnyTermWithEditContext>,
        term: AnyTermWithEditContext,
        modifier: Modifier
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TermDetailsCard(allTerms, term)
        }
    }

    @Composable
    private fun TermDetailsCard(allTerms: List<AnyTermWithEditContext>, term: AnyTermWithEditContext) {
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
                    label = stringResource(R.string.term_name_label),
                    value = term.name
                )

                DetailRow(
                    label = stringResource(R.string.term_slug_label),
                    value = term.slug
                )

                DetailRow(
                    label = stringResource(R.string.term_description_label),
                    value = term.description
                )

                DetailRow(
                    label = stringResource(R.string.term_count_label),
                    value = term.count.toString()
                )

                term.parent?.let { parentId ->
                    val parentName = allTerms.firstOrNull {  it.id == parentId }?.name
                    parentName?.let {
                        DetailRow(
                            label = stringResource(R.string.term_parent_label),
                            value = parentName
                        )
                    }
                }
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

    companion object {
        private const val TAXONOMY_SLUG = "taxonomy_slug"
        private const val IS_HIERARCHICAL = "is_hierarchical"
        private const val TAXONOMY_NAME = "taxonomy_name"
        private const val KEY_TERM_ID = "termId"

        fun getIntent(context: Context, taxonomySlug: String, taxonomyName: String, isHierarchical: Boolean): Intent =
            Intent(context, TermsDataViewActivity::class.java).apply {
                putExtra(TAXONOMY_SLUG, taxonomySlug)
                putExtra(TAXONOMY_NAME, taxonomyName)
                putExtra(IS_HIERARCHICAL, isHierarchical)
            }
    }
}
