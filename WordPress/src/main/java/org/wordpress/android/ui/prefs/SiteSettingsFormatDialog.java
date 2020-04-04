package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.EditTextUtils;

/**
 * Custom date/time format dialog
 */

public class SiteSettingsFormatDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public enum FormatType {
        DATE_FORMAT,
        TIME_FORMAT;

        public String[] getEntries(@NonNull Context context) {
            if (this == FormatType.DATE_FORMAT) {
                return context.getResources().getStringArray(R.array.date_format_entries);
            } else {
                return context.getResources().getStringArray(R.array.time_format_entries);
            }
        }

        public String[] getValues(@NonNull Context context) {
            if (this == FormatType.DATE_FORMAT) {
                return context.getResources().getStringArray(R.array.date_format_values);
            } else {
                return context.getResources().getStringArray(R.array.time_format_values);
            }
        }
    }

    private static final String KEY_FORMAT_TYPE = "format_type";
    public static final String KEY_FORMAT_VALUE = "format_value";

    private String mFormatValue;
    private boolean mConfirmed;

    private EditText mEditCustomFormat;
    private RadioGroup mRadioGroup;

    private String[] mEntries;
    private String[] mValues;

    public static SiteSettingsFormatDialog newInstance(@NonNull FormatType formatType, @NonNull String formatValue) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_FORMAT_TYPE, formatType);
        args.putString(KEY_FORMAT_VALUE, formatValue);

        SiteSettingsFormatDialog dialog = new SiteSettingsFormatDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.site_settings_format_dialog, null);
        TextView txtTitle = view.findViewById(R.id.text_title);
        TextView txtHelp = view.findViewById(R.id.text_help);
        mEditCustomFormat = view.findViewById(R.id.edit_custom);
        mRadioGroup = view.findViewById(R.id.radio_group);

        Bundle args = getArguments();
        FormatType formatType = (FormatType) args.getSerializable(KEY_FORMAT_TYPE);
        if (formatType == null) {
            formatType = FormatType.DATE_FORMAT;
        }
        mFormatValue = args.getString(KEY_FORMAT_VALUE);

        mEntries = formatType.getEntries(getActivity());
        mValues = formatType.getValues(getActivity());
        createRadioButtons();

        boolean isCustomFormat = isCustomFormatValue(mFormatValue);
        mEditCustomFormat.setEnabled(isCustomFormat);
        if (isCustomFormat) {
            mEditCustomFormat.setText(mFormatValue);
        }

        @StringRes int titleRes = formatType == FormatType.DATE_FORMAT
                ? R.string.site_settings_date_format_title : R.string.site_settings_time_format_title;
        txtTitle.setText(titleRes);

        txtHelp.setOnClickListener(
                v -> ActivityLauncher.openUrlExternal(v.getContext(), Constants.URL_DATETIME_FORMAT_HELP));

        int topOffset = getResources().getDimensionPixelOffset(R.dimen.settings_fragment_dialog_vertical_inset);

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity())
                .setBackgroundInsetTop(topOffset)
                .setBackgroundInsetBottom(topOffset);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        return builder.create();
    }

    private void createRadioButtons() {
        boolean isCustomFormat = isCustomFormatValue(mFormatValue);
        int margin = getResources().getDimensionPixelSize(R.dimen.margin_small);

        for (int i = 0; i < mEntries.length; i++) {
            MaterialRadioButton radio = new MaterialRadioButton(getActivity());
            radio.setText(mEntries[i]);
            radio.setId(i);
            mRadioGroup.addView(radio);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) radio.getLayoutParams();
            params.topMargin = margin;
            params.bottomMargin = margin;

            if (isCustomFormat && isCustomFormatEntry(mEntries[i])) {
                radio.setChecked(true);
            } else if (mValues[i].equals(mFormatValue)) {
                radio.setChecked(true);
            }
        }

        mRadioGroup.setOnCheckedChangeListener(
                (group, checkedId) -> mEditCustomFormat.setEnabled(isCustomFormatEntry(mEntries[checkedId])));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    private boolean isCustomFormatEntry(@NonNull String entry) {
        String customEntry = getString(R.string.site_settings_format_entry_custom);
        return entry.equals(customEntry);
    }

    private boolean isCustomFormatValue(@NonNull String value) {
        for (String thisValue : mValues) {
            if (thisValue.equals(value)) {
                return false;
            }
        }
        return true;
    }

    private String getSelectedFormatValue() {
        int id = mRadioGroup.getCheckedRadioButtonId();
        if (id == -1) {
            return null;
        }
        if (isCustomFormatEntry(mEntries[id])) {
            return EditTextUtils.getText(mEditCustomFormat);
        }
        return mValues[id];
    }

    @Override
    public void onResume() {
        super.onResume();
        UiHelpers.Companion.adjustDialogSize(getDialog());
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        String formatValue = getSelectedFormatValue();
        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        android.app.Fragment target = getTargetFragment();
        if (mConfirmed && target != null && !TextUtils.isEmpty(formatValue)) {
            Intent intent = new Intent().putExtra(KEY_FORMAT_VALUE, formatValue);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }

        super.onDismiss(dialog);
    }
}
