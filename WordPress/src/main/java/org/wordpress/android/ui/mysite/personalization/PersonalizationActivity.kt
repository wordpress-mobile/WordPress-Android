package org.wordpress.android.ui.mysite.personalization

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.components.buttons.WPSwitch
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText

@AndroidEntryPoint
class PersonalizationActivity : ComponentActivity() {
    private val viewModel: PersonalizationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                viewModel.start()
                PersonalizationScreen()
            }
        }
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun PersonalizationScreen(modifier: Modifier = Modifier) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.personalization_screen_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                TabScreen(modifier = modifier)
            }
        )
    }

    @Composable
    fun TabScreen(modifier: Modifier = Modifier) {
        val dashboardCardStates = viewModel.uiState.observeAsState()
        val shortcutsStates = viewModel.shortcutsState.collectAsState()

        var tabIndex by remember { mutableStateOf(0) }

        val tabs = listOf(
            R.string.personalization_screen_cards_tab_title,
            R.string.personalization_screen_shortcuts_tab_title
        )

        Column(modifier = modifier.fillMaxWidth()) {
            TabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(stringResource(id = title)) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
            when (tabIndex) {
                0 -> CardsPersonalizationContent(dashboardCardStates.value ?: emptyList())
                1 -> ShortCutsPersonalizationContent(shortcutsStates.value)
            }
        }
    }

    @Composable
    fun CardsPersonalizationContent(cardStateList: List<DashboardCardState>, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                item {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.personalization_screen_tab_cards_description),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                    )
                }
                items(cardStateList.size) { index ->
                    val cardState = cardStateList[index]
                    DashboardCardStateRow(
                        cardState = cardState,
                        viewModel::onCardToggled
                    )
                }

                item {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(id = R.string.personalization_screen_tab_cards_footer_cards),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                }
            }
        }
    }

    @Composable
    fun ShortCutsPersonalizationContent(cardStateList: List<ShortcutsState>, modifier: Modifier = Modifier) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentSize()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                item {
                    Text(
                        modifier = Modifier.padding(start = 16.dp),
                        text = stringResource(id = R.string.personalization_screen_tab_shortcuts_active_shortcuts),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                    )
                }
                items(cardStateList.size) { index ->
                    val cardState = cardStateList[index]
                    ShortcutStateRow(
                        state = cardState,
                    )
                }

                item {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                        text = stringResource(id = R.string.personalization_screen_tab_shortcuts_inactive_shortcuts),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCardStateRow(
    cardState: DashboardCardState,
    onCardToggled: (cardType: CardType, enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth(.9f)
            ) {
                Text(
                    text = stringResource(id = cardState.title),
                    fontSize = 16.sp
                )
                Text(
                    text = stringResource(id = cardState.description),
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }
            Spacer(Modifier.width(8.dp))
            WPSwitch(
                checked = cardState.enabled,
                onCheckedChange = {
                    onCardToggled(cardState.cardType, it)
                },
                modifier = Modifier
                    .weight(.1f)
            )
        }
        Divider(
            thickness = 0.5.dp,
            modifier = Modifier
                .padding()
        )
    }
}

@Composable
fun ShortcutStateRow(
    state: ShortcutsState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
    )
    {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(size = 10.dp)
                )
                .padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = state.icon),
                contentDescription = null, // Add appropriate content description
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 8.dp)
            )
            Text(
                text = uiStringText(state.label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
    }
}
