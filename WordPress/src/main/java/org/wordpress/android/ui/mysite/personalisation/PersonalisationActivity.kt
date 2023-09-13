package org.wordpress.android.ui.mysite.personalisation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme

class PersonalisationActivity : ComponentActivity() {
    private val viewModel: PersonalisationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                PersonalisationScreen(viewModel.uiState)
            }
        }
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun PersonalisationScreen(uiState: LiveData<List<DashboardCardState>>) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.personalisation_screen_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackPressedDispatcher::onBackPressed,
                )
            },
            content = {
                PersonalisationContent(uiState.value ?: emptyList())
            }
        )
    }
}


@Composable
fun PersonalisationContent(cardStateList: List<DashboardCardState>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp),
            text = stringResource(id = R.string.personalisation_screen_description),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0x99000000),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            items(cardStateList.size) { index ->
                val cardState = cardStateList[index]
                DashboardCardStateRow(
                    cardState = cardState,
                )
            }
        }
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp),
            text = stringResource(id = R.string.personalisation_screen_footer_cards),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666)
        )
    }
}

@Composable
fun DashboardCardStateRow(
    cardState: DashboardCardState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp,
                    end = 16.dp),
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
                    color = Color(0x99000000)
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = cardState.enabled,
                onCheckedChange = {},
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


@Preview
@Composable
fun PreviewPersonalizeScreen() {
    AppTheme {
        PersonalisationContent(
            cardStateList = listOf(
                DashboardCardState(
                    title = R.string.personalisation_screen_stats_card_title,
                    description = R.string.personalisation_screen_stats_card_description,
                    enabled = true,
                    cardType = CardType.STATS
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_draft_posts_card_title,
                    description = R.string.personalisation_screen_draft_posts_card_description,
                    enabled = true,
                    cardType = CardType.DRAFT_POSTS
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_scheduled_posts_card_title,
                    description = R.string.personalisation_screen_scheduled_posts_card_description,
                    enabled = true,
                    cardType = CardType.SCHEDULED_POSTS
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_pages_card_title,
                    description = R.string.personalisation_screen_pages_card_description,
                    enabled = true,
                    cardType = CardType.PAGES
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_activity_log_card_title,
                    description = R.string.personalisation_screen_activity_log_card_description,
                    enabled = true,
                    cardType = CardType.ACTIVITY_LOG
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_blaze_card_title,
                    description = R.string.personalisation_screen_blaze_card_description,
                    enabled = true,
                    cardType = CardType.BLAZE
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_blogging_prompts_card_title,
                    description = R.string.personalisation_screen_blogging_prompts_card_description,
                    enabled = true,
                    cardType = CardType.BLOGGING_PROMPTS
                ),
                DashboardCardState(
                    title = R.string.personalisation_screen_next_steps_card_title,
                    description = R.string.personalisation_screen_next_steps_card_description,
                    enabled = true,
                    cardType = CardType.NEXT_STEPS
                ),
            )
        )
    }
}
