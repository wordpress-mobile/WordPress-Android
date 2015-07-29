package org.wordpress.android.editor;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

import org.wordpress.android.util.AppLog;

/**
 * An EditText with support for {@link org.wordpress.android.editor.OnImeBackListener} and typeface setting
 * using a custom XML attribute.
 */
public class SourceViewEditText extends EditText {

    private OnImeBackListener mOnImeBackListener;

    public SourceViewEditText(Context context) {
        super(context);
    }

    public SourceViewEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomTypeface(attrs);
    }

    public SourceViewEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomTypeface(attrs);
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            if (this.mOnImeBackListener != null) {
                this.mOnImeBackListener.onImeBack();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    public void setOnImeBackListener(OnImeBackListener listener) {
        this.mOnImeBackListener = listener;
    }

    private void setCustomTypeface(AttributeSet attrs) {
        TypedArray values = getContext().obtainStyledAttributes(attrs, R.styleable.SourceViewEditText);
        String typefaceName = values.getString(R.styleable.SourceViewEditText_fontFile);
        if (typefaceName != null) {
            try {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + typefaceName);
                this.setTypeface(typeface);
            } catch (RuntimeException e) {
                AppLog.e(AppLog.T.EDITOR, "Could not load typeface " + typefaceName);
            }
        }
        values.recycle();
    }
}