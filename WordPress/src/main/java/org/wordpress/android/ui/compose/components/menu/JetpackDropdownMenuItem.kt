package org.wordpress.android.ui.compose.components.menu

import androidx.compose.foundation.background
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import me.saket.cascade.CascadeColumnScope

@Composable
fun CascadeColumnScope.JetpackDropdownMenuItem(item: DropdownMenuItemData) {
    if (item.children.isNotEmpty()) {
        DropdownMenuItem(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            text = {
                Text(
                    style = MaterialTheme.typography.bodyLarge,
                    text = item.text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            leadingIcon = {
                if (item.leadingIcon != NO_ICON) {
                    Icon(
                        painter = painterResource(id = item.leadingIcon),
                        contentDescription = null,
                    )
                }
            },
            children = {
                item.children.forEach {
                    JetpackDropdownMenuItem(it)
                }
            },
            childrenHeader = {
                JetpackDropdownMenuHeader()
            }
        )
    } else {
        androidx.compose.material3.DropdownMenuItem(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            text = {
                Text(
                    style = MaterialTheme.typography.bodyLarge,
                    text = item.text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
            },
            leadingIcon = {
                if (item.leadingIcon != NO_ICON) {
                    Icon(
                        painter = painterResource(id = item.leadingIcon),
                        contentDescription = null,
                    )
                }
            },
            onClick = {},
        )
    }
}
