package org.wordpress.android.ui.prefs;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.TypefaceCache;

/**
 * Custom {@link ListPreference} used to display detail text per item.
 */

public class DetailListPreference extends ListPreference
        implements SiteSettingsFragment.HasHint {
    private DetailListAdapter mListAdapter;
    private String[] mDetails;
    private int mStartingIndex;
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
                } else {
                    mDetails = null;
                }
            } else if (index == R.styleable.DetailListPreference_longClickHint) {
                mHint = array.getString(index);
            }
        }

        array.recycle();
    }

    public DetailListPreference(Context context) {
        super(context);

        mSelectedIndex = 0;
        mDetails = null;
        setLayoutResource(R.layout.detail_list_preference);
    }

    @Override
    protected void onBindView(@NonNull View view) {
        super.onBindView(view);

        Resources res = getContext().getResources();
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        Typeface font = TypefaceCache.getTypeface(getContext(),
                TypefaceCache.FAMILY_OPEN_SANS,
                Typeface.NORMAL,
                TypefaceCache.VARIATION_NORMAL);

        if (titleView != null) {
            if (isEnabled()) {
                titleView.setTextColor(res.getColor(R.color.grey_dark));
            } else {
                titleView.setTextColor(res.getColor(R.color.grey_lighten_10));
            }
            titleView.setTextSize(16);
            titleView.setTypeface(font);
        }

        if (summaryView != null) {
            if (isEnabled()) {
                summaryView.setTextColor(res.getColor(R.color.grey_darken_10));
            } else {
                summaryView.setTextColor(res.getColor(R.color.grey_lighten_10));
            }
            summaryView.setTextSize(14);
            summaryView.setTypeface(font);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        Context context = getContext();
        Resources res = context.getResources();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Calypso_AlertDialog);

        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mWhichButtonClicked = which;
            }
        });
        builder.setNegativeButton(res.getString(R.string.cancel).toUpperCase(), this);

        mListAdapter = new DetailListAdapter(getContext(), R.layout.detail_list_preference, mDetails);
        mStartingIndex = mSelectedIndex = findIndexOfValue(getValue());

        builder.setSingleChoiceItems(mListAdapter, mSelectedIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectedIndex != which) {
                            mSelectedIndex = which;
                            mListAdapter.notifyDataSetChanged();
                            setValue(getEntryValues()[mSelectedIndex].toString());
                            notifyChanged();
                        }
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

        mDialog = builder.create();

        if (mDialog != null) {
            Button positive = mDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negative = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            Typeface typeface = TypefaceCache.getTypeface(getContext(),
                    TypefaceCache.FAMILY_OPEN_SANS,
                    Typeface.BOLD,
                    TypefaceCache.VARIATION_NORMAL);

            if (positive != null) {
                positive.setTextColor(res.getColor(R.color.blue_medium));
                positive.setTypeface(typeface);
            }

            if (negative != null) {
                negative.setTextColor(res.getColor(R.color.blue_medium));
                negative.setTypeface(typeface);
            }

            if (state != null) {
                mDialog.onRestoreInstanceState(state);
            }
            mDialog.setOnDismissListener(this);
            mDialog.show();
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
        CharSequence[] values = getEntryValues();
        if (values != null && mSelectedIndex < values.length && mSelectedIndex != mStartingIndex) {
            String value = values[positiveResult ? mSelectedIndex : mStartingIndex].toString();
            setValue(value);
            callChangeListener(value);
        }
    }

    @Override
    public boolean hasHint() {
        return !TextUtils.isEmpty(mHint);
    }

    @Override
    public String getHintText() {
        return mHint;
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

            if (mainText != null && position < getEntries().length) {
                mainText.setText(getEntries()[position]);
                mainText.setTypeface(TypefaceCache.getTypeface(getContext(),
                        TypefaceCache.FAMILY_OPEN_SANS,
                        Typeface.NORMAL,
                        TypefaceCache.VARIATION_NORMAL));
            }

            if (detailText != null && position < mDetails.length) {
                detailText.setText(mDetails[position]);
                detailText.setTypeface(TypefaceCache.getTypeface(getContext(),
                        TypefaceCache.FAMILY_OPEN_SANS,
                        Typeface.NORMAL,
                        TypefaceCache.VARIATION_LIGHT));
            }

            if (radioButton != null) {
                radioButton.setChecked(mSelectedIndex == position);
                radioButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeSelection(radioButton, position);
                    }
                });
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    changeSelection(radioButton, position);
                }
            });

            return convertView;
        }

        private void changeSelection(RadioButton radioButton, int position) {
            CharSequence[] values = getEntryValues();

            if (radioButton != null && mSelectedIndex != position &&
                    values != null && position < values.length) {
                mSelectedIndex = position;
                radioButton.setChecked(true);
                callChangeListener(values[position]);
            }
        }
    }
}
