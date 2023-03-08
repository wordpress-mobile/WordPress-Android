package org.wordpress.android.ui.prefs.appicon.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.components.SecondaryButton
import org.wordpress.android.ui.prefs.appicon.AppIcon

@Composable
fun AppIconSelector(
    icons: List<AppIcon>,
    currentIcon: AppIcon,
    onDismiss: () -> Unit,
    onIconSelected: (AppIcon) -> Unit
) {
    var selectedIcon by remember {
        mutableStateOf(currentIcon)
    }

    Column(
        modifier = Modifier
            .padding(top = 32.dp)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Select App Icon",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            Modifier.weight(1f, fill = false)
        ) {
            items(icons.size) {
                val icon = icons[it]
                val onClick = {
                    selectedIcon = icon
                }

                AppIconSelectorItem(
                    appIcon = icon,
                    isSelected = icon == selectedIcon,
                    onRadioClick = onClick,
                    modifier = Modifier
                        .clickable(onClick = onClick)
                        .padding(vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        PrimaryButton(
            text = "Update app icon",
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (selectedIcon == currentIcon) return@PrimaryButton
                onIconSelected(selectedIcon)
            },
        )
        SecondaryButton(
            text = "Cancel",
            modifier = Modifier.fillMaxWidth(),
            onClick = onDismiss
        )
    }
}
