package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun IneligibleClosureDialog(
    onDismissRequest: () -> Unit,
) {
    val padding = 10.dp
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(padding))
                .background(MaterialTheme.colors.background)
                .padding(padding),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = padding),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.account_closure_dialog_title),
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(R.string.account_closure_dialog_active_purchases))
            Spacer(Modifier.size(padding))
            FlatOutlinedButton(
                text = stringResource(R.string.dismiss),
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    contentColor = MaterialTheme.colors.primary,
                    backgroundColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.size(padding))
            FlatButton(
                text = stringResource(R.string.contact_support),
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewIneligibleClosureDialog() {
    AppTheme {
        IneligibleClosureDialog(
            onDismissRequest = {},
        )
    }
}
