package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import android.annotation.SuppressLint
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
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2
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
            AppThemeM2 {
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

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun CampaignListingPage(uiState: CampaignListingUiState) {
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { snackbarHostState ->
                // SnackbarHost needs to be provided to show Snackbars
                SnackbarHost(hostState = snackbarHostState)
            },
            topBar = {
                MainTopAppBar(
                    title = stringResource(R.string.blaze_campaigns_page_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = {
                        campaignViewModel.onNavigationUp()
                    }
                )
            },
            floatingActionButton = {
                if (uiState is CampaignListingUiState.Success) {
                    CreateCampaignFloatingActionButton(
                        onClick = uiState.createCampaignClick
                    )
                }
            },
        ) { CampaignListingContent(uiState) }

        LaunchedEffect(viewModel.snackBar) {
            viewModel.snackBar.collect { message ->
                if (message.isNotEmpty()) {
                    scaffoldState.snackbarHostState.showSnackbar(message)
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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun CampaignListingSuccess(uiState: CampaignListingUiState.Success) {
        val refreshState = viewModel.refresh.observeAsState()

        val listState = rememberLazyListState()

        val isScrollToEnd by remember {
            derivedStateOf {
                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index == listState.layoutInfo.totalItemsCount - 1
            }
        }

        if (isScrollToEnd && uiState.pagingDetails.loadingNext.not()) {
            uiState.pagingDetails.loadMoreFunction()
        }

        val pullRefreshState = rememberPullRefreshState(
            refreshing = refreshState.value ?: false,
            onRefresh = viewModel::refreshCampaigns
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
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

            PullRefreshIndicator(
                refreshing = refreshState.value ?: false,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colors.primaryVariant,
            )
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
            style = MaterialTheme.typography.h5,
        )
        Text(
            text = uiStringText(uiString = error.description),
            style = MaterialTheme.typography.body1,
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

@Preview
@Composable
fun CampaignListingErrorPreview() {
    AppThemeM2 {
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

@Composable
private fun CreateCampaignFloatingActionButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isInDarkMode = !isLightTheme()
    FloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        containerColor = if (isInDarkMode)
            AppColor.Gray30
        else MaterialTheme.colors.onSurface
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = stringResource(id = R.string.campaign_listing_page_create_campaign_fab_description),
            tint = if (isInDarkMode) MaterialTheme.colors.onSurface
            else MaterialTheme.colors.surface
        )
    }
}

