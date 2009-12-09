package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.Button;
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
			setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_default_selected));
		} 
		else 
		{
			setBackgroundDrawable(getResources().getDrawable(isPressed()?R.drawable.btn_default_selected : R.drawable.btn_default_normal));
		}
	super.onDraw(canvas);
	}

	}
