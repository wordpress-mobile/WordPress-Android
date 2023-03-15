package org.wordpress.android.ui.prefs.appicon.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.prefs.appicon.AppIcon

@Composable
fun AppIconSelectorItem(
    appIcon: AppIcon,
    isSelected: Boolean,
    onRadioClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        val a = rememberImagePainter(appIcon.iconRes) {
            placeholder(R.color.gray_0)
        }
        Image(
            painter = a,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(56.dp)
        )
        Text(
            text = stringResource(appIcon.nameRes),
            style = MaterialTheme.typography.body1,
        )
        Spacer(Modifier.weight(1f))
        RadioButton(
            selected = isSelected,
            onClick = onRadioClick,
            modifier = Modifier.padding(end = 4.dp)
        )
    }
}

@Preview(name = "Not selected - Light Mode")
@Preview(name = "Not selected - Night Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AppIconSelectorItemPreview() {
    AppTheme {
        AppIconSelectorItem(
            appIcon = AppIcon.DEFAULT,
            isSelected = false,
            onRadioClick = { },
        )
    }
}

@Preview(name = "Selected - Light Mode")
@Preview(name = "Selected - Night Mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun AppIconSelectorItemSelectedPreview() {
    AppTheme {
        AppIconSelectorItem(
            appIcon = AppIcon.DEFAULT,
            isSelected = true,
            onRadioClick = { },
        )
    }
}
