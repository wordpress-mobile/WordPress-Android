package org.wordpress.android.login.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.login.R;
import org.wordpress.android.util.ViewUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compound view composed of an icon and an EditText
 */
public class WPLoginInputRow extends RelativeLayout {
    public interface OnEditorCommitListener {
        void onEditorCommit();
    }

    private ImageView mIcon;
    private TextInputLayout mTextInputLayout;
    private EditText mEditText;

    private List<Integer> mNewIds;

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

        mIcon = (ImageView) findViewById(R.id.icon);
        mTextInputLayout = (TextInputLayout) findViewById(R.id.input_layout);
        mEditText = (EditText) findViewById(R.id.input);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.wpLoginInputRow, 0, 0);

            try {
                if (a.hasValue(R.styleable.wpLoginInputRow_wpIconDrawable)) {
                    mIcon.setImageResource(a.getResourceId(R.styleable.wpLoginInputRow_wpIconDrawable, 0));
                    mIcon.setVisibility(View.VISIBLE);
                } else {
                    mIcon.setVisibility(View.GONE);
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_hint)) {
                    mTextInputLayout.setHint(a.getString(R.styleable.wpLoginInputRow_android_hint));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_passwordToggleEnabled)) {
                    mTextInputLayout.setPasswordVisibilityToggleEnabled(
                            a.getBoolean(R.styleable.wpLoginInputRow_passwordToggleEnabled, false));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_passwordToggleTint)) {
                    mTextInputLayout.setPasswordVisibilityToggleTintList(
                            a.getColorStateList(R.styleable.wpLoginInputRow_passwordToggleTint));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_inputType)) {
                    mEditText.setInputType(a.getInteger(R.styleable.wpLoginInputRow_android_inputType, 0));
                }

                if (a.hasValue(R.styleable.wpLoginInputRow_android_imeOptions)) {
                    mEditText.setImeOptions(a.getInteger(R.styleable.wpLoginInputRow_android_imeOptions, 0));
                }
            } finally {
                a.recycle();
            }
        }

        mNewIds = Arrays.asList(ViewUtils.generateViewId(), ViewUtils.generateViewId(), ViewUtils.generateViewId());

        reassignIds();
    }

    /**
     * Assign new IDs to the Views so multiple instances of the compound View can exist in the same layout and
     *  auto-save of the Views state can be performed
     */
    private void reassignIds() {
        RelativeLayout.LayoutParams iconLayoutParams = (LayoutParams) mIcon.getLayoutParams();
        int[] rules = iconLayoutParams.getRules();
        for (int i = 0; i < rules.length; i++) {
            if (rules[i] == mTextInputLayout.getId()) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    rules[i] = mNewIds.get(1);
                } else {
                    iconLayoutParams.addRule(i, mNewIds.get(1));
                }
            }
        }
        mIcon.setLayoutParams(iconLayoutParams);

        RelativeLayout.LayoutParams editTextLayoutParams = (LayoutParams) mTextInputLayout.getLayoutParams();
        rules = editTextLayoutParams.getRules();
        for (int i = 0; i < rules.length; i++) {
            if (rules[i] == mIcon.getId()) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    rules[i] = mNewIds.get(0);
                } else {
                    editTextLayoutParams.addRule(i, mNewIds.get(0));
                }
            }
        }
        mTextInputLayout.setLayoutParams(editTextLayoutParams);

        mIcon.setId(mNewIds.get(0));
        mTextInputLayout.setId(mNewIds.get(1));
        mEditText.setId(mNewIds.get(2));
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mNewIds);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mNewIds = savedState.mIds;

        reassignIds();
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
        private List<Integer> mIds;

        SavedState(Parcelable superState, List<Integer> ids) {
            super(superState);
            mIds = ids;
        }

        SavedState(Parcel in) {
            super(in);

            mIds = new ArrayList<>();
            in.readList(mIds, List.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(mIds);
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
