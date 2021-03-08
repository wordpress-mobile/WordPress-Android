package org.wordpress.android.ui.posts.chat

import android.view.View
import android.view.ViewGroup

val String.withBr: String
    get() = this.replace("\n", "<br>")

val String.toParagraph: String
    get() = "<!-- wp:paragraph --><p>$withBr</p><!-- /wp:paragraph -->"

val String.toHeading: String
    get() = "<!-- wp:heading --><h2>$withBr</h2><!-- /wp:heading -->"

val String.wordCount: Int
    get() = replace("\n", " ").split(" ").size

fun ViewGroup.findByContentDescription(contentDescription: String): View? {
    for (i in 0 until childCount) {
        val v = getChildAt(i)
        if (v.contentDescription == contentDescription) {
            return v
        } else if (v is ViewGroup) {
            return v.findByContentDescription(contentDescription)
        }
    }
    return null
}
