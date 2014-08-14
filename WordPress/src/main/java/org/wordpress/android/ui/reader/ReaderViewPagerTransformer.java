package org.wordpress.android.ui.reader;

import android.support.v4.view.ViewPager;
import android.view.View;

/*
 * ViewPager transformation animation invoked when a visible/attached page is scrolled - before
 * changing this, first see https://code.google.com/p/android/issues/detail?id=58918
 *
 * note: based on examples here, with many fixes and simplifications:
 * http://developer.android.com/training/animation/screen-slide.html#pagetransformer
 */
class ReaderViewPagerTransformer implements ViewPager.PageTransformer {
    static enum TransformType { DEPTH, ZOOM }
    private final TransformType mTransformType;

    ReaderViewPagerTransformer(TransformType transformType) {
        mTransformType = transformType;
    }

    private static final float MIN_SCALE_DEPTH = 0.75f;
    private static final float MIN_SCALE_ZOOM = 0.85f;
    private static final float MIN_ALPHA_ZOOM = 0.5f;

    public void transformPage(View view, float position) {
        float alpha;
        float scale;
        float translationX;

        switch (mTransformType) {
            case DEPTH:
                if (position > 0 && position < 1) {
                    // moving to the right
                    alpha = (1 - position);
                    scale = MIN_SCALE_DEPTH + (1 - MIN_SCALE_DEPTH) * (1 - Math.abs(position));
                    translationX = (view.getWidth() * -position);
                } else {
                    // use default for all other cases
                    alpha = 1;
                    scale = 1;
                    translationX = 0;
                }
                break;

            case ZOOM:
                if (position >= -1 && position <= 1) {
                    scale = Math.max(MIN_SCALE_ZOOM, 1 - Math.abs(position));
                    alpha = MIN_ALPHA_ZOOM +
                            (scale - MIN_SCALE_ZOOM) / (1 - MIN_SCALE_ZOOM) * (1 - MIN_ALPHA_ZOOM);
                    float vMargin = view.getHeight() * (1 - scale) / 2;
                    float hMargin = view.getWidth() * (1 - scale) / 2;
                    if (position < 0) {
                        translationX = (hMargin - vMargin / 2);
                    } else {
                        translationX = (-hMargin + vMargin / 2);
                    }
                } else {
                    alpha = 1;
                    scale = 1;
                    translationX = 0;
                }
                break;

            default:
                return;
        }

        view.setAlpha(alpha);
        view.setTranslationX(translationX);
        view.setScaleX(scale);
        view.setScaleY(scale);
    }
}
