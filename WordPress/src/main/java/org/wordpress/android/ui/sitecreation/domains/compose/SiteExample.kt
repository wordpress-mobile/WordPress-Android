package org.wordpress.android.ui.sitecreation.domains.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2

private val grayColor @Composable get() = MaterialTheme.colors.onSurface.copy(0.05f)
private val regularFontSize = 12.sp
private val regularRadius = 4.dp
private val radius2x = regularRadius*2
@Composable
fun SiteExample() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        val domainText = stringResource(R.string.site_creation_search_domain_example_title)
        Column(
            Modifier
                .weight(1f)
                .background(
                verticalGradient(0.3f to grayColor, 1f to Color.Transparent),
                RoundedCornerShape(radius2x, radius2x),
            )
        ) {
            AddressBar(domainText.lowercase())
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, grayColor.copy(0.1f), RoundedCornerShape(regularRadius))
                .background(grayColor.copy(0.025f), RoundedCornerShape(regularRadius))
                .padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(grayColor, RoundedCornerShape(regularRadius))
                    .padding(8.dp, 4.dp)
            ) {
                Text(
                    text = domainText,
                    fontSize = regularFontSize,
                )
            }
            Text(
                text = stringResource(R.string.site_creation_search_domain_example_body),
                fontSize = regularFontSize,
            )
        }
    }
}

@Composable
private fun AddressBar(domainText: String) {
    val protocolText = stringResource(R.string.https)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .background(MaterialTheme.colors.surface, RoundedCornerShape(radius2x))
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(8.dp)
                    .size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(0.8f)
                )
            }
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(grayColor.copy(0.5f))) { append(protocolText) }
                    append(domainText)
                },
                fontSize = regularFontSize,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colors.surface, RoundedCornerShape(radius2x))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .alpha(0.8f)
            )
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 8.dp)
            .background(
                verticalGradient(0f to MaterialTheme.colors.surface, 1f to Color.Transparent),
                RoundedCornerShape(radius2x, radius2x),
            )
    )
}

@Composable
@Preview(widthDp = 415, heightDp = 900)
@Preview(widthDp = 415, heightDp = 900, name = "Dark", uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 900, heightDp = 415, name = "Landscape")
@Preview(widthDp = 415, heightDp = 900, name = "RTL", locale = "ar")
private fun SiteExamplePreview() {
    AppThemeM2 {
        SiteExample()
    }
}
