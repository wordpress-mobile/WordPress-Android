package org.wordpress.android.ui.compose.components.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.saket.cascade.CascadeColumnScope
import org.wordpress.android.R

@Composable
fun CascadeColumnScope.JetpackDropdownMenuHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.5.dp),
    text: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .clickable(enabled = hasParentMenu, role = Role.Button) {
                if (!isNavigationRunning) { // Prevent accidental double clicks.
                    cascadeState.navigateBack()
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val headerColor = LocalContentColor.current.copy(alpha = 0.6f)
        val headerStyle = MaterialTheme.typography.labelLarge.run { // Same style as DropdownMenuItem().
            copy(
                fontSize = fontSize * 0.9f,
                letterSpacing = letterSpacing * 0.9f
            )
        }
        CompositionLocalProvider(
            LocalContentColor provides headerColor,
            LocalTextStyle provides headerStyle
        ) {
            if (this@JetpackDropdownMenuHeader.hasParentMenu) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_left_white_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(headerColor),
                )
            }
            Box(Modifier.weight(1f)) {
                text?.invoke()
            }
        }
    }
}
