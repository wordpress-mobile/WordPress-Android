package org.wordpress.android.ui.reader.views.compose.readingpreferences

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences

fun ReaderReadingPreferences.FontFamily.toComposeFontFamily(): FontFamily {
    return when (this) {
        ReaderReadingPreferences.FontFamily.SANS -> FontFamily.SansSerif
        ReaderReadingPreferences.FontFamily.SERIF -> FontFamily.Serif
        ReaderReadingPreferences.FontFamily.MONO -> FontFamily.Monospace
    }
}

fun ReaderReadingPreferences.FontSize.toSp(): TextUnit {
    return value.sp
}
