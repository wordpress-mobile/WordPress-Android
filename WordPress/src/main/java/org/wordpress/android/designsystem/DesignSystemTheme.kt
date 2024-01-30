package org.wordpress.android.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

@Composable
fun DesignSystemTheme(
    colors: DesignSystemColorsBase = DesignSystemTheme.colors,
    content: @Composable () -> Unit
) {
    val rememberedColors = remember { colors.copy() }.apply { updateColorsFrom(colors) }
    CompositionLocalProvider(
        LocalColors provides rememberedColors
    ) {
        content()
    }
}

    object DesignSystemTheme {
        val colors: DesignSystemColorsBase
            @Composable
            @ReadOnlyComposable
            get() = LocalColors.current
    }

