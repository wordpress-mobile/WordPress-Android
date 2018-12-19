package org.wordpress.android.ui.utils

import android.content.Context
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

// TODO: Add this function and similar ones to an injected helper object
fun getTextOfUiString(context: Context, uiString: UiString) =
        when (uiString) {
            is UiStringRes -> context.getString(uiString.stringRes)
            is UiStringText -> uiString.text
        }
