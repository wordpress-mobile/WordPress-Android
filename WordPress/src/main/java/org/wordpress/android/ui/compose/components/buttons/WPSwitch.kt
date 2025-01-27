package org.wordpress.android.ui.compose.components.buttons

import android.annotation.SuppressLint
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.isLightTheme
import org.wordpress.android.widgets.WPSwitchCompat
import com.google.android.material.R as MaterialR

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
    // always pass a lambda to `onCheckedChange` to avoid different padding when the callback is null
    // note: maybe provide `false` to LocalMinimumInteractiveComponentEnforcement for ignoring min 48dp touch target
    Switch(
        checked = checked,
        onCheckedChange = { onCheckedChange?.invoke(it) },
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = colors,
    )
}

object WPSwitchDefaults {
    @Composable
    fun colors(): SwitchColors {
        // thumb colors
        val thumbDisabledColor = colorResource(
            if (isLightTheme()) {
                MaterialR.color.switch_thumb_disabled_material_light
            } else {
                MaterialR.color.switch_thumb_disabled_material_dark
            }
        )
        val thumbEnabledUncheckedColor = colorResource(
            if (isLightTheme()) {
                MaterialR.color.switch_thumb_normal_material_light
            } else {
                MaterialR.color.switch_thumb_normal_material_dark
            }
        )
        val thumbEnabledCheckedColor = MaterialTheme.colorScheme.primary

        // track colors
        val baseTrackColor = MaterialTheme.colorScheme.surface
        val trackDisabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        val trackEnabledUncheckedColor = MaterialTheme.colorScheme.onSurface
        val trackEnabledCheckedColor = MaterialTheme.colorScheme.primary

        return SwitchDefaults.colors(
            checkedThumbColor = thumbEnabledCheckedColor,
            checkedTrackColor = trackEnabledCheckedColor,
            uncheckedThumbColor = thumbEnabledUncheckedColor,
            uncheckedTrackColor = trackEnabledUncheckedColor,
            disabledCheckedThumbColor = thumbDisabledColor,
            disabledCheckedTrackColor = trackDisabledColor.compositeOver(baseTrackColor),
            disabledUncheckedThumbColor = thumbDisabledColor,
            disabledUncheckedTrackColor = trackDisabledColor.compositeOver(baseTrackColor),
        )
    }
}

/**
 * Compose for previewing against the Android View-based [WPSwitchCompat].
 */
@Composable
private fun StatefulWPSwitchWithText(
    text: String,
    modifier: Modifier = Modifier,
    initialCheckedState: Boolean = false,
    enabled: Boolean = true,
) {
    val checkedState = remember { mutableStateOf(initialCheckedState) }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color.Unspecified else LocalContentColor.current.copy(alpha = 0.38f),
        )
        WPSwitch(
            checked = checkedState.value,
            onCheckedChange = { checkedState.value = it },
            enabled = enabled,
        )
    }
}

@SuppressLint("SetTextI18n")
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun WPSwitchPreview() {
    AppThemeM3 {
        Column(modifier = Modifier.fillMaxWidth()) {
            val viewModifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)

            val composeModifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)

            // compose enabled checked
            StatefulWPSwitchWithText(
                text = "Compose enabled checked",
                modifier = composeModifier,
                initialCheckedState = true,
            )

            HorizontalDivider()

            // view enabled checked
            AndroidView(
                factory = { context ->
                    WPSwitchCompat(context).apply {
                        isChecked = true
                        isEnabled = true
                        text = "View enabled checked"
                    }
                },
                modifier = viewModifier
            )

            HorizontalDivider()

            // compose enabled unchecked
            StatefulWPSwitchWithText(
                text = "Compose enabled unchecked",
                modifier = composeModifier,
                initialCheckedState = false,
            )

            HorizontalDivider()

            // view enabled unchecked
            AndroidView(
                factory = { context ->
                    WPSwitchCompat(context).apply {
                        isChecked = false
                        isEnabled = true
                        text = "View enabled unchecked"
                    }
                },
                modifier = viewModifier
            )

            HorizontalDivider()

            // compose disabled checked
            StatefulWPSwitchWithText(
                text = "Compose disabled checked",
                modifier = composeModifier,
                initialCheckedState = true,
                enabled = false,
            )

            HorizontalDivider()

            // view disabled checked
            AndroidView(
                factory = { context ->
                    WPSwitchCompat(context).apply {
                        isChecked = true
                        isEnabled = false
                        text = "View disabled checked"
                    }
                },
                modifier = viewModifier
            )

            HorizontalDivider()

            // compose disabled unchecked
            StatefulWPSwitchWithText(
                text = "Compose disabled unchecked",
                modifier = composeModifier,
                initialCheckedState = false,
                enabled = false,
            )

            HorizontalDivider()

            // view disabled unchecked
            AndroidView(
                factory = { context ->
                    WPSwitchCompat(context).apply {
                        isChecked = false
                        isEnabled = false
                        text = "View disabled unchecked"
                    }
                },
                modifier = viewModifier
            )
        }
    }
}
