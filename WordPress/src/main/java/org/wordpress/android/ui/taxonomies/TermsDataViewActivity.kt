package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.ToastUtils
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

        viewModel.initialize(taxonomySlug, isHierarchical)
        lifecycleScope.launch {
            viewModel.uiEvent.filterNotNull().collect { event ->
                when (event) {
                    is UiEvent.ShowError -> ToastUtils.showToast(
                        this@TermsDataViewActivity,
                        event.messageRes,
                        ToastUtils.Duration.LONG
                    )
                }
                viewModel.consumeUIEvent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavigableContent(taxonomyName: String) {
        navController = rememberNavController()

        LaunchedEffect(navController) {
            viewModel.setNavController(navController)
        }

        val termDetailState by viewModel.termDetailState.collectAsState()

        // Observe navigation changes to trigger recomposition
        val currentBackStackEntry by navController.currentBackStackEntryFlow
            .collectAsState(initial = navController.currentBackStackEntry)
        val currentRoute = currentBackStackEntry?.destination?.route

        LaunchedEffect(termDetailState) {
            if (termDetailState == null && currentRoute != TermScreen.List.name) {
                navController.navigateUp()
            }
        }

        AppThemeM3 {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(taxonomyName) },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (navController.previousBackStackEntry != null) {
                                    viewModel.navigateBack()
                                } else {
                                    finish()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                            }
                        },
                        actions = {
                            // Show the add button only on the List screen
                            if (currentRoute == TermScreen.List.name) {
                                IconButton(onClick = {
                                    viewModel.navigateToCreateTerm()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_term)
                                    )
                                }
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
                        ShowListScreen(
                            modifier = Modifier.padding(contentPadding)
                        )
                    }

                    composable(route = TermScreen.Detail.name) {
                        termDetailState?.let { state ->
                            ShowTermDetailScreen(
                                state = state,
                                modifier = Modifier.padding(contentPadding)
                            )
                        }
                    }

                    composable(route = TermScreen.Create.name) {
                        termDetailState?.let { state ->
                            ShowTermDetailScreen(
                                state = state,
                                modifier = Modifier.padding(contentPadding)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ShowListScreen(
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
                    viewModel.navigateToTermDetail(term.id)
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
        state: TermDetailUiState,
        modifier: Modifier
    ) {
        val isSaving by viewModel.isSaving.collectAsState()
        val isDeleting by viewModel.isDeleting.collectAsState()

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TermDetailsCard(state = state)

            Button(
                onClick = { viewModel.saveTerm() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && !isDeleting
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }

            // Only show delete button if editing existing term (termId != 0)
            if (state.termId != 0L) {
                DeleteTermButton(
                    isDeleting,
                    onClick = {
                        if (!isDeleting) {
                            showDeleteTermConfirmation(state.termId, state.name)
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun DeleteTermButton(
        isDeleting: Boolean,
        onClick: () -> Unit,
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.error
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            if (isDeleting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = stringResource(R.string.delete),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    private fun TermDetailsCard(state: TermDetailUiState) {
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
                EditableDetailField(
                    label = stringResource(R.string.term_name_label),
                    value = state.name,
                    onValueChange = { viewModel.updateTermName(it) }
                )

                EditableDetailField(
                    label = stringResource(R.string.term_slug_label),
                    value = state.slug,
                    onValueChange = { viewModel.updateTermSlug(it) }
                )

                EditableDetailField(
                    label = stringResource(R.string.term_description_label),
                    value = state.description,
                    onValueChange = { viewModel.updateTermDescription(it) },
                    singleLine = false
                )

                if (!state.availableParents.isNullOrEmpty() && state.parentId != null) {
                    ParentDropdownField(
                        label = stringResource(R.string.term_parent_label),
                        availableParents = state.availableParents,
                        selectedParentId = state.parentId,
                        onParentIdChange = { viewModel.updateTermParent(it) }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ParentDropdownField(
        label: String,
        availableParents: List<ParentOption>,
        selectedParentId: Long,
        onParentIdChange: (Long) -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        val selectedParentName = availableParents.firstOrNull { it.id == selectedParentId }?.name
            ?: stringResource(R.string.term_parent_none)

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedParentName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    textStyle = MaterialTheme.typography.bodyMedium
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.term_parent_none)) },
                        onClick = {
                            onParentIdChange(0L)
                            expanded = false
                        }
                    )

                    availableParents.forEach { parent ->
                        DropdownMenuItem(
                            text = { Text(parent.name) },
                            onClick = {
                                onParentIdChange(parent.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    private fun showDeleteTermConfirmation(termId: Long, termName: String) {
        MaterialAlertDialogBuilder(this).also { builder ->
            builder.setTitle(R.string.term_delete_confirmation_title)
            builder.setMessage(getString(R.string.term_delete_confirmation_message, termName))
            builder.setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTerm(termId)
            }
            builder.setNegativeButton(R.string.cancel, null)
            builder.show()
        }
    }

    @Composable
    private fun EditableDetailField(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        singleLine: Boolean = true
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }

    companion object {
        private const val TAXONOMY_SLUG = "taxonomy_slug"
        private const val IS_HIERARCHICAL = "is_hierarchical"
        private const val TAXONOMY_NAME = "taxonomy_name"

        fun getIntent(context: Context, taxonomySlug: String, taxonomyName: String, isHierarchical: Boolean): Intent =
            Intent(context, TermsDataViewActivity::class.java).apply {
                putExtra(TAXONOMY_SLUG, taxonomySlug)
                putExtra(TAXONOMY_NAME, taxonomyName)
                putExtra(IS_HIERARCHICAL, isHierarchical)
            }
    }
}
