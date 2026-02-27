package org.wordpress.android.ui.newstats.subscribers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.CardPosition
import org.wordpress.android.ui.newstats.subscribers.alltimestats.AllTimeSubscribersCard
import org.wordpress.android.ui.newstats.subscribers.alltimestats.AllTimeSubscribersViewModel
import org.wordpress.android.ui.newstats.subscribers.emails.EmailsCard
import org.wordpress.android.ui.newstats.subscribers.emails.EmailsCardViewModel
import org.wordpress.android.ui.newstats.subscribers.emails.EmailsDetailActivity
import org.wordpress.android.ui.newstats.subscribers.subscribersgraph.SubscribersGraphCard
import org.wordpress.android.ui.newstats.subscribers.subscribersgraph.SubscribersGraphViewModel
import org.wordpress.android.ui.newstats.subscribers.subscriberslist.SubscribersListCard
import org.wordpress.android.ui.newstats.subscribers.subscriberslist.SubscribersListDetailActivity
import org.wordpress.android.ui.newstats.subscribers.subscriberslist.SubscribersListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
fun SubscribersTabContent(
    allTimeViewModel: AllTimeSubscribersViewModel =
        viewModel(),
    graphViewModel: SubscribersGraphViewModel =
        viewModel(),
    subscribersListViewModel: SubscribersListViewModel =
        viewModel(),
    emailsViewModel: EmailsCardViewModel = viewModel(),
    subscribersTabViewModel: SubscribersTabViewModel =
        viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allTimeUiState by
        allTimeViewModel.uiState.collectAsState()
    val graphUiState by
        graphViewModel.uiState.collectAsState()
    val graphSelectedTab by
        graphViewModel.selectedTab.collectAsState()
    val subscribersListUiState by
        subscribersListViewModel.uiState.collectAsState()
    val emailsUiState by
        emailsViewModel.uiState.collectAsState()

    val isAllTimeRefreshing by
        allTimeViewModel.isRefreshing.collectAsState()
    val isGraphRefreshing by
        graphViewModel.isRefreshing.collectAsState()
    val isSubscribersListRefreshing by
        subscribersListViewModel
            .isRefreshing.collectAsState()
    val isEmailsRefreshing by
        emailsViewModel.isRefreshing.collectAsState()
    val isRefreshing = listOf(
        isAllTimeRefreshing,
        isGraphRefreshing,
        isSubscribersListRefreshing,
        isEmailsRefreshing
    ).any { it }
    val pullToRefreshState = rememberPullToRefreshState()

    val visibleCards by
        subscribersTabViewModel
            .visibleCards.collectAsState()
    val hiddenCards by
        subscribersTabViewModel
            .hiddenCards.collectAsState()
    val isNetworkAvailable by
        subscribersTabViewModel
            .isNetworkAvailable.collectAsState()
    val cardsToLoad by
        subscribersTabViewModel
            .cardsToLoad.collectAsState()
    var showAddCardSheet by
        remember { mutableStateOf(false) }
    val addCardSheetState =
        rememberModalBottomSheetState()

    LaunchedEffect(cardsToLoad) {
        cardsToLoad.dispatchToVisibleCards(
            onAllTimeStats = {
                allTimeViewModel.loadDataIfNeeded()
            },
            onGraph = {
                graphViewModel.loadDataIfNeeded()
            },
            onSubscribersList = {
                subscribersListViewModel
                    .loadDataIfNeeded()
            },
            onEmails = {
                emailsViewModel.loadDataIfNeeded()
            }
        )
    }

    if (showAddCardSheet) {
        AddSubscribersCardBottomSheet(
            sheetState = addCardSheetState,
            availableCards = hiddenCards,
            onDismiss = { showAddCardSheet = false },
            onCardSelected = { cardType ->
                subscribersTabViewModel.addCard(cardType)
            }
        )
    }

    var showNoConnectionScreen by
        remember { mutableStateOf(!isNetworkAvailable) }

    val loadVisibleCards = {
        visibleCards.dispatchToVisibleCards(
            onAllTimeStats = {
                allTimeViewModel.loadData()
            },
            onGraph = { graphViewModel.loadData() },
            onSubscribersList = {
                subscribersListViewModel.loadData()
            },
            onEmails = { emailsViewModel.loadData() }
        )
    }

    LaunchedEffect(isNetworkAvailable) {
        if (isNetworkAvailable &&
            showNoConnectionScreen
        ) {
            showNoConnectionScreen = false
            loadVisibleCards()
        } else if (!isNetworkAvailable &&
            !showNoConnectionScreen
        ) {
            showNoConnectionScreen = true
        }
    }

    if (showNoConnectionScreen) {
        NoConnectionContent(
            onRetry = {
                val isAvailable =
                    subscribersTabViewModel
                        .checkNetworkStatus()
                if (isAvailable) {
                    showNoConnectionScreen = false
                    loadVisibleCards()
                }
            }
        )
        return
    }

    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            subscribersTabViewModel.checkNetworkStatus()
            visibleCards.dispatchToVisibleCards(
                onAllTimeStats = {
                    allTimeViewModel.refresh()
                },
                onGraph = {
                    graphViewModel.refresh()
                },
                onSubscribersList = {
                    subscribersListViewModel.refresh()
                },
                onEmails = {
                    emailsViewModel.refresh()
                }
            )
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                color = MaterialTheme
                    .colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopCenter)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (visibleCards.isEmpty()) {
                val emptyMsg = stringResource(
                    R.string.stats_no_cards_message
                )
                Text(
                    text = emptyMsg,
                    style = MaterialTheme
                        .typography.bodyLarge,
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .semantics {
                            contentDescription = emptyMsg
                        },
                    textAlign = TextAlign.Center
                )
            }

            val cardPositions =
                remember(visibleCards) {
                    visibleCards.mapIndexed { idx, _ ->
                        CardPosition(
                            index = idx,
                            totalCards =
                                visibleCards.size
                        )
                    }
                }

            visibleCards.forEachIndexed { index,
                cardType ->
                val cardPosition =
                    cardPositions[index]
                when (cardType) {
                    SubscribersCardType
                        .ALL_TIME_SUBSCRIBERS ->
                        AllTimeSubscribersCard(
                            uiState = allTimeUiState,
                            onRetry = {
                                allTimeViewModel
                                    .loadData()
                            },
                            onRemoveCard = {
                                subscribersTabViewModel
                                    .removeCard(cardType)
                            },
                            cardPosition = cardPosition,
                            onMoveUp = {
                                subscribersTabViewModel
                                    .moveCardUp(cardType)
                            },
                            onMoveToTop = {
                                subscribersTabViewModel
                                    .moveCardToTop(
                                        cardType
                                    )
                            },
                            onMoveDown = {
                                subscribersTabViewModel
                                    .moveCardDown(
                                        cardType
                                    )
                            },
                            onMoveToBottom = {
                                subscribersTabViewModel
                                    .moveCardToBottom(
                                        cardType
                                    )
                            }
                        )

                    SubscribersCardType
                        .SUBSCRIBERS_GRAPH ->
                        SubscribersGraphCard(
                            uiState = graphUiState,
                            selectedTab =
                                graphSelectedTab,
                            onTabSelected = {
                                graphViewModel
                                    .onTabSelected(it)
                            },
                            onRetry = {
                                graphViewModel.loadData()
                            },
                            onRemoveCard = {
                                subscribersTabViewModel
                                    .removeCard(cardType)
                            },
                            cardPosition = cardPosition,
                            onMoveUp = {
                                subscribersTabViewModel
                                    .moveCardUp(cardType)
                            },
                            onMoveToTop = {
                                subscribersTabViewModel
                                    .moveCardToTop(
                                        cardType
                                    )
                            },
                            onMoveDown = {
                                subscribersTabViewModel
                                    .moveCardDown(
                                        cardType
                                    )
                            },
                            onMoveToBottom = {
                                subscribersTabViewModel
                                    .moveCardToBottom(
                                        cardType
                                    )
                            }
                        )

                    SubscribersCardType
                        .SUBSCRIBERS_LIST ->
                        SubscribersListCard(
                            uiState =
                                subscribersListUiState,
                            onShowAllClick = {
                                scope.launch {
                                    val items =
                                        subscribersListViewModel
                                            .getDetailData()
                                    SubscribersListDetailActivity
                                        .start(context, items)
                                }
                            },
                            onRetry = {
                                subscribersListViewModel
                                    .loadData()
                            },
                            onRemoveCard = {
                                subscribersTabViewModel
                                    .removeCard(cardType)
                            },
                            cardPosition = cardPosition,
                            onMoveUp = {
                                subscribersTabViewModel
                                    .moveCardUp(cardType)
                            },
                            onMoveToTop = {
                                subscribersTabViewModel
                                    .moveCardToTop(
                                        cardType
                                    )
                            },
                            onMoveDown = {
                                subscribersTabViewModel
                                    .moveCardDown(
                                        cardType
                                    )
                            },
                            onMoveToBottom = {
                                subscribersTabViewModel
                                    .moveCardToBottom(
                                        cardType
                                    )
                            }
                        )

                    SubscribersCardType.EMAILS ->
                        EmailsCard(
                            uiState = emailsUiState,
                            onShowAllClick = {
                                scope.launch {
                                    val items =
                                        emailsViewModel
                                            .getDetailData()
                                    EmailsDetailActivity
                                        .start(
                                            context, items
                                        )
                                }
                            },
                            onRetry = {
                                emailsViewModel
                                    .loadData()
                            },
                            onRemoveCard = {
                                subscribersTabViewModel
                                    .removeCard(cardType)
                            },
                            cardPosition = cardPosition,
                            onMoveUp = {
                                subscribersTabViewModel
                                    .moveCardUp(cardType)
                            },
                            onMoveToTop = {
                                subscribersTabViewModel
                                    .moveCardToTop(
                                        cardType
                                    )
                            },
                            onMoveDown = {
                                subscribersTabViewModel
                                    .moveCardDown(
                                        cardType
                                    )
                            },
                            onMoveToBottom = {
                                subscribersTabViewModel
                                    .moveCardToBottom(
                                        cardType
                                    )
                            }
                        )
                }
            }

            // Add Card Button
            OutlinedButton(
                onClick = { showAddCardSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(
                        R.string.stats_add_card_title
                    )
                )
            }
        }
    }
}

private fun List<SubscribersCardType>
    .dispatchToVisibleCards(
    onAllTimeStats: () -> Unit,
    onGraph: () -> Unit,
    onSubscribersList: () -> Unit,
    onEmails: () -> Unit
) {
    if (SubscribersCardType.ALL_TIME_SUBSCRIBERS in this)
        onAllTimeStats()
    if (SubscribersCardType.SUBSCRIBERS_GRAPH in this)
        onGraph()
    if (SubscribersCardType.SUBSCRIBERS_LIST in this)
        onSubscribersList()
    if (SubscribersCardType.EMAILS in this)
        onEmails()
}

@Composable
private fun NoConnectionContent(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(
                    R.drawable.ic_wifi_off_24px
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme
                            .colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
                    .padding(12.dp),
                tint = MaterialTheme
                    .colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(
                    R.string.no_connection_error_title
                ),
                style = MaterialTheme
                    .typography.titleMedium,
                color = MaterialTheme
                    .colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string
                        .no_connection_error_description
                ),
                style = MaterialTheme
                    .typography.bodyMedium,
                color = MaterialTheme
                    .colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
