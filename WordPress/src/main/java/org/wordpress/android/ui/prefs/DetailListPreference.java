package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.TypefaceCache;

/**
 * Custom {@link ListPreference} used to display detail text per item.
 */

public class DetailListPreference extends ListPreference {
    private DetailListAdapter mListAdapter;
    private String[] mDetails;
    private int mSelectedIndex;

    public DetailListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DetailListPreference);

        Log.d("", "index count =" + array.getIndexCount());

        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.DetailListPreference_entryDetails) {
                int id = array.getResourceId(index, -1);
                if (id != -1) {
                    mDetails = array.getResources().getStringArray(id);
                } else {
                    mDetails = null;
                }
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
    protected void onPrepareDialogBuilder(@NonNull AlertDialog.Builder builder) {
        mListAdapter = new DetailListAdapter(getContext(), R.layout.detail_list_preference, mDetails);
        mSelectedIndex = findIndexOfValue(getValue());

        builder.setSingleChoiceItems(mListAdapter, mSelectedIndex,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mSelectedIndex != which) {
                            mSelectedIndex = which;
                            notifyChanged();
                        }
                        DetailListPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    }
                });
        builder.setPositiveButton(null, null);
        builder.setCustomTitle(View.inflate(getContext(), R.layout.detail_list_preference_title, null));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null && mSelectedIndex < entryValues.length) {
            String value = entryValues[mSelectedIndex].toString();
            if (callChangeListener(value)) {
                setValue(value);
            }
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

            if (radioButton != null && mSelectedIndex == position) {
                radioButton.setChecked(true);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (radioButton != null) {
                        radioButton.setChecked(true);
                    }
                    DetailListPreference.this.callChangeListener(getEntryValues()[position]);
                }
            });

            return convertView;
        }
    }
}
