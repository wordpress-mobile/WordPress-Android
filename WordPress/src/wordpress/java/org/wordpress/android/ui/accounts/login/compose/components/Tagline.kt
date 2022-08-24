package org.wordpress.android.ui.accounts.login.compose.components

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun ColumnScope.Tagline(text: String, modifier: Modifier = Modifier) {
    Column(
            modifier = modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
    ) {
        Image(
                painter = painterResource(drawable.ic_wordpress_gridicon),
                contentDescription = stringResource(string.login_prologue_revamped_content_description_wordpress_icon),
                colorFilter = ColorFilter.tint(colorResource(id = color.login_prologue_revamped_icon)),
                modifier = Modifier
                        .size(50.dp)
        )
        Text(
                text,
                Modifier
                        .width(234.dp)
                        .padding(top = 25.dp),
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
        )
    }
}

@Preview(showBackground = true, heightDp = 200)
@Preview(showBackground = true, heightDp = 200, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoginPrologue() {
    AppTheme {
        Column {
            Tagline(text = stringResource(string.login_prologue_revamped_tagline))
        }
    }
}
