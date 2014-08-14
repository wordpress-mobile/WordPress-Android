package org.wordpress.android.ui.reader;

import android.support.v4.view.ViewPager;
import android.view.View;

/*
 * custom ViewPager transformation animation invoked whenever a visible/attached
 * page is is scrolled
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
        switch (mTransformType) {
            case DEPTH:
                if (position <= -1) {
                    // page is off-screen to the left
                    view.setAlpha(0);
                    view.setVisibility(View.INVISIBLE);
                } else if (position <= 0) { // between -1 and 0
                    // use the default slide transition when moving to the left page
                    view.setAlpha(1);
                    view.setTranslationX(0);
                    view.setScaleX(1);
                    view.setScaleY(1);
                    view.setVisibility(View.VISIBLE);
                } else if (position <= 1) { // between 0 and 1
                    // fade the page out
                    view.setAlpha(1 - position);

                    // counteract the default slide transition
                    int pageWidth = view.getWidth();
                    view.setTranslationX(pageWidth * -position);

                    // scale the page down (between MIN_SCALE and 1)
                    float scaleFactor =
                            MIN_SCALE_DEPTH + (1 - MIN_SCALE_DEPTH) * (1 - Math.abs(position));
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                    if (position == 1) {
                        view.setVisibility(View.INVISIBLE);
                    } else {
                        view.setVisibility(View.VISIBLE);
                    }
                } else {
                    // page is off-screen to the right
                    view.setAlpha(0);
                    view.setVisibility(View.INVISIBLE);
                }
                break;

            case ZOOM:
                if (position < -1) {
                    // page is way off-screen to the left.
                    view.setAlpha(0);
                } else if (position <= 1) {
                    // modify the default slide transition to shrink the page as well
                    int pageWidth = view.getWidth();
                    int pageHeight = view.getHeight();

                    float scaleFactor = Math.max(MIN_SCALE_ZOOM, 1 - Math.abs(position));
                    float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                    float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                    if (position < 0) {
                        view.setTranslationX(horzMargin - vertMargin / 2);
                    } else {
                        view.setTranslationX(-horzMargin + vertMargin / 2);
                    }

                    // scale the page down (between MIN_SCALE and 1)
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                    // fade the page relative to its size.
                    view.setAlpha(MIN_ALPHA_ZOOM +
                            (scaleFactor - MIN_SCALE_ZOOM) /
                                    (1 - MIN_SCALE_ZOOM) * (1 - MIN_ALPHA_ZOOM));

                } else {
                    // This page is way off-screen to the right.
                    view.setAlpha(0);
                }
                break;
        }
    }
}
