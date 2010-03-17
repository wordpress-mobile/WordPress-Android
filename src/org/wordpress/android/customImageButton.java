package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class customImageButton extends ImageButton{

	public customImageButton(Context context) {
	super(context);
	}

	public customImageButton(Context context, AttributeSet attrs){
	super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
	//sets the button image based on whether the button in its pressed state
		if (isFocused()) 
		{
			setBackgroundDrawable(getResources().getDrawable(android.R.drawable.btn_default));
		} 
		else 
		{
			setBackgroundDrawable(getResources().getDrawable(isPressed()?android.R.drawable.btn_default : android.R.drawable.btn_default));
		}
	super.onDraw(canvas);
	}

	}
