package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

typealias NavigationIcon = @Composable () -> Unit

/**
 * Set of ready-to-use Composable functions to be used as navigation icons in the [MainTopAppBar] Composable.
 */
object NavigationIcons {
    val BackIcon: NavigationIcon = {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.navigate_up_desc),
            modifier = Modifier.graphicsLayer(
                scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f,
            ),
        )
    }

    @Suppress("unused")
    val CloseIcon: NavigationIcon = {
        Icon(
            Icons.Default.Close,
            contentDescription = stringResource(R.string.close_desc)
        )
    }
}

/**
 * TopAppBar customized according to app design specs (surface background color and no elevation).
 *
 * Extra info and workarounds regarding [TopAppBar]:
 *
 * [TopAppBar] from Material 3
 * @param title The title String to be shown in the top bar.
 * @param modifier The [Modifier] to be applied to this TopAppBar.
 * @param navigationIcon The composable to be used as navigation icon, preferably one of the default options from
 * [NavigationIcons]. It can be an composable function providing an [Icon] as well, since it is used inside an
 * [IconButton]. Note that leaving this field null will cause the navigation icon to not be shown.
 * @param elevation The elevation of this MainTopAppBar.
 * @param onNavigationIconClick The lambda to be invoked when the navigation icon is pressed.
 * @param actions The actions displayed at the end of the TopAppBar. This should typically be IconButtons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String?,
    modifier: Modifier = Modifier,
    navigationIcon: NavigationIcon? = null,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    elevation: Dp = 0.dp,
) {
    TopAppBar(
        modifier = modifier.then(
            Modifier.shadow(
                elevation = elevation,
            )
        ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = contentColor,
        ),
        title = { Text(text = title ?: "") },
        navigationIcon = {
            navigationIcon?.let {
                IconButton(onClick = onNavigationIconClick) {
                    navigationIcon()
                }
            }
        },
        actions = actions
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainTopAppBarPreview() {
    AppThemeM3 {
        MainTopAppBar(
            title = "Preview",
            navigationIcon = NavigationIcons.BackIcon,
            onNavigationIconClick = {}
        )
    }
}
