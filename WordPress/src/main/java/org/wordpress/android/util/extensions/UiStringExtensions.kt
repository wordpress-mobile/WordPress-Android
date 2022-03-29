package org.wordpress.android.util.extensions

import android.content.Context
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText

fun UiString.getText(context: Context): String = when (this) {
    is UiStringText -> text.toString().orEmpty()
    is UiStringRes -> context.getString(stringRes).orEmpty()
    is UiStringResWithParams -> context.getString(stringRes, params.map { it.getText(context) }.toTypedArray())
            .orEmpty()
}
