package org.wordpress.android.login.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputLayout;

import org.wordpress.android.login.R;

/**
 * Compound view composed of an icon and an EditText
 */
public class WPLoginInputRow extends RelativeLayout {
    private static final String KEY_SUPER_STATE = "wplogin_input_row_super_state";

    public interface OnEditorCommitListener {
        void onEditorCommit();
    }

    private TextInputLayout mTextInputLayout;
    private EditText mEditText;

    public EditText getEditText() {
        return mEditText;
    }

    public WPLoginInputRow(Context context) {
        super(context);
        init(context, null);
    }

    public WPLoginInputRow(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public WPLoginInputRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflate(context, R.layout.login_input_row, this);

        mTextInputLayout = findViewById(R.id.input_layout);
        mEditText = findViewById(R.id.input);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.wpLoginInputRow, 0, 0);

            try {
                if (a.hasValue(R.styleable.wpLoginInputRow_android_inputType)) {
                    mEditText.setInputType(a.getInteger(R.styleable.wpLoginInputRow_android_inputType, 0));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_imeOptions)) {
                    mEditText.setImeOptions(a.getInteger(R.styleable.wpLoginInputRow_android_imeOptions, 0));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_hint)) {
                    String hint = a.getString(R.styleable.wpLoginInputRow_android_hint);
                    mTextInputLayout.setHint(hint);
                    mEditText.setHint(hint);
                    // Makes the hint transparent, so the TalkBack can read it, when the field is prefilled
                    mEditText.setHintTextColor(getResources().getColor(android.R.color.transparent));

                    // Passes autofill hints values forward to child views
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (isImportantForAutofill()) {
                            mEditText.setAutofillHints(getAutofillHints());
                        }
                    }
                }
                if (a.hasValue(R.styleable.wpLoginInputRow_passwordToggleEnabled)) {
                    mTextInputLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
                    mTextInputLayout.setEndIconDrawable(R.drawable.selector_password_visibility);
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_textAlignment)) {
                    mEditText.setTextAlignment(
                            a.getInt(R.styleable.wpLoginInputRow_android_textAlignment, TEXT_ALIGNMENT_GRAVITY));
                }
            } finally {
                a.recycle();
            }
        }
    }


    /**
     * Save the Views state manually so multiple instances of the compound View can exist in the same layout.
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        Parcelable editTextState = mEditText.onSaveInstanceState();
        bundle.putParcelable(KEY_SUPER_STATE, new SavedState(super.onSaveInstanceState(), editTextState));
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            state = restoreViewsState(bundle.getParcelable(KEY_SUPER_STATE));
        }

        super.onRestoreInstanceState(state);
    }

    private Parcelable restoreViewsState(SavedState state) {
        mEditText.onRestoreInstanceState(state.mEditTextState);
        return state.getSuperState();
    }

    /**
     * Disable the auto-save feature, since the Views state is saved manually.
     */
    @Override
    protected void dispatchSaveInstanceState(SparseArray container) {
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray container) {
        super.dispatchThawSelfOnly(container);
    }


    public void addTextChangedListener(TextWatcher watcher) {
        mEditText.addTextChangedListener(watcher);
    }

    public void setOnEditorCommitListener(final OnEditorCommitListener listener) {
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT
                    || (event != null
                        && event.getAction() == KeyEvent.ACTION_UP
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    listener.onEditorCommit();
                }

                // always consume the event so the focus stays in the EditText
                return true;
            }
        });
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener l) {
        mEditText.setOnEditorActionListener(l);
    }

    public final void setText(CharSequence text) {
        mEditText.setText(text);
    }

    public void setError(@Nullable final CharSequence error) {
        mTextInputLayout.setError(error);
        if (error == null) {
            mTextInputLayout.setErrorEnabled(false);
        }
    }

    private static class SavedState extends BaseSavedState {
        private Parcelable mEditTextState;

        SavedState(Parcelable superState, Parcelable editTextState) {
            super(superState);
            this.mEditTextState = editTextState;
        }

        SavedState(Parcel in) {
            super(in);
            mEditTextState = in.readParcelable(Parcelable.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(mEditTextState, 0);
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
