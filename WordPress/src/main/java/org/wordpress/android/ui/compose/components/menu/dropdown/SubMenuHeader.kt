package org.wordpress.android.ui.compose.components.menu.dropdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
fun CascadeColumnScope.JetpackDropdownSubMenuHeader(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.5.dp),
    text: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .clickable(enabled = hasParentMenu, role = Role.Button) {
                if (!isNavigationRunning) {
                    cascadeState.navigateBack()
                }
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides MaterialTheme.typography.labelLarge
        ) {
            if (this@JetpackDropdownSubMenuHeader.hasParentMenu) {
                Image(
                    painter = painterResource(R.drawable.ic_arrow_left_white_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
                )
            }
            Box(Modifier.weight(1f)) {
                text?.invoke()
            }
        }
    }
}
