package org.wordpress.android.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */

public class WPPrefView extends LinearLayout {

    public enum PrefType {
        TEXT,
        TOGGLE,
        CHOOSER;

        public static PrefType fromInt(int value) {
            switch (value) {
                case 1:
                    return TOGGLE;
                case 2:
                    return CHOOSER;
                default:
                    return TEXT;
            }
        }
    }

    private PrefType mPrefType = PrefType.TEXT;
    private final List<String> mChoices = new ArrayList<>();

    private TextView mHeadingTextView;
    private TextView mTitleTextView;
    private TextView mSummaryTextView;
    private View mDivider;
    private Switch mSwitch;


    public WPPrefView(Context context) {
        super(context);
        initView(context, null);
    }

    public WPPrefView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        ViewGroup view = (ViewGroup) inflate(context, R.layout.wppref_view, this);
        mHeadingTextView = (TextView) view.findViewById(R.id.text_heading);
        mTitleTextView = (TextView) view.findViewById(R.id.text_title);
        mSummaryTextView = (TextView) view.findViewById(R.id.text_summary);
        mSwitch = (Switch) view.findViewById(R.id.switch_view);
        mDivider = view.findViewById(R.id.divider);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.wpPrefView,
                    0, 0);
            try {
                int prefTypeInt = a.getInteger(R.styleable.wpPrefView_wpPrefType, 0);
                String heading = a.getString(R.styleable.wpPrefView_wpHeading);
                String title = a.getString(R.styleable.wpPrefView_wpTitle);
                String summary = a.getString(R.styleable.wpPrefView_wpSummary);
                boolean showDivider = a.getBoolean(R.styleable.wpPrefView_wpShowDivider, true);

                setPrefType(PrefType.fromInt(prefTypeInt));
                setHeading(heading);
                setTitle(title);
                setSummary(summary);
                setShowDivider(showDivider);
            } finally {
                a.recycle();
            }
        }
    }

    public PrefType getPrefType() {
        return mPrefType;
    }

    public void setPrefType(@NonNull PrefType prefType) {
        mPrefType = prefType;
    }

    public void setHeading(String heading) {
        mHeadingTextView.setText(heading);
        mHeadingTextView.setVisibility(TextUtils.isEmpty(heading) ? GONE : VISIBLE);
    }
    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    public void setSummary(String summary) {
        mSummaryTextView.setText(summary);
        mSwitch.setText(summary);

        boolean isEmpty = TextUtils.isEmpty(summary);
        boolean isToggle = mPrefType == PrefType.TOGGLE;

        mSummaryTextView.setVisibility(!isEmpty && !isToggle ? VISIBLE : GONE);
        mSwitch.setVisibility(!isEmpty && isToggle ? VISIBLE : GONE);
    }

    public boolean isChecked() {
        return mPrefType == PrefType.TOGGLE && mSwitch.isChecked();
    }

    public void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }

    public void setShowDivider(boolean show) {
        mDivider.setVisibility(show ? VISIBLE : GONE);
    }

    public void setChoices(@NonNull List<String> choices) {
        mChoices.clear();
        mChoices.addAll(choices);
    }

}
