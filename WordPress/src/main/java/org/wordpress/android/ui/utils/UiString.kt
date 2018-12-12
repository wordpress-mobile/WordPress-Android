package org.wordpress.android.ui.utils

import android.support.annotation.StringRes

/**
 * [UiString] is a utility sealed class that represents a string to be used in the UI. It allows a string to be
 * represented as both string resource and text.
 */
sealed class UiString {
    class UiStringText(val text: String) : UiString()
    class UiStringRes(@StringRes val stringRes: Int) : UiString()
}
