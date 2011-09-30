package org.wordpress.android.util;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class WPEditText extends EditText {

	private EditTextImeBackListener mOnImeBack;

	public WPEditText(Context context) {
		super(context);
	}

	public WPEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public WPEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_UP) {
			clearFocus();
			if (mOnImeBack != null)
				mOnImeBack.onImeBack(this, this.getText().toString());
		}
		return super.dispatchKeyEvent(event);
	}

	public void setOnEditTextImeBackListener(EditTextImeBackListener listener) {
		mOnImeBack = listener;
	}
	
	public interface EditTextImeBackListener {

		public abstract void onImeBack(WPEditText ctrl, String text);
	}

}

