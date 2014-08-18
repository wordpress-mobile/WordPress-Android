package org.wordpress.android.ui.reader;

import android.support.v4.view.ViewPager;
import android.view.View;

/*
 * ViewPager transformation animation invoked when a visible/attached page is scrolled - before
 * changing this, first see https://code.google.com/p/android/issues/detail?id=58918#c5
 * tl;dr make sure to remove X translation when a page is no longer fully visible
 *
 * zoom & depth are based on examples here, with many fixes and simplifications:
 * http://developer.android.com/training/animation/screen-slide.html#pagetransformer
 */
class ReaderViewPagerTransformer implements ViewPager.PageTransformer {
    static enum TransformType {
        FLOW,
        DEPTH,
        ZOOM,
        SLIDE_OVER
    }
    private final TransformType mTransformType;

    ReaderViewPagerTransformer(TransformType transformType) {
        mTransformType = transformType;
    }

    private static final float MIN_SCALE_DEPTH = 0.75f;
    private static final float MIN_SCALE_ZOOM = 0.85f;
    private static final float MIN_ALPHA_ZOOM = 0.5f;
    private static final float SCALE_FACTOR_SLIDE = 0.95f;

    public void transformPage(View view, float position) {
        final float alpha;
        final float scale;
        final float translationX;

        switch (mTransformType) {
            case FLOW:
                view.setRotationY(position * -30f);
                return;

            case SLIDE_OVER:
                alpha = 1;
                if (position <= 0) {
                    // apply effect only to pages on the left
                    scale = Math.abs(Math.abs(position) - 1) * (1.0f - SCALE_FACTOR_SLIDE) + SCALE_FACTOR_SLIDE;
                    int pageWidth = view.getWidth();
                    float translateValue = position * -pageWidth;
                    if (translateValue > -pageWidth) {
                        translationX = translateValue;
                    } else {
                        translationX = 0;
                    }
                } else {
                    scale = 1;
                    translationX = 0;
                }
                break;

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
