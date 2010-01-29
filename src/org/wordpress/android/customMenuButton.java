package org.wordpress.android;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class customMenuButton extends ImageButton{

	public customMenuButton(Context context) {
	super(context);
	}

	public customMenuButton(Context context, AttributeSet attrs){
	super(context, attrs);
	}

	@Override
	protected void onDraw(Canvas canvas) {
	//sets the button image based on whether the button in its pressed state
		if (isFocused()) 
		{
			setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_bg));
		} 
		else 
		{
			if (isPressed()){
				setBackgroundDrawable(getResources().getDrawable(R.drawable.menu_button_bg));
			}
			else{
				setBackgroundDrawable(null);
			}
			
		}
	super.onDraw(canvas);
	}

	}
