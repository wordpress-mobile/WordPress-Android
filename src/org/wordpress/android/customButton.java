package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
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
			setBackgroundDrawable(getResources().getDrawable(android.R.drawable.btn_default));
		} 
		else 
		{
			setBackgroundDrawable(getResources().getDrawable(isPressed()?android.R.drawable.btn_default : android.R.drawable.btn_default));
		}
		
	//sets the button image based on whether the button in its pressed state

	super.onDraw(canvas);
	}

	}
