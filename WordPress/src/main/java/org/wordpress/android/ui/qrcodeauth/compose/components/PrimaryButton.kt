package org.wordpress.android.ui.qrcodeauth.compose.components

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: String
) {
    Button(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled
    ) {
        Text(text = text)
    }
    //TODO
    //    <com.google.android.material.button.MaterialButton
//    android:id="@+id/error_primary_action"
//    style="@style/QRCodeAuth.PrimaryButton"
//    android:text="@string/scan_again"
//    <item name="android:textAllCaps">false</item>
//    <item name="android:layout_width">0dp</item>
//    <item name="android:layout_height">wrap_content</item>
//    <item name="android:layout_marginTop">@dimen/margin_small</item>
//    <item name="android:layout_marginBottom">@dimen/margin_small</item>
//    <item name="android:layout_marginEnd">@dimen/margin_extra_extra_medium_large</item>
//    <item name="android:layout_marginStart">@dimen/margin_extra_extra_medium_large</item>
}
