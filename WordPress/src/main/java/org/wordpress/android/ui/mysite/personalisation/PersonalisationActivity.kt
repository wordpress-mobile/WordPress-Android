package org.wordpress.android.ui.mysite.personalisation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    @Composable
    fun PersonalisationContent(cardStateList: List<DashboardCardState>) {
        Row {
            Text(text = stringResource(id = R.string.personalisation_screen_description))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(cardStateList.size) { index ->
                    val cardState = cardStateList[index]
                    DashboardCardStateRow(
                        cardState = cardState,
                        onToggle = viewModel::onToggle
                    )
                }
            }
            Text(text = stringResource(id = R.string.personalisation_screen_footer_cards))
        }
    }
}

@Composable
fun DashboardCardStateRow(
    cardState: DashboardCardState,
    onToggle: (Boolean, CardType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row () {
                Text(text = stringResource(id = cardState.title))
                cardState.description?.let {  Text(text = stringResource(id = cardState.description)) }
            }
            Switch(checked = cardState.enabled, onCheckedChange = {
                onToggle(cardState.enabled, cardState.cardType)
            })
        }
        Divider(
            thickness = 0.5.dp,
            modifier = Modifier
                .padding()
        )
    }
}
