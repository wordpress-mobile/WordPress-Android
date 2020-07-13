package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.reader_tag_header_view.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.reader.views.ReaderTagHeaderViewUiState.ReaderTagHeaderUiState
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
class ReaderTagHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    @Inject lateinit var uiHelpers: UiHelpers

    private var onFollowBtnClicked: (() -> Unit)? = null

    init {
        (context.applicationContext as WordPress).component().inject(this)
        initView(context)
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.reader_tag_header_view, this)
        follow_button.setOnClickListener { onFollowBtnClicked?.invoke() }
    }

    fun updateUi(uiState: ReaderTagHeaderUiState) {
        text_tag.text = uiState.title
        with(uiState.followButtonUiState) {
            uiHelpers.updateVisibility(follow_button, followButtonVisibility)
            follow_button.setIsFollowed(isFollowed)
            onFollowBtnClicked = onFollowButtonClicked
        }
    }
}
