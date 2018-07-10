package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import org.wordpress.android.R;

/**
 * Perpetually animated quick start focus point (hint)
 * Consists of:
 * - Initial expand animation with bounce
 * 2 staggered animations on repeat:
 * - Collapse
 * - Expand
 */
public class QuickStartFocusPoint extends FrameLayout {
    public QuickStartFocusPoint(Context context) {
        super(context);
        initView();
    }

    public QuickStartFocusPoint(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public QuickStartFocusPoint(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        inflate(getContext(), R.layout.quick_start_focus_circle, this);

        final View outerCircle = findViewById(R.id.quick_start_focus_outer_circle);
        final View innerCircle = findViewById(R.id.quick_start_focus_inner_circle);

        Animation outerCircleInitialAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_initial_animation);
        Animation innerCircleInitialAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_initial_animation);

        final Animation outerCircleCollapseAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_collapse_animation);
        final Animation innerCircleCollapseAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_collapse_animation);

        final Animation innerCircleExpanAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_expand_animation);
        final Animation outerCircleExpanAnimation =
                AnimationUtils.loadAnimation(getContext(), R.anim.quick_start_circle_expand_animation);

        innerCircleInitialAnimation.setAnimationListener(new AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
            }

            @Override public void onAnimationEnd(Animation animation) {
                outerCircleCollapseAnimation.setStartOffset(1000);
                innerCircleCollapseAnimation.setStartOffset(1050);

                outerCircle.startAnimation(outerCircleCollapseAnimation);
                innerCircle.startAnimation(innerCircleCollapseAnimation);
            }

            @Override public void onAnimationRepeat(Animation animation) {
            }
        });


        innerCircleCollapseAnimation.setAnimationListener(new AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
            }

            @Override public void onAnimationEnd(Animation animation) {
                outerCircle.startAnimation(outerCircleExpanAnimation);
                innerCircle.startAnimation(innerCircleExpanAnimation);
            }

            @Override public void onAnimationRepeat(Animation animation) {
            }
        });


        innerCircleExpanAnimation.setAnimationListener(new AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
            }

            @Override public void onAnimationEnd(Animation animation) {
                outerCircleCollapseAnimation.setStartOffset(1000);
                innerCircleCollapseAnimation.setStartOffset(1050);

                outerCircle.startAnimation(outerCircleCollapseAnimation);
                innerCircle.startAnimation(innerCircleCollapseAnimation);
            }

            @Override public void onAnimationRepeat(Animation animation) {
            }
        });

        outerCircleInitialAnimation.setStartOffset(1000);
        outerCircle.startAnimation(outerCircleInitialAnimation);
        innerCircleInitialAnimation.setStartOffset(1050);
        innerCircle.startAnimation(innerCircleInitialAnimation);
    }
}
