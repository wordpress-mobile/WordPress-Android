package org.wordpress.android.ui.comments;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;

import org.wordpress.android.R;

/**
 * Dialogs related to comment moderation displayed from CommentsActivity and NotificationsActivity
 */
class CommentDialogs {
    public static final int ID_COMMENT_DLG_APPROVING = 100;
    public static final int ID_COMMENT_DLG_DISAPPROVING = 101;
    public static final int ID_COMMENT_DLG_SPAMMING = 102;
    public static final int ID_COMMENT_DLG_TRASHING = 103;
    public static final int ID_COMMENT_DLG_DELETING = 104;

    private CommentDialogs() {
        throw new AssertionError();
    }

    public static Dialog createCommentDialog(Activity activity, int dialogId) {
        final int resId;
        switch (dialogId) {
            case ID_COMMENT_DLG_APPROVING :
                resId = R.string.dlg_approving_comments;
                break;
            case ID_COMMENT_DLG_DISAPPROVING:
                resId = R.string.dlg_unapproving_comments;
                break;
            case ID_COMMENT_DLG_TRASHING:
                resId = R.string.dlg_trashing_comments;
                break;
            case ID_COMMENT_DLG_SPAMMING:
                resId = R.string.dlg_spamming_comments;
                break;
            case ID_COMMENT_DLG_DELETING:
                resId = R.string.dlg_deleting_comments;
                break;
            default :
                return null;
        }

        ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setMessage(activity.getString(resId));
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);

        return dialog;
    }
}
