package org.wordpress.android.util

import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.widget.ImageView
import android.widget.TextView

/**
 * This class represents either a String resource in strings.xml file or a String value to be drawn in a TextView
 */
class TextResource
private constructor(@StringRes val id: Int? = null, val text: String? = null) {
    constructor(@StringRes id: Int): this(id, null)
    constructor(text: String): this(null, text)
}

fun TextView.setTextResource(resource: TextResource) {
    resource.id?.let { this.setText(it) }
    resource.text?.let { this.text = it }
}

class ImageResource
private constructor(@DrawableRes val id: Int? = null, val text: String? = null) {
    constructor(@DrawableRes id: Int): this(id, null)
    constructor(text: String): this(null, text)
}

fun ImageView.loadImage(resource: ImageResource, load: (url: String) -> Unit) {
    resource.id?.let { this.setImageResource(it) }
    resource.text?.let { load(it) }
}
