package org.wordpress.android.ui.prefs.appicon.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.ButtonSize
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.buttons.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.prefs.appicon.AppIcon
import org.wordpress.android.ui.prefs.appicon.AppIconSet

private fun Modifier.contentPadding() = padding(horizontal = 16.dp)

private fun Modifier.buttonPadding() = padding(horizontal = 20.dp)

@Composable
fun AppIconSelector(
    icons: List<AppIcon>,
    currentIcon: AppIcon,
    onDismiss: () -> Unit,
    onIconSelected: (AppIcon) -> Unit
) {
    var selectedIcon by remember { mutableStateOf(currentIcon) }

    Column(
        modifier = Modifier.padding(top = 32.dp),
    ) {
        Text(
            text = stringResource(R.string.app_icon_setting_title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.contentPadding()
        )

        Spacer(Modifier.height(10.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.app_icon_setting_selector_description),
                style = MaterialTheme.typography.body2,
                modifier = Modifier.contentPadding()
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            Modifier.weight(1f, fill = false)
        ) {
            items(icons.size) {
                val icon = icons[it]
                val onClick = { selectedIcon = icon }

                if (it > 0) {
                    Box(
                        Modifier
                            .padding(start = 88.dp)
                            .height(1.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                    )
                }

                AppIconSelectorItem(
                    appIcon = icon,
                    isSelected = icon == selectedIcon,
                    onRadioClick = onClick,
                    modifier = Modifier.clickable(onClick = onClick),
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.app_icon_setting_selector_warning),
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .contentPadding()
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(10.dp))

        PrimaryButton(
            text = stringResource(R.string.app_icon_setting_selector_button_update),
            useDefaultMargins = false,
            buttonSize = ButtonSize.LARGE,
            modifier = Modifier
                .buttonPadding()
                .fillMaxWidth(),
            onClick = { onIconSelected(selectedIcon) },
        )
        SecondaryButton(
            text = stringResource(R.string.app_icon_setting_selector_button_cancel),
            useDefaultMargins = false,
            buttonSize = ButtonSize.LARGE,
            modifier = Modifier
                .buttonPadding()
                .fillMaxWidth(),
            onClick = onDismiss
        )
    }
}

@Preview
@Composable
private fun AppIconSelectorPreview() {
    AppTheme {
        AppIconSelector(
            icons = AppIconSet().appIcons,
            currentIcon = AppIcon.DEFAULT,
            onDismiss = {},
            onIconSelected = {},
        )
    }
}
