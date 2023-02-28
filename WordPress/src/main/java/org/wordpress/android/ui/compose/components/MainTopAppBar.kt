package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.withFullContentAlpha

typealias NavigationIcon = @Composable () -> Unit

/**
 * Set of ready-to-use Composable functions to be used as navigation icons in the [MainTopAppBar] Composable.
 */
object NavigationIcons {
    val BackIcon: NavigationIcon = {
        Icon(
            Icons.Default.ArrowBack,
            contentDescription = stringResource(R.string.navigate_up_desc)
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
 * [TopAppBar] from Material internally provide a different [LocalContentAlpha] according to low vs high
 * contrast use cases. This forces the navigation icon, title, and actions "Composables" to use that alpha.
 * In addition to that, most people use the default phone settings, which is considered LOW CONTRAST, making
 * the provided alpha be smaller than 1f, which makes the components mentioned above have some transparency, which
 * is not aligned with our Design Specs.
 *
 * This Composable works around that by setting LocalContentAlpha in the appropriate parts of the code using the helper
 * [withFullContentAlpha] function, which provides a full alpha to the [LocalContentAlpha] Composition Local.
 *
 * @param title The title String to be shown in the top bar.
 * @param modifier The [Modifier] to be applied to this TopAppBar.
 * @param navigationIcon The composable to be used as navigation icon, preferably one of the default options from
 * [NavigationIcons]. It can be an composable function providing an [Icon] as well, since it is used inside an
 * [IconButton]. Note that leaving this field null will cause the navigation icon to not be shown.
 * @param onNavigationIconClick The lambda to be invoked when the navigation icon is pressed.
 * @param actions The actions displayed at the end of the TopAppBar. This should typically be IconButtons
 */
@Composable
fun MainTopAppBar(
    title: String?,
    modifier: Modifier = Modifier,
    navigationIcon: NavigationIcon? = null,
    onNavigationIconClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.onSurface,
        elevation = 0.dp,
        title = title?.let {
            withFullContentAlpha {
                Text(title)
            }
        } ?: { },
        navigationIcon = navigationIcon?.let {
            withFullContentAlpha {
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
    AppTheme {
        MainTopAppBar(
            title = "Preview",
            navigationIcon = NavigationIcons.BackIcon,
            onNavigationIconClick = {}
        )
    }
}
