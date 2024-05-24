@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments.unified

import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import org.wordpress.android.R
import org.wordpress.android.databinding.CommentActionsBinding
import org.wordpress.android.ui.comments.CommentDetailFragment
import org.wordpress.android.util.ToastUtils

object CommentActionPopupHandler {
    @JvmStatic
    fun show(anchorView: View, listener: CommentDetailFragment.OnActionClickListener?) {
        val popupWindow = PopupWindow(anchorView.context, null, R.style.WordPress)
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = anchorView.context.resources.getDimension(R.dimen.popup_over_toolbar_elevation)
        popupWindow.contentView = CommentActionsBinding
            .inflate(LayoutInflater.from(anchorView.context))
            .apply {
                textUserInfo.setOnClickListener {
                    listener?.onUserInfoClicked()
                    popupWindow.dismiss()
                }
                textShare.setOnClickListener {
                    ToastUtils.showToast(it.context, "not yet implemented")
                    popupWindow.dismiss()
                }
                textEditComment.setOnClickListener {
                    listener?.onEditCommentClicked()
                    popupWindow.dismiss()
                }
                textChangeStatus.setOnClickListener {
                    ToastUtils.showToast(it.context, "not yet implemented")
                    popupWindow.dismiss()
                }
            }.root
        popupWindow.showAsDropDown(anchorView)
    }
}
