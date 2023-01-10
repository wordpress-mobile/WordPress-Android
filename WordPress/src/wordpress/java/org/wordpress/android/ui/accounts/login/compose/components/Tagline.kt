package org.wordpress.android.ui.accounts.login.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.theme.EBGaramond

private val fontSize = 22.sp
private val lineHeight = fontSize * 1.3

@Composable
fun ColumnScope.Tagline(text: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.weight(1f),
    ) {
        Image(
            painter = painterResource(drawable.ic_wordpress_gridicon),
            contentDescription = stringResource(string.login_prologue_revamped_content_description_wordpress_icon),
            colorFilter = ColorFilter.tint(colorResource(id = color.login_prologue_revamped_icon)),
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color = colorResource(id = color.login_prologue_revamped_background))
        )
        Text(
            text,
            fontFamily = FontFamily.EBGaramond,
            fontSize = fontSize,
            lineHeight = lineHeight,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .width(234.dp)
                .padding(top = 25.dp),
        )
    }
}

@Preview(showBackground = true, heightDp = 200)
@Preview(showBackground = true, heightDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginPrologue() {
    AppTheme {
        Column(
            Modifier
                .background(color = colorResource(id = color.login_prologue_revamped_background))
        ) {
            Tagline(text = stringResource(string.login_prologue_revamped_tagline))
        }
    }
}
