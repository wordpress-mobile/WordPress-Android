package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.M3Theme

@Composable
fun ProgressDialog(progressDialogState: Flow<ProgressDialogState?>) {
    progressDialogState.collectAsState(initial = null).value?.let {
        ProgressDialog(progressDialogState = it)
    }
}

@Composable
fun ProgressDialog(progressDialogState: ProgressDialogState) {
    M3Theme {
        val dialogProps = DialogProperties(
            dismissOnBackPress = progressDialogState.dismissible,
            dismissOnClickOutside = progressDialogState.dismissible,
            usePlatformDefaultWidth = false
        )
        Dialog(onDismissRequest = progressDialogState.onDismiss, dialogProps) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.defaultMinSize(100.dp, 100.dp)
                ) {
                    if (progressDialogState.progress != null) {
                        CircularProgressIndicator(progress = {
                            progressDialogState.progress
                        }, Modifier.padding(16.dp))
                    } else {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                    if (progressDialogState.message != null) {
                        Text(
                            text = LocalContext.current.getString(progressDialogState.message),
                            Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (progressDialogState.showCancel) {
                        OutlinedButton(
                            onClick = progressDialogState.onDismiss,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                            modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.cancel),
                                Modifier.padding(2.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ProgressDialogState(
    @StringRes val message: Int? = null,
    val progress: Float? = null,
    val showCancel: Boolean = false,
    val dismissible: Boolean = true,
    val onDismiss: () -> Unit = {}
)

@Preview(
    name = "Light Mode",
    showBackground = true
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun ProgressDialogPreview() {
    ProgressDialog(
        progressDialogState = ProgressDialogState(
            message = R.string.uploading,
            showCancel = true,
            progress = 50f / 100f,
        )
    )
}
