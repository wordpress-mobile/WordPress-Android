package org.wordpress.android.ui.compose.utils

import android.content.res.Configuration
import android.os.Build
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import org.wordpress.android.util.extensions.primaryLocale
import java.util.Locale

/**
 * Utility function that returns a Composable function that wraps the [content] inside a [CompositionLocalProvider]
 * setting the [LocalContentAlpha] to 1f. Useful for using with some Material Composables that override that alpha
 * Composition Local in a hard-coded fashion (e.g.: TopAppBar). This should not need to be used very often.
 */
fun withFullContentAlpha(content: @Composable () -> Unit): @Composable () -> Unit = {
    CompositionLocalProvider(
        LocalContentAlpha provides 1f,
        content = content
    )
}

/**
 * Utility function that wraps the [content] inside a [CompositionLocalProvider] overriding the [LocalContext]
 * configuration with the specified [locale] when the specified language should apply.
 * Useful to apply a custom language to Compose UIs that do not respond correctly to app language changes.
 * @param locale The locale to be used in the [LocalContext] configuration override.
 * @param onLocaleChange Callback to be invoked when the locale is overridden, useful to update other app components.
 * @param content The Composable function to be rendered with the overridden locale.
 */

@Suppress("DEPRECATION")
@Composable
fun LocaleAwareComposable(
    locale: Locale = Locale.getDefault(),
    onLocaleChange: (Locale) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val resources = context.resources
    val configuration = resources.configuration

    val currentLocale = context.primaryLocale
    if (currentLocale != locale) {
        val newContext = context.createConfigurationContext(
            Configuration(configuration).apply {
                setLocale(locale)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                    configuration.locale = locale
                    resources.updateConfiguration(configuration, resources.displayMetrics)
                }
            }
        )
        onLocaleChange(locale)
        CompositionLocalProvider(LocalContext provides newContext) {
            content()
        }
    } else {
        content()
    }
}
