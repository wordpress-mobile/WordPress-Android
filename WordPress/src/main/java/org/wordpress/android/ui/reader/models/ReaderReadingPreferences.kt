package org.wordpress.android.ui.reader.models

data class ReaderReadingPreferences(
    val theme: Theme,
    val textSizeMultiplier: Float = 1.0f,
) {
    sealed class Theme(
        open val backgroundHexColor: String,
        open val textHexColor: String,
        open val fontFamily: String,
    ) {
        data object Light : Theme(
            backgroundHexColor = "#FFFFFF",
            textHexColor = "#000000",
            fontFamily = "sans-serif"
        )

        data object Dark : Theme(
            backgroundHexColor = "#000000",
            textHexColor = "#FFFFFF",
            fontFamily = "sans-serif"
        )

        data class Custom(
            override val backgroundHexColor: String,
            override val textHexColor: String,
            override val fontFamily: String,
        ) : Theme(
            backgroundHexColor = backgroundHexColor,
            textHexColor = textHexColor,
            fontFamily = fontFamily
        )
    }
}
