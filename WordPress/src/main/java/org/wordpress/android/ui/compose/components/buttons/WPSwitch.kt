package org.wordpress.android.ui.compose.components.buttons

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

/**
 * A switch that by default uses the same colors as the SwitchCompat from the App Compat libraries, so there are no
 * differences when using this component next to Android View-based switch views.
 *
 * The default colors are available through [WPSwitchDefaults.colors], in case you want to use them as a base for
 * your own custom colors. This [WPSwitch] is just a helper to avoid having to pass the same colors every time.
 *
 * @param checked whether or not this component is checked
 * @param onCheckedChange callback to be invoked when Switch is being clicked,
 * therefore the change of checked state is requested.  If null, then this is passive
 * and relies entirely on a higher-level component to control the "checked" state.
 * @param modifier Modifier to be applied to the switch layout
 * @param enabled whether the component is enabled or grayed out
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Switch. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this Switch in different [Interaction]s.
 * @param colors [SwitchColors] that will be used to determine the color of the thumb and track
 * in different states. See [WPSwitchDefaults.colors].
 *
 * @see [Switch]
 */
@Composable
fun WPSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = WPSwitchDefaults.colors(),
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors,
    )
}

object WPSwitchDefaults {
    @Composable
    fun colors(): SwitchColors {
        val disabledAlpha = 0.3f // it seems this the alpha used by the AppCompat
        // thumb colors
        val baseThumbColor = Color(0xFFF9F9F9) // color from the 9-patch drawable used for the thumb
        val thumbDisabledColor = colorResource(
            if (MaterialTheme.colors.isLight) {
                R.color.switch_thumb_disabled_material_light
            } else {
                R.color.switch_thumb_disabled_material_dark
            }
        )
        val thumbEnabledUncheckedColor = colorResource(
            if (MaterialTheme.colors.isLight) {
                R.color.switch_thumb_normal_material_light
            } else {
                R.color.switch_thumb_normal_material_dark
            }
        )
        val thumbEnabledCheckedColor = MaterialTheme.colors.primary

        // track colors
        val baseTrackColor = MaterialTheme.colors.surface
        val trackDisabledColor = MaterialTheme.colors.onSurface.copy(alpha = disabledAlpha)
        val trackEnabledUncheckedColor = MaterialTheme.colors.onSurface
        val trackEnabledCheckedColor = MaterialTheme.colors.primary

        return SwitchDefaults.colors(
            checkedTrackAlpha = disabledAlpha,
            uncheckedTrackAlpha = disabledAlpha,
            checkedThumbColor = thumbEnabledCheckedColor.multiply(baseThumbColor),
            checkedTrackColor = trackEnabledCheckedColor,
            uncheckedThumbColor = thumbEnabledUncheckedColor.multiply(baseThumbColor),
            uncheckedTrackColor = trackEnabledUncheckedColor,
            disabledCheckedThumbColor = thumbDisabledColor.multiply(baseThumbColor),
            disabledCheckedTrackColor = trackDisabledColor.compositeOver(baseTrackColor),
            disabledUncheckedThumbColor = thumbDisabledColor.multiply(baseThumbColor),
            disabledUncheckedTrackColor = trackDisabledColor.compositeOver(baseTrackColor),
        )
    }

    private fun Color.multiply(other: Color): Color {
        return Color(
            red = this.red * other.red,
            green = this.green * other.green,
            blue = this.blue * other.blue,
            alpha = this.alpha * other.alpha,
        )
    }
}

@SuppressLint("SetTextI18n")
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
fun WPSwitchPreview() {
    AppTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            val itemModifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)

            AndroidView(
                factory = { context ->
                    SwitchCompat(context).apply {
                        isChecked = true
                        isEnabled = true
                        text = "The first one view gets wrong colors..."
                    }
                },
                modifier = itemModifier
            )

            Divider()

            // compose enabled checked
            Row(modifier = itemModifier) {
                Text(
                    "Compose enabled checked",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2
                )
                WPSwitch(checked = true, onCheckedChange = null)
            }

            Divider()

            // view enabled checked
            AndroidView(
                factory = { context ->
                    SwitchCompat(context).apply {
                        isChecked = true
                        isEnabled = true
                        text = "View enabled checked"
                    }
                },
                modifier = itemModifier
            )

            Divider()

            // compose enabled unchecked
            Row(modifier = itemModifier) {
                Text(
                    "Compose enabled unchecked",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2
                )
                WPSwitch(checked = false, onCheckedChange = null)
            }

            Divider()

            // view enabled unchecked
            AndroidView(
                factory = { context ->
                    SwitchCompat(context).apply {
                        isChecked = false
                        isEnabled = true
                        text = "View enabled unchecked"
                    }
                },
                modifier = itemModifier
            )

            Divider()

            // compose disabled checked
            Row(modifier = itemModifier) {
                Text(
                    "Compose disabled checked",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2
                )
                WPSwitch(checked = true, onCheckedChange = null, enabled = false)
            }

            Divider()

            // view disabled checked
            AndroidView(
                factory = { context ->
                    SwitchCompat(context).apply {
                        isChecked = true
                        isEnabled = false
                        text = "View disabled checked"
                    }
                },
                modifier = itemModifier
            )

            Divider()

            // compose disabled unchecked
            Row(modifier = itemModifier) {
                Text(
                    "Compose disabled unchecked",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.body2
                )
                WPSwitch(checked = false, onCheckedChange = null, enabled = false)
            }

            Divider()

            // view disabled unchecked
            AndroidView(
                factory = { context ->
                    SwitchCompat(context).apply {
                        isChecked = false
                        isEnabled = false
                        text = "View disabled unchecked"
                    }
                },
                modifier = itemModifier
            )
        }
    }
}
