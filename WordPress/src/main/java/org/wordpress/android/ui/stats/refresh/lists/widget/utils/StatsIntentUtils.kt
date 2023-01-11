package org.wordpress.android.ui.stats.refresh.lists.widget.utils

import android.content.Intent
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.WidgetType

private const val COLOR_MODE_KEY = "color_mode_key"
private const val VIEW_TYPE_KEY = "view_type_key"

fun Intent.getColorMode(): Color {
    return this.getEnumExtra(COLOR_MODE_KEY, Color.LIGHT)
}

fun Intent.putColorMode(color: Color) {
    this.putEnumExtra(COLOR_MODE_KEY, color)
}

fun Intent.getViewType(): WidgetType {
    return this.getEnumExtra(VIEW_TYPE_KEY, WidgetType.WEEK_VIEWS)
}

fun Intent.putViewType(widgetType: WidgetType) {
    this.putEnumExtra(VIEW_TYPE_KEY, widgetType)
}

private inline fun <reified T : Enum<T>> Intent.putEnumExtra(key: String, victim: T): Intent =
    putExtra(key, victim.ordinal)

private inline fun <reified T : Enum<T>> Intent.getEnumExtra(key: String, default: T): T =
    getIntExtra(key, -1)
        .takeUnless { it == -1 }
        ?.let {
            @Suppress("UNNECESSARY_SAFE_CALL")
            T::class.java.enumConstants?.get(it)
        } ?: default
