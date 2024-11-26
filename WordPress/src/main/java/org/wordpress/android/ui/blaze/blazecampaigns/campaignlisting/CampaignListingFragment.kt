package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.ActivityNavigator
import org.wordpress.android.ui.blaze.blazecampaigns.CampaignViewModel
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.isLightTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

private const val CAMPAIGN_LISTING_PAGE_SOURCE = "campaign_listing_page_source"

@AndroidEntryPoint
class CampaignListingFragment : Fragment() {
    @Inject
    lateinit var activityNavigator: ActivityNavigator

    companion object {
        fun newInstance(campaignListingPageSource: CampaignListingPageSource) = CampaignListingFragment().apply {
            arguments = Bundle().apply {
                putSerializable(CAMPAIGN_LISTING_PAGE_SOURCE, campaignListingPageSource)
            }
        }
    }

    private val viewModel: CampaignListingViewModel by viewModels()

    private val campaignViewModel: CampaignViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM3 {
                val campaigns by viewModel.uiState.observeAsState()
                CampaignListingPage(campaigns ?: CampaignListingUiState.Loading)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start(getPageSource())
        initObservers()
    }

    private fun initObservers() {
        viewModel.navigation.observeEvent(viewLifecycleOwner) { navigation ->
            when (navigation) {
                is CampaignListingNavigation.CampaignDetailPage -> {
                    activityNavigator.navigateToCampaignDetailPage(
                        requireContext(),
                        navigation.campaignId,
                        navigation.campaignDetailPageSource
                    )
                }

                is CampaignListingNavigation.CampaignCreatePage -> {
                    activityNavigator.openPromoteWithBlaze(
                        requireContext(),
                        navigation.blazeFlowSource
                    )
                }
            }
        }
        viewModel.onSelectedSiteMissing.observe(viewLifecycleOwner) {
            requireActivity().finish()
        }
    }

    private fun getPageSource(): CampaignListingPageSource {
        return arguments?.getSerializableCompat<CampaignListingPageSource>(CAMPAIGN_LISTING_PAGE_SOURCE)
            ?: CampaignListingPageSource.UNKNOWN
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CampaignListingPage(uiState: CampaignListingUiState) {
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(R.string.blaze_campaigns_page_title))
                    },
                    navigationIcon = {
                        IconButton(onClick = { campaignViewModel.onNavigationUp() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                    },
                )
            },
            floatingActionButton = {
                if (uiState is CampaignListingUiState.Success) {
                    CreateCampaignFloatingActionButton(
                        onClick = uiState.createCampaignClick
                    )
                }
            },
        ) { contentPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
            ) {
                CampaignListingContent(uiState)
            }
        }

        LaunchedEffect(viewModel.snackBar) {
            viewModel.snackBar.collect { message ->
                if (message.isNotEmpty()) {
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    @Composable
    fun CampaignListingContent(
        uiState: CampaignListingUiState
    ) {
        when (uiState) {
            is CampaignListingUiState.Loading -> LoadingState()
            is CampaignListingUiState.Error -> CampaignListingError(uiState)
            is CampaignListingUiState.Success -> CampaignListingSuccess(uiState)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CampaignListingSuccess(uiState: CampaignListingUiState.Success) {
        val refreshState = viewModel.refresh.observeAsState()
        val pullToRefreshState = rememberPullToRefreshState()
        val listState = rememberLazyListState()

        val isScrollToEnd by remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1
            }
        }

        if (isScrollToEnd && uiState.pagingDetails.loadingNext.not()) {
            uiState.pagingDetails.loadMoreFunction()
        }

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = refreshState.value ?: false,
            onRefresh = viewModel::refreshCampaigns,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = refreshState.value ?: false,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 72.dp),
            ) {
                items(uiState.campaigns) { campaign ->
                    CampaignListRow(
                        campaignModel = campaign,
                        modifier = Modifier.clickable { uiState.itemClick(campaign) })
                }
                if (uiState.pagingDetails.loadingNext) {
                    item {
                        LoadingState(modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CampaignListingError(error: CampaignListingUiState.Error) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Text(
            text = uiStringText(uiString = error.title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = uiStringText(uiString = error.description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (error.button != null) {
            Button(
                modifier = Modifier.padding(top = 8.dp),
                onClick = error.button.click
            ) {
                Text(text = uiStringText(uiString = error.button.text))
            }
        }
    }
}

@Composable
private fun CreateCampaignFloatingActionButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isInDarkMode = !isLightTheme()
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        containerColor = if (isInDarkMode)
            AppColor.Gray30
        else MaterialTheme.colorScheme.onSurface
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(id = R.string.campaign_listing_page_create_campaign_fab_description),
            tint = if (isInDarkMode) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.surface
        )
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CampaignListingErrorPreview() {
    AppThemeM3 {
        CampaignListingError(CampaignListingUiState.Error(
            title = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_title),
            description = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_description),
            button = CampaignListingUiState.Error.ErrorButton(
                text = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_button_text),
                click = { }
            )
        ))
    }
}
