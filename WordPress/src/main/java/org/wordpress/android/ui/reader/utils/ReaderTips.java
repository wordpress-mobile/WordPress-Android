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
    public static enum ReaderTipType {
        SWIPE_POSTS
    }

    public static void showTipDelayed(final Activity activity, final ReaderTipType tipType) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showTip(activity, tipType);
            }
        }, 500);
    }

    public static void showTip(final Activity activity, final ReaderTipType tipType) {
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
                R.drawable.reader_tip_bg,
                UndoBarStyle.DEFAULT_DURATION);
        Animation animIn = AnimationUtils.loadAnimation(activity, R.anim.fade_in);
        Animation animOut = AnimationUtils.loadAnimation(activity, R.anim.fade_out);
        style.setAnim(animIn, animOut);

        UndoBarController.UndoListener listener = new UndoBarController.UndoListener() {
            @Override
            public void onUndo(Parcelable parcelable) {
                //setTipShown(tipType);
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
        SharedPreferences.Editor editor = AppPrefs.prefs().edit();
        for (ReaderTipType tipType: ReaderTipType.values()) {
            editor.remove(getTipPrefKey(tipType));
        }
        editor.apply();
    }

    private static boolean isTipShown(ReaderTipType tipType) {
        return AppPrefs.prefs().getBoolean(getTipPrefKey(tipType), false);
    }

    private static void setTipShown(ReaderTipType tipType) {
        AppPrefs.prefs().edit().putBoolean(getTipPrefKey(tipType), true).apply();
    }

    private static String getTipPrefKey(ReaderTipType tipType) {
        switch (tipType) {
            case SWIPE_POSTS:
                return "reader-tip-swipe-posts";
            default :
                AppLog.w(AppLog.T.READER, "unknown reader tip type");
                return "";
        }
    }
}
