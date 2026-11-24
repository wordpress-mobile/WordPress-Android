package org.wordpress.android.ui.reader.views.compose.postcard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction
import org.wordpress.android.util.extensions.getColorFromAttribute

/**
 * One entry in a post's more menu, shared by the post list's cards and the tags feed's
 */
@Composable
internal fun ReaderPostMoreMenuItem(item: ReaderPostCardAction, onClick: (SecondaryAction) -> Unit) {
    when (item) {
        is SecondaryAction -> {
            val context = LocalContext.current
            DropdownMenuItem(
                text = {
                    Text(text = uiStringText(item.label), color = Color(context.getColorFromAttribute(item.labelColor)))
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = Color(context.getColorFromAttribute(item.iconColor)),
                    )
                },
                onClick = { onClick(item) },
            )
        }
        is ReaderPostCardAction.SpacerNoAction -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = Margin.Small.value))
        }
        is PrimaryAction -> Unit // never part of the more menu
    }
}
