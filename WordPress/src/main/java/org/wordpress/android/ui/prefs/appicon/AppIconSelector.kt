package org.wordpress.android.ui.prefs.appicon

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import org.wordpress.android.ui.compose.components.PrimaryButton
import org.wordpress.android.ui.compose.components.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppTheme

// Extension for Activity
fun Activity.showAsBottomSheet(content: @Composable BottomSheetScope.() -> Unit) {
    val viewGroup = this.findViewById(android.R.id.content) as ViewGroup
    addContentToView(viewGroup, content)
}

// Extension for Fragment
fun Fragment.showAsBottomSheet(content: @Composable BottomSheetScope.() -> Unit) {
    val viewGroup = requireActivity().findViewById(android.R.id.content) as ViewGroup
    addContentToView(viewGroup, content)
}

// Helper method
private fun addContentToView(
    viewGroup: ViewGroup,
    content: @Composable BottomSheetScope.() -> Unit
) {
    viewGroup.addView(
        ComposeView(viewGroup.context).apply {
            setContent {
                BottomSheetWrapper(viewGroup, this, content)
            }
        }
    )
}

interface BottomSheetScope {
    fun hideBottomSheet()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetWrapper(
    parent: ViewGroup,
    composeView: ComposeView,
    content: @Composable BottomSheetScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    var isSheetOpened by remember { mutableStateOf(false) }

    val bottomSheetScope = object : BottomSheetScope {
        override fun hideBottomSheet() {
            coroutineScope.launch {
                modalBottomSheetState.hide()
            }
        }
    }

    ModalBottomSheetLayout(
        sheetShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        sheetState = modalBottomSheetState,
        sheetContent = {
            bottomSheetScope.content()
        },
    ) {}

    BackHandler {
        bottomSheetScope.hideBottomSheet()
    }

    // Take action based on hidden state
    LaunchedEffect(modalBottomSheetState.currentValue) {
        when (modalBottomSheetState.currentValue) {
            ModalBottomSheetValue.Hidden -> {
                when {
                    isSheetOpened -> parent.removeView(composeView)
                    else -> {
                        isSheetOpened = true
                        modalBottomSheetState.show()
                    }
                }
            }

            else -> { /* do nothing */
            }
        }
    }
}

object AppIconSelector {
    @JvmStatic
    fun showBottomSheet(activity: Activity, appIconHelper: AppIconHelper, callback: Callback) {
        activity.showAsBottomSheet {
            val icons = appIconHelper.appIcons
            val currentIcon = appIconHelper.getCurrentIcon()
            var selectedIcon by remember {
                mutableStateOf(currentIcon)
            }

            AppTheme {
                Column(
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "Select App Icon",
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        Modifier.weight(1f, fill = false)
                    ) {
                        items(icons.size) {
                            val icon = icons[it]
                            val onClick = {
                                selectedIcon = icon
                            }

                            AppIconSelectorItem(
                                appIcon = icon,
                                isSelected = icon == selectedIcon,
                                onRadioClick = onClick,
                                modifier = Modifier
                                    .clickable(onClick = onClick)
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    PrimaryButton(
                        text = "Update app icon",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (selectedIcon == currentIcon) return@PrimaryButton
                            callback.onAppIconSelected(selectedIcon)
                            hideBottomSheet()
                        },
                    )
                    SecondaryButton(
                        text = "Cancel",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = ::hideBottomSheet
                    )
                }
            }
        }
    }

    @Composable
    private fun AppIconSelectorItem(
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

    interface Callback {
        fun onAppIconSelected(appIcon: AppIcon)
    }
}
