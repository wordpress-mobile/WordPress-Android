package org.wordpress.android.ui.compose.components.menu.dropdown

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.menu.dropdown.MenuElementData.Item
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.utils.UiString.UiStringText
import androidx.compose.material3.MaterialTheme as Material3Theme

@Composable
fun DropdownMenuButton(
    selectedItem: Item,
    onClick: () -> Unit,
    height: Dp = 36.dp,
    contentSizeAnimation: FiniteAnimationSpec<IntSize> = spring(),
) {
    Button(
        onClick = onClick,
        modifier = Modifier.height(height),
        elevation = ButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colors.onPrimary,
            backgroundColor = MaterialTheme.colors.onSurface,
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(
            start = Margin.MediumLarge.value,
            end = Margin.MediumLarge.value,
            top = 0.dp,
            bottom = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.animateContentSize(contentSizeAnimation),
        ) {
            if (selectedItem is Item.Single && selectedItem.leadingIcon != NO_ICON) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    painter = painterResource(id = selectedItem.leadingIcon),
                    contentDescription = null,
                )
                Spacer(Modifier.width(Margin.Small.value))
            }
            Text(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .widthIn(max = 280.dp),
                style = Material3Theme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                text = uiStringText(selectedItem.text),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
            Spacer(Modifier.width(Margin.Small.value))
            Icon(
                modifier = Modifier.align(Alignment.CenterVertically),
                painter = painterResource(id = R.drawable.ic_small_chevron_down_white_16dp),
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JetpackDropdownMenuButtonPreview() {
    AppThemeM2 {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DropdownMenuButton(
                selectedItem = Item.Single(
                    id = "text-only",
                    text = UiStringText("Text only"),
                ),
                onClick = {}
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    id = "text-and-icon",
                    text = UiStringText("Text and Icon"),
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    id = "text-with-a-really-long-text-as-the-button-label",
                    text = UiStringText("Text type with a really long text as the button label"),
                ),
                onClick = {},
            )
            DropdownMenuButton(
                selectedItem = Item.Single(
                    id = "text-with-a-really-long-text-as-the-button-label-and-icon",
                    text = UiStringText("Text type with a really long text as the button label"),
                    leadingIcon = R.drawable.ic_jetpack_logo_white_24dp,
                ),
                onClick = {},
            )
        }
    }
}
