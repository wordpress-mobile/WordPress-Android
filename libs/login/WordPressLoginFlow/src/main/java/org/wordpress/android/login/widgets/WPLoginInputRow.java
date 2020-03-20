package org.wordpress.android.login.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

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

    private ImageView mIcon;
    private TextInputLayout mTextInputLayout;
    private EditText mEditText;

    public ImageView getIcon() {
        return mIcon;
    }

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

        mIcon = findViewById(R.id.icon);
        mTextInputLayout = findViewById(R.id.input_layout);
        mEditText = findViewById(R.id.input);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.wpLoginInputRow, 0, 0);

            try {
                if (a.hasValue(R.styleable.wpLoginInputRow_wpIconDrawable)) {
                    int iconResId = a.getResourceId(R.styleable.wpLoginInputRow_wpIconDrawable,
                            R.drawable.ic_user_grey_24dp);
                    int tintResId = a.getResourceId(R.styleable.wpLoginInputRow_wpIconDrawableTint,
                            R.color.login_input_icon_color);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, tintResId)));
                        mIcon.setImageResource(iconResId);
                    } else {
                        Drawable drawable = context.getResources().getDrawable(iconResId);
                        DrawableCompat.setTint(drawable, context.getResources().getColor(tintResId));
                        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                        mIcon.setImageDrawable(drawable);
                    }

                    mIcon.setVisibility(View.VISIBLE);
                } else {
                    mIcon.setVisibility(View.GONE);
                }

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
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_passwordToggleEnabled)) {
                    mTextInputLayout.setPasswordVisibilityToggleEnabled(
                            a.getBoolean(R.styleable.wpLoginInputRow_passwordToggleEnabled, false));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_passwordToggleTint)) {
                    mTextInputLayout.setPasswordVisibilityToggleTintList(
                            a.getColorStateList(R.styleable.wpLoginInputRow_passwordToggleTint));
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    if (a.hasValue(R.styleable.wpLoginInputRow_android_textAlignment)) {
                        mEditText.setTextAlignment(
                                a.getInt(R.styleable.wpLoginInputRow_android_textAlignment, TEXT_ALIGNMENT_GRAVITY));
                    }
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
            state = restoreViewsState((SavedState) bundle.getParcelable(KEY_SUPER_STATE));
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
