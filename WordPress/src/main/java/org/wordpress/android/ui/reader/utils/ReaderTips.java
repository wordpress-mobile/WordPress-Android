package org.wordpress.android.ui.reader.utils;

import android.app.Activity;
import android.os.Parcelable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarStyle;

import org.wordpress.android.R;

/**
 * shows one-shot "tips" related to reader activities - relies on a styled undo bar
 */
public class ReaderTips {
    public static enum ReaderTipType {
        SWIPE_POSTS
    }

    private static final int TIP_DURATION = (UndoBarStyle.DEFAULT_DURATION * 2);

    public static void showTip(Activity activity, final ReaderTipType tipType) {
        if (tipType == null || isTipShown(tipType)) {
            return;
        }
        if (activity ==  null || activity.isFinishing()) {
            return;
        }

        int tipMessageRes;
        switch (tipType) {
            case SWIPE_POSTS:
                tipMessageRes = R.string.reader_tip_swipe_posts;
                break;
            default:
                return;
        }

        UndoBarStyle style = new UndoBarStyle(
                R.drawable.ic_action_accept,
                R.string.reader_btn_got_it,
                R.drawable.button_blue,
                TIP_DURATION);
        Animation animIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        Animation animOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out);
        style.setAnim(animIn, animOut);

        UndoBarController.UndoListener listener = new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                setTipShown(tipType);
            }
        };

        new UndoBarController.UndoBar(activity)
                .message(tipMessageRes)
                .listener(listener)
                .style(style)
                .translucent(true)
                .show();
    }

    public static void reset() {

    }

    private static boolean isTipShown(ReaderTipType tipType) {
        return false;
    }

    private static void setTipShown(ReaderTipType tipType) {

    }
}
