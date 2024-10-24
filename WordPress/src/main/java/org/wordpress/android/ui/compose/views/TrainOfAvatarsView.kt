package org.wordpress.android.ui.compose.views

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import androidx.annotation.DimenRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import org.wordpress.android.R
import org.wordpress.android.ui.avatars.TrainOfAvatarsItem.AvatarItem
import org.wordpress.android.ui.compose.components.TrainOfIcons
import org.wordpress.android.ui.compose.components.TrainOfIconsModel
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.WPAvatarUtils

@Suppress("MemberVisibilityCanBePrivate", "unused")
class TrainOfAvatarsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private val avatarsState: MutableState<List<AvatarItem>> = mutableStateOf(emptyList())

    // icon size in DP used by the Composable
    private val iconSizeState: MutableState<Int> = mutableIntStateOf(DEFAULT_ICON_SIZE_DP)

    // icon border width in DP used by the Composable
    private val iconBorderWidthState: MutableState<Int> = mutableIntStateOf(DEFAULT_ICON_BORDER_WIDTH_DP)

    // placeholder drawable/color resource to be used by the Composable
    private val placeholderResState: MutableState<Int> = mutableIntStateOf(R.color.placeholder)

    var avatars: List<AvatarItem>
        get() = avatarsState.value
        set(value) {
            if (avatarsState.value != value) avatarsState.value = value
        }

    /**
     * Icon size in pixels
     */
    var iconSize: Int
        get() = DisplayUtils.dpToPx(context, iconSizeState.value)
        set(value) {
            val iconSizeDp = DisplayUtils.pxToDp(context, value)
            if (iconSizeState.value != iconSizeDp) iconSizeState.value = iconSizeDp
        }

    /**
     * Icon border width in pixels
     */
    var iconBorderWidth: Int
        get() = DisplayUtils.dpToPx(context, iconBorderWidthState.value)
        set(value) {
            val borderWidthDp = DisplayUtils.pxToDp(context, value)
            if (iconBorderWidthState.value != borderWidthDp) iconBorderWidthState.value = borderWidthDp
        }

    var placeholderRes: Int
        get() = placeholderResState.value
        set(value) {
            if (placeholderResState.value != value) placeholderResState.value = value
        }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.TrainOfAvatarsView)
        ta.getDimensionPixelSize(R.styleable.TrainOfAvatarsView_iconSize, -1)
            .takeIf { it != -1 }
            ?.let { iconSize = it }
        ta.getDimensionPixelSize(R.styleable.TrainOfAvatarsView_iconBorderWidth, -1)
            .takeIf { it != -1 }
            ?.let { iconBorderWidth = it }
        ta.getResourceId(R.styleable.TrainOfAvatarsView_placeholder, -1)
            .takeIf { it != -1 }
            ?.let { placeholderRes = it }
        ta.recycle()
    }

    @Composable
    override fun Content() {
        if (avatarsState.value.isEmpty()) return

        AppThemeM2 {
            TrainOfIcons(
                iconModels = avatarModels(),
                iconSize = iconSizeState.value.dp,
                iconBorderWidth = iconBorderWidthState.value.dp,
                placeholderPainter = placeholderPainter()
            )
        }
    }

    fun setIconSizeRes(@DimenRes sizeRes: Int) {
        val newIconSize = context.resources.getDimensionPixelSize(sizeRes)
        if (newIconSize != iconSize) iconSize = newIconSize
    }

    fun setIconBorderWidthRes(@DimenRes sizeRes: Int) {
        val newIconBorderWidth = context.resources.getDimensionPixelSize(sizeRes)
        if (newIconBorderWidth != iconBorderWidth) iconBorderWidth = newIconBorderWidth
    }

    // returning null for a model will cause the Composable to render a placeholder
    private fun avatarModels(): List<TrainOfIconsModel> = avatarsState.value
        .map { WPAvatarUtils.rewriteAvatarUrl(it.userAvatarUrl, iconSize) }
        .map {
            TrainOfIconsModel(
                it.takeIf { gravatarUrl -> gravatarUrl.isNotEmpty() }
            )
        }

    @Composable
    private fun placeholderPainter(): Painter {
        val resId = placeholderResState.value

        val drawable = AppCompatResources.getDrawable(context, resId)
        return if (drawable is ColorDrawable) {
            ColorPainter(colorResource(resId))
        } else if (drawable != null) {
            // use this workaround to support basically any drawable
            // from: https://slack-chats.kotlinlang.org/t/506477/hello-i-am-trying-to-load-a-layer-list-drawable-with-this-co#707c4aef-021c-421b-b873-ea7ca453b61e
            val imageBitmap = drawable.toBitmap().asImageBitmap()
            BitmapPainter(imageBitmap)
        } else {
            ColorPainter(colorResource(R.color.placeholder))
        }
    }

    companion object {
        private const val DEFAULT_ICON_SIZE_DP = 32
        private const val DEFAULT_ICON_BORDER_WIDTH_DP = 2
    }
}
