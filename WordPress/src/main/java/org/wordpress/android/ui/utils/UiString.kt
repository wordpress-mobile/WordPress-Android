package org.wordpress.android.ui.utils

import androidx.annotation.StringRes

/**
 * [UiString] is a utility sealed class that represents a string to be used in the UI. It allows a string to be
 * represented as both string resource and text.
 */
sealed class UiString {
    data class UiStringText(val text: CharSequence) : UiString()
    data class UiStringRes(@StringRes val stringRes: Int) : UiString()
    data class UiStringResWithParams(@StringRes val stringRes: Int, val params: List<UiString>) : UiString()

    // Current localization process does not support <plurals> resource strings,
    // so we need to use multiple string resources. Switch to @PluralsRes when it is supported by localization process.
    data class UiStringPluralRes(
        @StringRes val zeroRes: Int,
        @StringRes val oneRes: Int,
        @StringRes val otherRes: Int,
        val count: Int
    ) : UiString()
}
