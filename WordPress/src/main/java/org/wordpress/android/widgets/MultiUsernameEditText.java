package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.google.android.material.textfield.TextInputEditText;

/**
 * Used to handle backspace in People Management username field
 */
public class MultiUsernameEditText extends TextInputEditText {
    private OnBackspacePressedListener mOnBackspacePressedListener;


    public MultiUsernameEditText(Context context) {
        super(context);
    }

    public MultiUsernameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiUsernameEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnBackspacePressedListener(OnBackspacePressedListener onBackspacePressedListener) {
        this.mOnBackspacePressedListener = onBackspacePressedListener;
    }

    public interface OnBackspacePressedListener {
        boolean onBackspacePressed();
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        // in this case it makes sense to not change EditText to fullscreen mode at landscape
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        return new MultiUsernameEditTextInputConnection(this, false);
    }


    private class MultiUsernameEditTextInputConnection extends BaseInputConnection {
        MultiUsernameEditTextInputConnection(View targetView, boolean fullEditor) {
            super(targetView, fullEditor);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                if (mOnBackspacePressedListener != null) {
                    // if username was not deleted pass event to parent method and return the result
                    return !mOnBackspacePressedListener.onBackspacePressed() && super.sendKeyEvent(event);
                }
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                       && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }
}
