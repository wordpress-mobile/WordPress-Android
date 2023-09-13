package org.wordpress.android.ui.mysite.personalisation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme

class PersonalisationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val uiItems =
                PersonalisationScreen()
            }
        }
    }

    @Composable
    fun PersonalisationScreen() {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(id = R.string.personalisation_screen_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackClick,
                )
            }
        ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 72.dp),
                    ) {


                    }
            }
        }
    }
}





