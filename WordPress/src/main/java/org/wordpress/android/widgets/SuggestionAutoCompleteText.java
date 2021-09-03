package org.wordpress.android.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.appcompat.widget.AppCompatMultiAutoCompleteTextView;

import org.wordpress.android.ui.suggestion.util.SuggestionTokenizer;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.persistentedittext.PersistentEditTextHelper;

public class SuggestionAutoCompleteText extends AppCompatMultiAutoCompleteTextView {
    PersistentEditTextHelper mPersistentEditTextHelper;
    private OnEditTextBackListener mBackListener;
    private SuggestionTokenizer mSuggestionTokenizer;
    private char mPrefix;

    public interface OnEditTextBackListener {
        void onEditTextBack();
    }

    public SuggestionAutoCompleteText(Context context) {
        super(context, null);
        init(context);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SuggestionAutoCompleteText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mPersistentEditTextHelper = new PersistentEditTextHelper(context);
        // When TYPE_TEXT_FLAG_AUTO_COMPLETE is set, autocorrection is disabled.
        setRawInputType(getInputType() & ~EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE);
    }

    public void initializeWithPrefix(char prefix) {
        mPrefix = prefix;
        mSuggestionTokenizer = new SuggestionTokenizer(mPrefix);
        setTokenizer(mSuggestionTokenizer);
        setThreshold(1);
    }

    @Override
    public boolean enoughToFilter() {
        Editable text = getText();

        int end = getSelectionEnd();
        if (end < 0) {
            return false;
        }

        int start = mSuggestionTokenizer.findTokenStart(text, end);

        return start > 0
               && (end - start >= 1
                             || (start == end && text.charAt(start - 1) == mPrefix));
    }

    public void forceFiltering(CharSequence text) {
        performFiltering(text, 0);
    }

    public PersistentEditTextHelper getAutoSaveTextHelper() {
        return mPersistentEditTextHelper;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().loadString(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getAutoSaveTextHelper().getUniqueId() == null) {
            return;
        }
        getAutoSaveTextHelper().saveString(this);
    }

    public void setOnBackListener(OnEditTextBackListener listener) {
        mBackListener = listener;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());

        // store the current Focused state
        savedState.mIsFocused = isFocused();

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        // if we were focused, setup a properly timed future request for focus
        if (savedState.mIsFocused) {
            // this OnLayoutChangeListener will self unregister upon running and it's there so we can properly time the
            // on-screen IME opening
            addOnLayoutChangeListener(mOneoffFocusRequest);
        }
    }

    private final OnLayoutChangeListener mOneoffFocusRequest = new OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop,
                                   int oldRight, int oldBottom) {
            // we're now at a good point in time to launch a focus request
            post(new Runnable() {
                @Override
                public void run() {
                    // self unregister so we won't auto-request focus again
                    removeOnLayoutChangeListener(mOneoffFocusRequest);

                    // request focus
                    setFocusableInTouchMode(true);
                    requestFocus();
                }
            });
        }
    };

    @Override
    public boolean performClick() {
        // make sure we are focusable otherwise we will not get focused
        setFocusableInTouchMode(true);
        requestFocus();

        return super.performClick();
    }

    /*
     * detect when user hits the back button while soft keyboard is showing (hiding the keyboard)
     */
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            // clear focus but stop being focusable first. This way we won't receive focus if we're the only focusable
            // widget on the page
            setFocusableInTouchMode(false);
            clearFocus();

            if (mBackListener != null) {
                mBackListener.onEditTextBack();
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        // if no hardware keys are present, associate being focused to having the on-screen keyboard visible
        if (!DeviceUtils.getInstance().hasHardwareKeyboard(getContext())) {
            InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);

            if (focused) {
                // show the on-screen keybpoard if we got focused
                inputMethodManager.showSoftInput(this, 0);
            } else {
                // stop being focusable so closing the keyboard won't focus us
                setFocusableInTouchMode(false);
                inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    /**
     * Local class for holding the EditBox's focused or not state
     */
    static class SavedState extends BaseSavedState {
        private boolean mIsFocused;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.mIsFocused = (in.readInt() == 1);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.mIsFocused ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
