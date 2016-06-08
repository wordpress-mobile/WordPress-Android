package org.wordpress.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;

public class SlidingScrollView extends ScrollView {

    private float mXFraction = 0;

    public SlidingScrollView(Context context) {
        super(context);
    }

    public SlidingScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private ViewTreeObserver.OnPreDrawListener mPreDrawListener = null;

    // This property is used by the objectAnimator for Fragment slide animations. ex: `fragment_slide_in_from_right.xml`
    @SuppressWarnings("UnusedDeclaration")
    public float getXFraction() {
        return mXFraction;
    }

    // This implementation fixes the first frame not being translated: http://trickyandroid.com/fragments-translate-animation/
    @SuppressWarnings("UnusedDeclaration")
    public void setXFraction(float fraction) {
        mXFraction = fraction;

        if (getWidth() == 0) {
            if (mPreDrawListener == null) {
                mPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
                        setXFraction(mXFraction);
                        return true;
                    }
                };
                getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
            }
            return;
        }

        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }
}
