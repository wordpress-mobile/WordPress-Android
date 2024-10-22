package org.wordpress.android.ui.mysite.personalization

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.components.buttons.WPSwitch
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.utils.LocaleAwareComposable
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.LocaleManager

@AndroidEntryPoint
class PersonalizationActivity : AppCompatActivity() {
    private val viewModel: PersonalizationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppThemeM2 {
                val language by viewModel.appLanguage.observeAsState("")

                LocaleAwareComposable(
                    locale = LocaleManager.languageLocale(language),
                ) {
                    viewModel.start()
                    PersonalizationScreen()
                }
            }
        }
        viewModel.onSelectedSiteMissing.observe(this) { finish() }
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

        var tabIndex by remember { mutableIntStateOf(0) }

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
                    Tab(text = { Text(stringResource(id = title).uppercase()) },
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
    fun ShortCutsPersonalizationContent(state: ShortcutsState, modifier: Modifier = Modifier) {
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
                val activeShortcuts = state.activeShortCuts
                if (activeShortcuts.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                            text = stringResource(id = R.string.personalization_screen_tab_shortcuts_active_shortcuts),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                        )
                    }
                    items(activeShortcuts.size) { index ->
                        val shortcutState = activeShortcuts[index]
                        ShortcutStateRow(
                            state = shortcutState,
                            actionIcon = R.drawable.ic_personalization_quick_link_remove_circle,
                            actionIconTint = Color(0xFFD63638),
                            actionButtonClick = { viewModel.removeShortcut(shortcutState) }
                        )
                    }
                }
                val inactiveShortcuts = state.inactiveShortCuts
                if (inactiveShortcuts.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                            text = stringResource(
                                id = R.string.personalization_screen_tab_shortcuts_inactive_shortcuts
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                        )
                    }

                    items(inactiveShortcuts.size) { index ->
                        val shortcutState = inactiveShortcuts[index]
                        ShortcutStateRow(
                            state = shortcutState,
                            actionIcon = R.drawable.ic_personalization_shortcuts_plus_circle,
                            actionIconTint = Color(0xFF008710),
                            actionButtonClick = { viewModel.addShortcut(shortcutState) },
                        )
                    }
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
    state: ShortcutState,
    actionIcon: Int,
    actionIconTint: Color,
    actionButtonClick: () -> Unit,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = painterResource(id = state.icon),
                contentDescription = null, // Add appropriate content description
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(24.dp)
                    .padding(1.dp),
                colorFilter = if (state.disableTint) null
                else ColorFilter.tint(MaterialTheme.colors.onSurface)

            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = uiStringText(state.label),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.high),
                modifier = Modifier
                    .padding(end = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                modifier = modifier
                    .size(24.dp)
                    .padding(1.dp),
                onClick = { actionButtonClick() }
            ) {
                Icon(
                    painter = painterResource(id = actionIcon),
                    tint = actionIconTint,
                    contentDescription = stringResource(
                        R.string.personalization_screen_shortcuts_add_or_remove_shortcut_button
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
fun PersonalizationScreenPreview() {
    AppThemeM2 {
        ShortcutStateRow(
            state = ShortcutState(
                label = UiString.UiStringRes(R.string.media),
                icon = R.drawable.media_icon_circle,
                isActive = true,
                listItemAction = ListItemAction.MEDIA
            ),
            actionIcon = R.drawable.ic_personalization_shortcuts_plus_circle,
            actionIconTint = Color(0xFF008710),
            actionButtonClick = { },
        )
    }
}

