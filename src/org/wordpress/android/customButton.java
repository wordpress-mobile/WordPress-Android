package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.Button;

public class customButton extends Button{

	public customButton(Context context) {
	super(context);
	}

	public customButton(Context context, AttributeSet attrs){
	super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		
		if (isFocused()) 
		{
			setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_default_selected));
		} 
		else 
		{
			setBackgroundDrawable(getResources().getDrawable(isPressed()?R.drawable.btn_default_selected : R.drawable.btn_default_normal));
		}
		
	//sets the button image based on whether the button in its pressed state
	//
	super.onDraw(canvas);
	}

	}
