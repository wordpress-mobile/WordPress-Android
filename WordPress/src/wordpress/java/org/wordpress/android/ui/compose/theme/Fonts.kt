package org.wordpress.android.ui.compose.theme

import android.os.Build
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import org.wordpress.android.R.font

/**
 * On Huawei devices with Android 7 loading custom fonts fails; so we fallback to the standard Serif font.
 * see: [GitHub: Problem with fonts on the Android platform](https://github.com/JetBrains/compose-jb/issues/333)
 */
val FontFamily.Companion.EBGaramond
    get() = when (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
        true -> FontFamily(Font(font.eb_garamond))
        false -> Serif
    }
