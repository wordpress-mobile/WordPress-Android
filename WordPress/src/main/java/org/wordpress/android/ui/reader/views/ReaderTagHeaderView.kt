package org.wordpress.android.ui.reader.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.reader_tag_header_view.view.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderTagActions
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
class ReaderTagHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    private var currentTag: ReaderTag? = null

    @Inject lateinit var accountStore: AccountStore

    init {
        (context.applicationContext as WordPress).component().inject(this)
        initView(context)
    }

    private fun initView(context: Context) {
        inflate(context, R.layout.reader_tag_header_view, this)
        follow_button.setOnClickListener { toggleFollowStatus() }
    }

    fun setCurrentTag(tag: ReaderTag?) {
        if (tag == null) {
            return
        }
        val isTagChanged = !ReaderTag.isSameTag(tag, currentTag)
        if (isTagChanged) {
            currentTag = tag
        }
        updateUi(tag)
    }

    private fun updateUi(tag: ReaderTag) {
        text_tag.text = tag.label
        follow_button.visibility = if (!accountStore.hasAccessToken()) View.GONE else View.VISIBLE
        follow_button.setIsFollowed(ReaderTagTable.isFollowedTagName(tag.tagSlug))
    }

    private fun toggleFollowStatus() {
        if (currentTag == null || !NetworkUtils.checkConnection(context)) {
            return
        }
        val isAskingToFollow = currentTag?.let { !ReaderTagTable.isFollowedTagName(it.tagSlug) } ?: false
        val listener = ActionListener { succeeded: Boolean ->
            if (context == null) {
                return@ActionListener
            }
            follow_button.isEnabled = true
            if (!succeeded) {
                val errResId = if (isAskingToFollow) {
                    R.string.reader_toast_err_add_tag
                } else {
                    R.string.reader_toast_err_remove_tag
                }
                ToastUtils.showToast(context, errResId)
                follow_button.setIsFollowed(!isAskingToFollow)
            }
        }
        follow_button.isEnabled = false
        val success = if (isAskingToFollow) {
            ReaderTagActions.addTag(currentTag, listener)
        } else {
            ReaderTagActions.deleteTag(currentTag, listener)
        }
        if (success) {
            follow_button.setIsFollowedAnimated(isAskingToFollow)
        }
    }
}
