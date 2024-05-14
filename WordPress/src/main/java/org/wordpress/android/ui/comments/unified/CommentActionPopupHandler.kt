@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments.unified

import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.CommentActionsBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.comments.CommentDetailFragment
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditActivity.Companion.createIntent
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.analytics.AnalyticsUtils

object CommentActionPopupHandler {
    @JvmStatic
    fun show(anchorView: View, listener: CommentDetailFragment.OnEditCommentListener?) {
        val popupWindow = PopupWindow(anchorView.context, null, R.style.WordPress)
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = anchorView.context.resources.getDimension(R.dimen.popup_over_toolbar_elevation)
        popupWindow.contentView = CommentActionsBinding
            .inflate(LayoutInflater.from(anchorView.context))
            .apply {
                textUserInfo.setOnClickListener {
//                    EngagedListNavigationEvent.OpenUserProfileBottomSheet()
                    popupWindow.dismiss()
                }
                textShare.setOnClickListener {
                    ToastUtils.showToast(it.context, "not yet implemented")
                    popupWindow.dismiss()
                }
                textEditComment.setOnClickListener {
                    listener?.onClicked()
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
