package org.wordpress.android.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons

enum class DesignSystemScreen {
    Start,
    Foundation,
    Components,
    Colors,
    Fonts
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
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.brand,
            contentColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Text(stringResource(labelResourceId))
    }
}

@Preview
@Composable
fun StartDesignSystemPreview(){
    DesignSystemTheme(isSystemInDarkTheme()) {
        DesignSystem {}
    }
}
private fun getTitleForRoute(route: String): String {
    return when (route) {
        "Start" -> "Design System"
        "Foundation" -> "Foundation"
        "Components" -> "Components"
        "Colors" -> "Colors"
        "Fonts" -> "Fonts"
        else -> ""
    }
}

@Composable
fun DesignSystem(
    onBackTapped: () -> Unit
    ) {
    val navController: NavHostController = rememberNavController()
    var actionBarTitle by remember { mutableStateOf("") }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            actionBarTitle = getTitleForRoute(backStackEntry.destination.route.toString())
        }
    }
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = actionBarTitle,
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = { onBackTapped() },
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            )
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
            navController = navController,
            startDestination = DesignSystemScreen.Start.name
        ) {
            composable(route = DesignSystemScreen.Start.name) {
                DesignSystemStartScreen(
                    onButtonClicked = {
                        navController.navigate(it)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            composable(route = DesignSystemScreen.Foundation.name) {
                DesignSystemFoundationScreen(
                    onButtonClicked = {
                        navController.navigate(it)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            composable(route = DesignSystemScreen.Components.name) {
                DesignSystemComponentsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            composable(route = DesignSystemScreen.Colors.name) {
                DesignSystemColorsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            composable(route = DesignSystemScreen.Fonts.name) {
                DesignSystemFontsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
