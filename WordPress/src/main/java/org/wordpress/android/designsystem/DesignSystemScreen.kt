package org.wordpress.android.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons

enum class DesignSystemScreen {
    Start,
    Foundation,
    Components
}

@Composable
fun SelectOptionButton(
    @StringRes labelResourceId: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
){
    Button(
        onClick = onClick,
        modifier = modifier.widthIn(min = 250.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(stringResource(labelResourceId))
    }
}

@Preview
@Composable
fun StartDesignSystemPreview(){
    DesignSystem {}
}

@Composable
fun DesignSystem(
    onBackTapped: () -> Unit
    ) {
    val navController: NavHostController = rememberNavController()
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.preference_design_system),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = { onBackTapped() },
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DesignSystemScreen.Start.name
        ) {
            composable(route = DesignSystemScreen.Start.name) {
                DesignSystemStartScreen(
                    onNextButtonClicked = {
                        navController.navigate(DesignSystemScreen.Foundation.name)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            composable(route = DesignSystemScreen.Foundation.name) {
                DesignSystemFoundationScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.button_container_shadow_height))
                )
            }
            composable(route = DesignSystemScreen.Components.name) {
                DesignSystemFoundationScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(R.dimen.button_container_shadow_height))
                )
            }
        }
    }
}
