package org.wordpress.android.ui.reader.utils;

import android.content.ActivityNotFoundException;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import javax.annotation.Nonnull;

/*
 * custom LinkMovementMethod which shows photo viewer when an image span is tapped
 */
public class ReaderLinkMovementMethod extends LinkMovementMethod {
    private static ReaderLinkMovementMethod mMovementMethod;
    private static ReaderLinkMovementMethod mMovementMethodPrivate;

    private final boolean mIsPrivate;

    /*
     * note that separate instances are returned depending on whether we're showing
     * content from a private blog
     */
    public static ReaderLinkMovementMethod getInstance(boolean isPrivate) {
        if (isPrivate) {
            if (mMovementMethodPrivate == null) {
                mMovementMethodPrivate = new ReaderLinkMovementMethod(true);
            }
            return mMovementMethodPrivate;
        } else {
            if (mMovementMethod == null) {
                mMovementMethod = new ReaderLinkMovementMethod(false);
            }
            return mMovementMethod;
        }
    }

    /*
     * override MovementMethod.getInstance() to ensure our getInstance(false) is used
     */
    public static ReaderLinkMovementMethod getInstance() {
        return getInstance(false);
    }

    private ReaderLinkMovementMethod(boolean isPrivate) {
        super();
        mIsPrivate = isPrivate;
    }

    @Override
    public boolean onTouchEvent(@Nonnull TextView textView,
                                @Nonnull Spannable buffer,
                                @Nonnull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= textView.getTotalPaddingLeft();
            y -= textView.getTotalPaddingTop();

            x += textView.getScrollX();
            y += textView.getScrollY();

            Layout layout = textView.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ImageSpan[] images = buffer.getSpans(off, off, ImageSpan.class);
            if (images != null && images.length > 0) {
                String imageUrl = StringUtils.notNullStr(images[0].getSource());
                ReaderActivityLauncher.showReaderPhotoViewer(
                        textView.getContext(),
                        imageUrl,
                        null,
                        textView,
                        mIsPrivate,
                        (int) event.getX(),
                        (int) event.getY());
                return true;
            }
        }

        try {
            return super.onTouchEvent(textView, buffer, event);
        } catch (ActivityNotFoundException e) {
            AppLog.e(AppLog.T.UTILS, e);
            return false;
        }
    }
}
