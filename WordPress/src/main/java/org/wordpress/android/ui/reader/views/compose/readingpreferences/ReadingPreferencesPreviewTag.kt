package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.R
import org.wordpress.android.ui.compose.unit.Margin

/**
 * Compose version of the "new" version of ReaderExpandableTagsView to be used in the Reading Preferences screen.
 * This looks better than trying to use the actual ReaderExpandableTagsView in Compose.
 */
@Composable
fun ReadingPreferencesPreviewTag(
    text: String,
    baseTextColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSizeMultiplier: Float = 1f,
    fontFamily: FontFamily = FontFamily.Default,
) {
    val minHeight = dimensionResource(R.dimen.reader_expandable_tags_view_chip_new_height)
    val horizontalPadding = Margin.ExtraLarge.value
    val textColor = baseTextColor.copy(alpha = ContentAlpha.medium)
    val cornerRadius = dimensionResource(R.dimen.reader_expandable_tags_view_chip_new_radius)
    val strokeAlpha = with(LocalContext.current) {
        ResourcesCompat.getFloat(resources, R.dimen.expandable_chips_chip_stroke_alpha)
    }
    val strokeColor = baseTextColor.copy(alpha = strokeAlpha)
    val strokeWidth = dimensionResource(R.dimen.reader_expandable_tags_view_chip_new_border)

    Box(
        modifier = Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
            }
            .heightIn(min = minHeight)
            .border(
                width = strokeWidth,
                color = strokeColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = textColor,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = baseTextColor,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize * fontSizeMultiplier,
                fontFamily = fontFamily,
            )
        )
    }
}
