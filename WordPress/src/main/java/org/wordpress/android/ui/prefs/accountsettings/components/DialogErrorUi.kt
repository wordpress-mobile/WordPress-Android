package org.wordpress.android.ui.prefs.accountsettings.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.ACTIVE_MEMBERSHIPS
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.ACTIVE_SUBSCRIPTIONS
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.ATOMIC_SITE
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.CHARGEBACKED_SITE
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.INVALID_TOKEN
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.UNAUTHORIZED
import org.wordpress.android.fluxc.network.rest.wpcom.account.CloseAccountResult.ErrorType.UNKNOWN
import org.wordpress.android.ui.domains.management.M3Theme

@Composable
fun DialogErrorUi(
    onDismissRequest: () -> Unit,
    onHelpRequested: () -> Unit,
    errorType: ErrorType,
) {
    val padding = 10.dp
    val messageId = when (errorType) {
        UNAUTHORIZED -> R.string.account_closure_dialog_error_unauthorized
        ATOMIC_SITE -> R.string.account_closure_dialog_error_atomic_site
        CHARGEBACKED_SITE -> R.string.account_closure_dialog_error_chargebacked_site
        ACTIVE_SUBSCRIPTIONS -> R.string.account_closure_dialog_error_active_subscriptions
        ACTIVE_MEMBERSHIPS -> R.string.account_closure_dialog_error_active_memberships
        INVALID_TOKEN, UNKNOWN -> R.string.account_closure_dialog_error_unknown
    }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = padding),
        textAlign = TextAlign.Center,
        text = stringResource(R.string.account_closure_dialog_error_title),
        fontWeight = FontWeight.Bold,
    )
    Text(stringResource(messageId))
    Spacer(Modifier.size(padding))
    FlatOutlinedButton(
        text = stringResource(R.string.dismiss),
        onClick = onDismissRequest,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            containerColor = Color.Transparent
        ),
    )
    Spacer(Modifier.size(padding))
    FlatButton(
        text = stringResource(R.string.contact_support),
        onClick = onHelpRequested,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewDialogErrorUi() {
    M3Theme {
        AccountClosureDialog(
            onDismissRequest = {},
        ) {
            DialogErrorUi(
                onDismissRequest = {},
                onHelpRequested = {},
                errorType = ATOMIC_SITE,
            )
        }
    }
}
