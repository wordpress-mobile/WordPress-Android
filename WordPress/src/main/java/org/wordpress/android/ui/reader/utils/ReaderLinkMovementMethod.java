package org.wordpress.android.ui.reader.utils;

import android.content.ActivityNotFoundException;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ImageSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.PhotoViewerOption;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.util.EnumSet;

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
    @SuppressWarnings("unused")
    public static ReaderLinkMovementMethod getInstance() {
        return getInstance(false);
    }

    private ReaderLinkMovementMethod(boolean isPrivate) {
        super();
        mIsPrivate = isPrivate;
    }

    @Override
    public boolean onTouchEvent(@NonNull TextView textView,
                                @NonNull Spannable buffer,
                                @NonNull MotionEvent event) {
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
                EnumSet<PhotoViewerOption> options = EnumSet.noneOf(PhotoViewerOption.class);
                if (mIsPrivate) {
                    options.add(ReaderActivityLauncher.PhotoViewerOption.IS_PRIVATE_IMAGE);
                }
                String imageUrl = StringUtils.notNullStr(images[0].getSource());
                ReaderActivityLauncher.showReaderPhotoViewer(
                        textView.getContext(),
                        imageUrl,
                        null,
                        textView,
                        options,
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
