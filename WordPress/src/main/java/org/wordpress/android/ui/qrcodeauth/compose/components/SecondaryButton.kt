package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SecondaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String
) {
    Button(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.onSurface)
    ) {
        Text(text = text)
    }
    //TODO
    //    <com.google.android.material.button.MaterialButton
//    android:id="@+id/error_secondary_action"
//    style="@style/QRCodeAuth.SecondaryButton"
//    android:text="@string/cancel"
//    <com.google.android.material.button.MaterialButton
//    android:id="@+id/error_secondary_action"
//    style="@style/QRCodeAuth.SecondaryButton"
//    android:text="@string/cancel"
//    app:layout_constraintBottom_toBottomOf="parent"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toBottomOf="@id/error_primary_action" />
}
