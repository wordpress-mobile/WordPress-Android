package org.wordpress.android.ui.qrcodeauth.compose.state

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.wordpress.android.ui.compose.components.ResourceImage
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun ContentState(
    @DrawableRes imageRes: Int,
    @StringRes contentDescriptionRes: Int
) {
    //    <FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
//    xmlns:tools="http://schemas.android.com/tools"
//    xmlns:app="http://schemas.android.com/apk/res-auto"
//    android:layout_width="match_parent"
//    android:layout_height="match_parent">
//
//    <androidx.constraintlayout.widget.ConstraintLayout
//    android:id="@+id/content_container"
//    android:layout_width="match_parent"
//    android:layout_height="wrap_content">
    Column(
            modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
    ) {
//    <ImageView
//    android:id="@+id/content_image"
//    android:contentDescription="@string/qrcode_auth_flow_content_description"
//    app:layout_constraintBottom_toTopOf="@+id/content_title"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toTopOf="parent"
//    tools:src="@drawable/img_illustration_qrcode_auth_validated_152dp" />
        //TODO
//    android:adjustViewBounds="true"
        ResourceImage(
                modifier = Modifier
                        .padding(
                                top = Margin.ExtraLarge.value,
                                bottom = Margin.ExtraLarge.value
                        )
                        .wrapContentHeight()
                        .wrapContentWidth(),
                imageRes = imageRes,
                contentDescription = stringResource(contentDescriptionRes)
        )

//    <com.google.android.material.textview.MaterialTextView
//    android:id="@+id/content_title"
//    style="@style/QRCodeAuth.Title"
//    app:layout_constraintBottom_toTopOf="@+id/content_subtitle"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toBottomOf="@+id/content_image"
//    tools:text="@string/qrcode_auth_flow_validated_title" />
//
//    <com.google.android.material.textview.MaterialTextView
//    android:id="@+id/content_subtitle"
//    style="@style/QRCodeAuth.Subtitle"
//    app:layout_constraintBottom_toTopOf="@+id/content_primary_action"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toBottomOf="@+id/content_title"
//    tools:text="@string/qrcode_auth_flow_validated_subtitle" />
//
//    <com.google.android.material.button.MaterialButton
//    android:id="@+id/content_primary_action"
//    style="@style/QRCodeAuth.PrimaryButton"
//    android:text="@string/qrcode_auth_flow_validated_primary_action"
//    app:layout_constraintBottom_toTopOf="@+id/content_secondary_action"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toBottomOf="@id/content_subtitle" />
//
//    <com.google.android.material.button.MaterialButton
//    android:id="@+id/content_secondary_action"
//    style="@style/QRCodeAuth.SecondaryButton"
//    android:text="@string/cancel"
//    app:layout_constraintBottom_toBottomOf="parent"
//    app:layout_constraintEnd_toEndOf="parent"
//    app:layout_constraintStart_toStartOf="parent"
//    app:layout_constraintTop_toBottomOf="@id/content_primary_action" />
//
//    </androidx.constraintlayout.widget.ConstraintLayout>
//
//    <ProgressBar
//    android:id="@+id/progress"
//    style="?android:attr/progressBarStyle"
//    android:layout_width="wrap_content"
//    android:layout_height="wrap_content"
//    android:layout_gravity="center"
//    android:indeterminate="true"
//    android:visibility="gone"
//    tools:visibility="visible" />
//    </FrameLayout>

    }
}
