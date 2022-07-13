package org.wordpress.android.ui.mysite.jetpackbadge

import androidx.annotation.RawRes
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.CAPTION
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.ILLUSTRATION
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Type.TITLE
import org.wordpress.android.ui.utils.UiString

sealed class JetpackPoweredItem(val type: Type) {
    enum class Type {
        ILLUSTRATION,
        TITLE,
        CAPTION
    }

    data class Illustration(@RawRes val illustration: Int) : JetpackPoweredItem(ILLUSTRATION)
    data class Title(val text: UiString) : JetpackPoweredItem(TITLE)
    data class Caption(val text: UiString) : JetpackPoweredItem(CAPTION)
}
