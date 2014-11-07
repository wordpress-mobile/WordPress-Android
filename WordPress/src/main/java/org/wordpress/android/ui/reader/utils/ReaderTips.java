package org.wordpress.android.ui.reader.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Parcelable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.cocosw.undobar.UndoBarController;
import com.cocosw.undobar.UndoBarStyle;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;

/**
 * shows one-shot "tips" related to reader activities - relies on a styled undo bar
 */
public class ReaderTips {
    private static final int TIP_DURATION = UndoBarStyle.DEFAULT_DURATION * 2;

    public static enum ReaderTipType {
        READER_SWIPE_POSTS
    }

    public static void showTipDelayed(final Activity activity, final ReaderTipType tipType) {
        if (isTipShown(tipType)) {
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (activity != null && !activity.isFinishing()) {
                    showTip(activity, tipType);
                }
            }
        }, 500);
    }

    private static void showTip(final Activity activity, final ReaderTipType tipType) {
        int tipMessageRes;
        switch (tipType) {
            case READER_SWIPE_POSTS:
                tipMessageRes = R.string.reader_tip_swipe_posts;
                break;
            default:
                return;
        }

        UndoBarStyle style = new UndoBarStyle(
                R.drawable.ic_action_accept,
                R.string.reader_btn_got_it,
                R.drawable.reader_tip_bg,
                TIP_DURATION);
        Animation animIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        Animation animOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out);
        animIn.setFillAfter(false);
        animOut.setFillAfter(false);
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

    public static void hideTip(Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            UndoBarController.clear(activity);
        }
    }

    public static void reset() {
        SharedPreferences.Editor editor = AppPrefs.prefs().edit();
        for (ReaderTipType tipType: ReaderTipType.values()) {
            editor.remove(getTipPrefKey(tipType));
        }
        editor.apply();
    }

    private static boolean isTipShown(ReaderTipType tipType) {
        if (tipType == null) {
            return true;
        } else {
            return AppPrefs.prefs().getBoolean(getTipPrefKey(tipType), false);
        }
    }

    public static void setTipShown(ReaderTipType tipType) {
        if (tipType != null) {
            AppPrefs.prefs().edit().putBoolean(getTipPrefKey(tipType), true).apply();
        }
    }

    private static String getTipPrefKey(ReaderTipType tipType) {
        switch (tipType) {
            case READER_SWIPE_POSTS:
                return "tip-reader-swipe-posts";
            default :
                AppLog.w(AppLog.T.READER, "reader tips > unknown tip type");
                return "";
        }
    }
}
