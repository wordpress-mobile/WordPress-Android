package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.ui.utils.UiHelpers;

import java.util.Locale;

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
        int topOffset = res.getDimensionPixelOffset(R.dimen.settings_fragment_dialog_vertical_inset);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(context)
                .setBackgroundInsetTop(topOffset)
                .setBackgroundInsetBottom(topOffset);

        mWhichButtonClicked = DialogInterface.BUTTON_NEGATIVE;
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(res.getString(android.R.string.cancel).toUpperCase(Locale.getDefault()), this);

        if (mDetails == null) {
            mDetails = new String[getEntries() == null ? 1 : getEntries().length];
        }

        mListAdapter = new DetailListAdapter(getContext(), R.layout.detail_list_preference, mDetails);
        mStartingValue = getValue();
        mSelectedIndex = findIndexOfValue(mStartingValue);

        builder.setSingleChoiceItems(mListAdapter, mSelectedIndex,
                (dialog, which) -> mSelectedIndex = which);

        View titleView = View.inflate(getContext(), R.layout.detail_list_preference_title, null);

        if (titleView != null) {
            TextView titleText = titleView.findViewById(R.id.title);
            if (titleText != null) {
                titleText.setText(getTitle());
            }

            builder.setCustomTitle(titleView);
        } else {
            builder.setTitle(getTitle());
        }

        mDialog = builder.create();

        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }
        mDialog.setOnDismissListener(this);
        mDialog.show();

        ListView listView = mDialog.getListView();

        if (listView != null) {
            listView.setDividerHeight(0);
            listView.setClipToPadding(true);
            listView.setPadding(0, 0, 0, res.getDimensionPixelSize(R.dimen.site_settings_divider_height));
        }

        UiHelpers.Companion.adjustDialogSize(mDialog);
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

    public void remove(int index) {
        if (index < 0 || index >= mDetails.length) {
            return;
        }

        mDetails = ArrayUtils.remove(mDetails, index);
        mListAdapter = new DetailListAdapter(getContext(), R.layout.detail_list_preference, mDetails);
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
        DetailListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, objects);
        }

        @NotNull
        @Override
        public View getView(final int position, View convertView, @NotNull ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), R.layout.detail_list_preference, null);
            }

            final RadioButton radioButton = convertView.findViewById(R.id.radio);
            TextView mainText = convertView.findViewById(R.id.main_text);
            TextView detailText = convertView.findViewById(R.id.detail_text);

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
            }

            convertView.setOnClickListener(v -> changeSelection(position));

            return convertView;
        }

        private void changeSelection(int position) {
            mSelectedIndex = position;
            notifyDataSetChanged();
        }
    }
}
