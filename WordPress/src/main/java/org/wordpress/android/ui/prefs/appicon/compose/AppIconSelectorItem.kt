package org.wordpress.android.ui.prefs.appicon.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.ui.prefs.appicon.AppIcon

@Composable
fun AppIconSelectorItem(
    appIcon: AppIcon,
    isSelected: Boolean,
    onRadioClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Image(
            bitmap = loadDrawableAsBitmap(LocalContext.current, appIcon.iconRes).asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(appIcon.displayName)
        Spacer(Modifier.weight(1f))
        RadioButton(selected = isSelected, onClick = onRadioClick)
    }
}

// TODO thomashorta, this might not be needed if we just use the legacy icons for display here
private fun loadDrawableAsBitmap(context: Context, @DrawableRes drawableRes: Int): Bitmap {
    return ResourcesCompat.getDrawable(
        context.resources,
        drawableRes,
        context.theme,
    )!!.let { drawable ->
        Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        ).apply {
            val canvas = Canvas(this)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
    }
}
