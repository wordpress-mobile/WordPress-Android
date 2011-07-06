package com.jjoe64.graphview.compatible;

import android.content.Context;
import android.view.ScaleGestureDetector;

public class RealScaleGestureDetector extends ScaleGestureDetector {
	public RealScaleGestureDetector(Context context, final com.jjoe64.graphview.compatible.ScaleGestureDetector fakeScaleGestureDetector, final com.jjoe64.graphview.compatible.ScaleGestureDetector.SimpleOnScaleGestureListener fakeListener) {
		super(context, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				return fakeListener.onScale(fakeScaleGestureDetector);
			}
		});
	}
}
