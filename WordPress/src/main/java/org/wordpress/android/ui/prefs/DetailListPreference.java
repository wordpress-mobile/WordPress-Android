package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.ListPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Custom {@link ListPreference} used to display detail text per item.
 */

public class DetailListPreference extends ListPreference
        implements PreferenceHint {
    private static final String STATE_SELECTED_INDEX = "DetailListPreference_STATE_SELECTED_INDEX";
    public boolean canShowDialog = true;
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
        if (!canShowDialog) {
            return;
        }
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
        if (state != null && state.containsKey(STATE_SELECTED_INDEX)) {
            mSelectedIndex = state.getInt(STATE_SELECTED_INDEX);
        } else {
            mSelectedIndex = findIndexOfValue(mStartingValue);
        }

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
        mDialog.setOnDismissListener(this);

        if (state != null) {
            mDialog.onRestoreInstanceState(state);
        }

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
            // if this happens it might also mean we changed the actual entries, let's check
            if (mDetails == null) {
                mListAdapter.clear();
                mListAdapter.notifyDataSetChanged();
            } else if (mDetails.length != mListAdapter.getCount()
                       || !Arrays.equals(mDetails, mListAdapter.getItems())) {
                mListAdapter.clear();
                mListAdapter.addAll(mDetails);
                mListAdapter.notifyDataSetChanged();
            }
        }
    }

    public void setDetails(String[] details) {
        mDetails = details;
        refreshAdapter();
    }

    // region adapted from DialogPreference to make sure dialog state behaves correctly, specially in system initiated
    // death/recreation scenarios, such as changing system dark mode.
    private void dismissDialog() {
        if (mDialog == null || !mDialog.isShowing()) {
            return;
        }

        mDialog.dismiss();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mDialog == null || !mDialog.isShowing()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isDialogShowing = true;
        myState.dialogBundle = mDialog.onSaveInstanceState();
        myState.dialogBundle.putInt(STATE_SELECTED_INDEX, mSelectedIndex);

        // Since dialog is showing, let's dismiss it so it doesn't leak. This is not the best place to do it, but
        // since the android.preference is deprecated we are not able to register the proper lifecycle listeners, and
        // we should migrate to androidx.preference or something similar in the future.
        dismissDialog();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isDialogShowing) {
            showDialog(myState.dialogBundle);
        }
    }

    private static class SavedState extends BaseSavedState {
        public static final @NonNull Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
        public boolean isDialogShowing;
        public Bundle dialogBundle;

        SavedState(Parcel source) {
            super(source);
            isDialogShowing = source.readInt() == 1;
            dialogBundle = source.readBundle(getClass().getClassLoader());
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isDialogShowing ? 1 : 0);
            dest.writeBundle(dialogBundle);
        }
    }
    // endregion

    private class DetailListAdapter extends ArrayAdapter<String> {
        DetailListAdapter(Context context, int resource, String[] objects) {
            super(context, resource, new ArrayList<>(Arrays.asList(objects)));
        }

        @NonNull
        @Override
        public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
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

        String[] getItems() {
            String[] items = new String[getCount()];
            for (int i = 0; i < getCount(); i++) {
                items[i] = getItem(i);
            }
            return items;
        }
    }
}
