/*
 * Sample FlowLayout wrote by Romain Guy: http://www.parleys.com/play/514892280364bc17fc56c0e2/chapter38/about
 * Fixed and tweaked since
 */

package org.wordpress.android.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import org.wordpress.android.R;

public class FlowLayout extends ViewGroup {
	private int mHorizontalSpacing;
	private int mVerticalSpacing;
	private Paint mPaint;

	public FlowLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout);
		try {
			mHorizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0);
			mVerticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0);
		} finally {
			a.recycle();
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthSize = MeasureSpec.getSize(widthMeasureSpec) - getPaddingRight() - getPaddingLeft();
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);

		boolean growHeight = widthMode != MeasureSpec.UNSPECIFIED;

		int width = 0;
		int height = getPaddingTop();

		int currentWidth = getPaddingLeft();
		int currentHeight = 0;

		boolean newLine = false;
		int spacing = 0;

		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			measureChild(child, widthMeasureSpec, heightMeasureSpec);

			LayoutParams lp = (LayoutParams) child.getLayoutParams();
		    spacing = mHorizontalSpacing;
			if (lp.horizontalSpacing >= 0) {
				spacing = lp.horizontalSpacing;
			}

			if (growHeight && currentWidth + child.getMeasuredWidth() > widthSize) {
				height += currentHeight + mVerticalSpacing;
				currentHeight = 0;
				width = Math.max(width, currentWidth - spacing);
				currentWidth = getPaddingLeft();
				newLine = true;
			} else {
				newLine = false;
			}

			lp.x = currentWidth;
			lp.y = height;

			currentWidth += child.getMeasuredWidth() + spacing;
			currentHeight = Math.max(currentHeight, child.getMeasuredHeight());
		}

		if (!newLine) {
			width = Math.max(width, currentWidth - spacing);
        }
		width += getPaddingRight();
		height += currentHeight + getPaddingBottom();

		setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(height, heightMeasureSpec));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			LayoutParams lp = (LayoutParams) child.getLayoutParams();
			child.layout(lp.x, lp.y, lp.x + child.getMeasuredWidth(), lp.y + child.getMeasuredHeight());
		}
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}
	
	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return new LayoutParams(p.width, p.height);
	}

	public static class LayoutParams extends ViewGroup.LayoutParams {
		int x;
		int y;
		
		public int horizontalSpacing;

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlowLayout_LayoutParams);
			try {
				horizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing, -1);
			} finally {
				a.recycle();
			}
		}

		public LayoutParams(int w, int h) {
			super(w, h);
		}
	}
}
