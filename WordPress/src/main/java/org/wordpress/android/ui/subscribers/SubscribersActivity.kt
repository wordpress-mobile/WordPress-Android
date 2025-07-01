package org.wordpress.android.ui.subscribers

import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.DataViewScreen
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.subscribers.SubscribersViewModel.Companion.displayNameOrEmail
import uniffi.wp_api.Subscriber

@AndroidEntryPoint
class SubscribersActivity : BaseAppCompatActivity() {
    private val viewModel by viewModels<SubscribersViewModel>()
    private lateinit var composeView: ComposeView

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

    private enum class SubscriberScreen {
        List,
        Detail,
        Plan,
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavigableContent() {
        val navController = rememberNavController()
        val listTitle = stringResource(R.string.subscribers)
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
                        },
                    )
                },
            ) { contentPadding ->
                NavHost(
                    navController = navController,
                    startDestination = SubscriberScreen.List.name
                ) {
                    composable(route = SubscriberScreen.List.name) {
                        titleState.value = listTitle
                        DataViewScreen(
                            uiState = viewModel.uiState.collectAsState(),
                            items = viewModel.items.collectAsState(),
                            supportedFilters = viewModel.getSupportedFilters(),
                            currentFilter = viewModel.itemFilter.collectAsState().value,
                            supportedSorts = viewModel.getSupportedSorts(),
                            currentSort = viewModel.itemSortBy.collectAsState().value,
                            errorMessage = viewModel.errorMessage.collectAsState().value,
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
                                (item.data as? Subscriber)?.let { subscriber ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        key = KEY_USER_ID,
                                        value = subscriber.userId
                                    )
                                    navController.navigate(route = SubscriberScreen.Detail.name)
                                }
                            },
                            onFilterClick = { filter ->
                                viewModel.onFilterClick(filter)
                            },
                            onSortClick = { sort ->
                                viewModel.onSortClick(sort)
                            },
                            modifier = Modifier.padding(contentPadding)
                        )
                    }

                    composable(route = SubscriberScreen.Detail.name) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            val userId = handle.get<Long>(KEY_USER_ID)
                            if (userId != null) {
                                viewModel.getSubscriber(userId)?.let { subscriber ->
                                    titleState.value = subscriber.displayNameOrEmail()
                                    SubscriberDetailScreen(
                                        subscriber = subscriber,
                                        onEmailClick = { email ->
                                            onEmailClick(email)
                                        },
                                        onUrlClick = { url ->
                                            onUrlClick(url)
                                        },
                                        onPlanClick = { planIndex ->
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                key = KEY_USER_ID,
                                                value = userId
                                            )
                                            // plans don't have a unique id, so we use the index to identify them
                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                key = KEY_PLAN_INDEX,
                                                value = planIndex
                                            )
                                            navController.navigate(route = SubscriberScreen.Plan.name)
                                        },
                                        modifier = Modifier.padding(contentPadding),
                                        subscriberStats = viewModel.subscriberStats.collectAsState()
                                    )
                                }
                            }
                        }
                    }

                    composable(route = SubscriberScreen.Plan.name) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            val userId = handle.get<Long>(KEY_USER_ID)
                            val planIndex = handle.get<Int>(KEY_PLAN_INDEX)
                            if (userId != null && planIndex != null) {
                                viewModel.getSubscriber(userId)?.let { subscriber ->
                                    subscriber.plans?.let { plans ->
                                        if (planIndex in plans.indices) {
                                            titleState.value = plans[planIndex].title
                                            SubscriberPlanScreen(
                                                plan = plans[planIndex],
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
        }
    }

    private fun onEmailClick(email: String) {
        ActivityLauncher.openUrlExternal(this, "mailto:$email")
    }

    private fun onUrlClick(url: String) {
        ActivityLauncher.openUrlExternal(this, url)
    }

    companion object {
        private const val KEY_USER_ID = "userId"
        private const val KEY_PLAN_INDEX = "planIndex"
    }
}
