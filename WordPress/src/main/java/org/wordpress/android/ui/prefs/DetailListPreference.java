package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPPrefUtils;

/**
 * Custom {@link ListPreference} used to display detail text per item.
 */

public class DetailListPreference extends ListPreference
        implements PreferenceHint {
    private DetailListAdapter mListAdapter;
    private String[] mDetails;
    private String mStartingValue;
    private int mSelectedIndex;
    private String mHint;
    private AlertDialog mDialog;
    private int mWhichButtonClicked;

    public DetailListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DetailListPreference);

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.DetailListPreference_entryDetails) {
                int id = array.getResourceId(index, -1);
                if (id != -1) {
                    mDetails = array.getResources().getStringArray(id);
                }
            } else if (index == R.styleable.DetailListPreference_longClickHint) {
                mHint = array.getString(index);
            }
        }

        array.recycle();

        mSelectedIndex = -1;
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        setupView((TextView) view.findViewById(android.R.id.title),
                R.dimen.text_sz_large, R.color.grey_dark, R.color.grey_lighten_10);
        setupView((TextView) view.findViewById(android.R.id.summary),
                R.dimen.text_sz_medium, R.color.grey_darken_10, R.color.grey_lighten_10);
    }

    @Override
    public CharSequence getEntry() {
        int index = findIndexOfValue(getValue());
        CharSequence[] entries = getEntries();

        if (entries != null && index >= 0 && index < entries.length) {
            return entries[index];
        }
        return null;
    }

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Calypso_AlertDialog);

        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(res.getString(R.string.cancel).toUpperCase(), this);

        if (mDetails == null) {
            mDetails = new String[getEntries() == null ? 1 : getEntries().length];
        }

        mListAdapter = new DetailListAdapter(getContext(), R.layout.detail_list_preference, mDetails);
        mStartingValue = getValue();
        mSelectedIndex = findIndexOfValue(mStartingValue);

        builder.setSingleChoiceItems(mListAdapter, mSelectedIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSelectedIndex = which;
                    }
                });

        View titleView = View.inflate(getContext(), R.layout.detail_list_preference_title, null);

        if (titleView != null) {
            TextView titleText = (TextView) titleView.findViewById(R.id.title);
            if (titleText != null) {
                titleText.setText(getTitle());
            }

            builder.setCustomTitle(titleView);
        } else {
            builder.setTitle(getTitle());
        }

        if ((mDialog = builder.create()) == null) return;

        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.setOnDismissListener(this);
        mDialog.show();

        ListView listView = mDialog.getListView();
        Button positive = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        Typeface typeface = WPPrefUtils.getSemiboldTypeface(getContext());

        if (listView != null) {
            listView.setDividerHeight(0);
            listView.setClipToPadding(true);
            listView.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.site_settings_divider_height));
        }

        if (positive != null) {
            //noinspection deprecation
            positive.setTextColor(res.getColor(R.color.blue_medium));
            positive.setTypeface(typeface);
        }

        if (negative != null) {
            //noinspection deprecation
            negative.setTextColor(res.getColor(R.color.blue_medium));
            negative.setTypeface(typeface);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mWhichButtonClicked = which;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        mDialog = null;
        onDialogClosed(mWhichButtonClicked == DialogInterface.BUTTON_POSITIVE);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        int index = positiveResult ? mSelectedIndex : findIndexOfValue(mStartingValue);
        CharSequence[] values = getEntryValues();
        if (values != null && index >= 0 && index < values.length) {
            String value = String.valueOf(values[index]);
            callChangeListener(value);
        }
    }

    @Override
    public boolean hasHint() {
        return !TextUtils.isEmpty(mHint);
    }

    @Override
    public String getHint() {
        return mHint;
    }

    @Override
    public void setHint(String hint) {
        mHint = hint;
    }

    public void refreshAdapter() {
        if (mListAdapter != null) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    public void setDetails(String[] details) {
        mDetails = details;
        refreshAdapter();
    }

    /**
     * Helper method to style the Preference screen view
     */
    private void setupView(TextView view, int sizeRes, int enabledColorRes, int disabledColorRes) {
        if (view != null) {
            Resources res = getContext().getResources();
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, res.getDimensionPixelSize(sizeRes));
            //noinspection deprecation
            view.setTextColor(res.getColor(isEnabled() ? enabledColorRes : disabledColorRes));
        }
    }

    private class DetailListAdapter extends ArrayAdapter<String> {
        public DetailListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.detail_list_preference, null);
            }

            final RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.radio);
            TextView mainText = (TextView) convertView.findViewById(R.id.main_text);
            TextView detailText = (TextView) convertView.findViewById(R.id.detail_text);

            if (mainText != null && getEntries() != null && position < getEntries().length) {
                mainText.setText(getEntries()[position]);
            }

            if (detailText != null) {
                if (mDetails != null && position < mDetails.length && !TextUtils.isEmpty(mDetails[position])) {
                    detailText.setVisibility(View.VISIBLE);
                    detailText.setText(mDetails[position]);
                } else {
                    detailText.setVisibility(View.GONE);
                }
            }

            if (radioButton != null) {
                radioButton.setChecked(mSelectedIndex == position);
                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeSelection(position);
                    }
                });
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSelection(position);
                }
            });

            return convertView;
        }

        private void changeSelection(int position) {
            mSelectedIndex = position;
            notifyDataSetChanged();
        }
    }
}
