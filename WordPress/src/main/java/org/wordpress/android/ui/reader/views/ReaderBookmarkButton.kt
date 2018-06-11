package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.reader_bookmark_button.view.*
import org.wordpress.android.R
import java.util.Locale

/**
 * Bookmark button used in reader post detail
 */
class ReaderBookmarkButton : LinearLayout {
    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView(context)
    }

    private fun initView(context: Context) {
        View.inflate(context, R.layout.reader_bookmark_button, this)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        label_bookmark_button.isEnabled = enabled
        icon_bookmark_button.isEnabled = enabled
    }

    fun updateIsBookmarkedState(isBookmarked: Boolean) {
        val bookmarkButtonLabel = if (isBookmarked) R.string.reader_btn_bookmarked else R.string.reader_btn_bookmark

        label_bookmark_button.text = context.getString(bookmarkButtonLabel).toUpperCase(Locale.getDefault())

        label_bookmark_button.isSelected = isBookmarked
        icon_bookmark_button.isSelected = isBookmarked

        contentDescription = if (isBookmarked) {
            context.getString(R.string.reader_remove_bookmark)
        } else {
            context.getString(R.string.reader_add_bookmark)
        }
    }
}
