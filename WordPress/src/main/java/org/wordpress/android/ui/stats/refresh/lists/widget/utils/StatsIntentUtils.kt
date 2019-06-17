package org.wordpress.android.ui.stats.refresh.lists.widget.utils

import android.content.Intent
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment.ViewType
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel.Color

private const val COLOR_MODE_KEY = "color_mode_key"
private const val VIEW_TYPE_KEY = "view_type_key"

fun Intent.getColorMode(): Color {
    return this.getEnumExtra(COLOR_MODE_KEY, Color.LIGHT)
}

fun Intent.putColorMode(color: Color) {
    this.putEnumExtra(COLOR_MODE_KEY, color)
}

fun Intent.getViewType(): ViewType {
    return this.getEnumExtra(VIEW_TYPE_KEY, ViewType.WEEK_VIEWS)
}

fun Intent.putViewType(viewType: ViewType) {
    this.putEnumExtra(VIEW_TYPE_KEY, viewType)
}

private inline fun <reified T : Enum<T>> Intent.putEnumExtra(key: String, victim: T): Intent =
        putExtra(key, victim.ordinal)

private inline fun <reified T : Enum<T>> Intent.getEnumExtra(key: String, default: T): T =
        getIntExtra(key, -1)
                .takeUnless { it == -1 }
                ?.let { T::class.java.enumConstants[it] } ?: default
