package org.wordpress.android.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.ArrayRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TODO: comment header explaining how to use this
 */

public class WPPrefView extends LinearLayout implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public enum PrefType {
        TEXT,
        TOGGLE,
        CHECKLIST,
        RADIOLIST;

        public static PrefType fromInt(int value) {
            switch (value) {
                case 1:
                    return TOGGLE;
                case 2:
                    return CHECKLIST;
                case 3:
                    return RADIOLIST;
                default:
                    return TEXT;
            }
        }
    }

    private PrefType mPrefType = PrefType.TEXT;
    private final List<String> mListEntries = new ArrayList<>();
    private final List<String> mListValues = new ArrayList<>();

    private ViewGroup mContainer;
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WPPrefView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        ViewGroup view = (ViewGroup) inflate(context, R.layout.wppref_view, this);

        mHeadingTextView = (TextView) view.findViewById(R.id.text_heading);
        mContainer = (ViewGroup) view.findViewById(R.id.container);
        mTitleTextView = (TextView) mContainer.findViewById(R.id.text_title);
        mSummaryTextView = (TextView) mContainer.findViewById(R.id.text_summary);
        mSwitch = (Switch) view.findViewById(R.id.switch_view);
        mDivider = view.findViewById(R.id.divider);

        mContainer.setOnClickListener(this);
        mSwitch.setOnCheckedChangeListener(this);

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
        boolean isToggle = mPrefType == PrefType.TOGGLE;
        mContainer.setVisibility(isToggle ? View.GONE : View.VISIBLE);
        mSwitch.setVisibility(isToggle ? View.VISIBLE : View.GONE);
    }

    public void setHeading(String heading) {
        mHeadingTextView.setText(heading);
        mHeadingTextView.setVisibility(TextUtils.isEmpty(heading) ? GONE : VISIBLE);
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
        mSwitch.setText(title);
    }

    public void setSummary(String summary) {
        mSummaryTextView.setText(summary);
        mSummaryTextView.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
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

    public void setListEntries(@NonNull List<String> entries) {
        mListEntries.clear();
        mListEntries.addAll(entries);
    }

    public void setListEntries(@ArrayRes int arrayResourceId) {
        String[] array = getResources().getStringArray(arrayResourceId);
        setListEntries(Arrays.asList(array));
    }

    public void setListValues(@NonNull List<String> values) {
        mListEntries.clear();
        mListEntries.addAll(values);
    }

    public void setListValues(@ArrayRes int arrayResourceId) {
        String[] array = getResources().getStringArray(arrayResourceId);
        setListValues(Arrays.asList(array));
    }

    @Override
    public void onClick(View v) {
        switch (mPrefType) {
            case CHECKLIST:
                showChecklistDialog();
                break;
            case RADIOLIST:
                showRadiolistDialog();
                break;
            case TEXT:
                showTextDialog();
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO: switch has been toggled
    }


    private static CharSequence[] listToArray(@NonNull List<String> list) {
        CharSequence[] array = new CharSequence[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    private void showTextDialog() {
        final EditText editText = new EditText(getContext());
        editText.setText(mSummaryTextView.getText());

        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setView(editText)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
        .show();
    }

    private void showChecklistDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO
                        SparseBooleanArray checkedItems =
                                ((AlertDialog) dialog).getListView().getCheckedItemPositions();
                    }
                })
                .setMultiChoiceItems(listToArray(mListEntries), null, null)
                .show();
    }

    private void showRadiolistDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle(mTitleTextView.getText())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO
                    }
                })
                .setSingleChoiceItems(listToArray(mListEntries), 0, null)
                .show();
    }
}
