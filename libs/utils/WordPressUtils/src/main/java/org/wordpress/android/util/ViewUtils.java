package org.wordpress.android.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewOutlineProvider;

import java.util.concurrent.atomic.AtomicInteger;

public class ViewUtils {
    /**
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *  Uses the native implementation if API 17 or above, otherwise uses a copied implementation.
     *
     * @return a generated ID value
     */
    public static int generateViewId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return View.generateViewId();
        } else {
            return copiedGenerateViewId();
        }
    }

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    /**
     * Copied from {@link View#generateViewId()}
     * Generate a value suitable for use in {@link View#setId(int)}.
     * This value will not collide with ID values generated at build time by aapt for R.id.
     *
     * @return a generated ID value
     */
    private static int copiedGenerateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static void setButtonBackgroundColor(Context context, View button, @StyleRes int styleId,
            @AttrRes int colorAttribute) {
        TypedArray a = context.obtainStyledAttributes(styleId, new int[] { colorAttribute } );
        ColorStateList color = a.getColorStateList(0);
        a.recycle();
        ViewCompat.setBackgroundTintList(button, color);
    }

    /**
     * adds an inset circular shadow outline the passed view (Lollipop+ only) - note that
     * the view should have its elevation set prior to calling this
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void addCircularShadowOutline(@NonNull View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        }
    }
}
