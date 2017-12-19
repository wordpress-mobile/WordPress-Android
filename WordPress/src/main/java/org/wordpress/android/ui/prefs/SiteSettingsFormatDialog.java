package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.wordpress.android.Constants;
import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.EditTextUtils;

/**
 * Custom date/time format dialog
 */

public class SiteSettingsFormatDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public enum FormatType {
        DATE_FORMAT, TIME_FORMAT
    }

    private static final String KEY_FORMAT_TYPE = "format_type";
    public static final String KEY_FORMAT_STRING = "format_string";

    private FormatType mFormatType;
    private boolean mConfirmed;
    private String mSelectedEntry;
    private String mSelectedValue;

    private EditText mEditText;
    private RadioGroup mRadioGroup;

    private String[] mEntries;
    private String[] mValues;

    public static SiteSettingsFormatDialog newInstance(@NonNull FormatType formatType, @NonNull String formatString) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_FORMAT_TYPE, formatType);
        args.putString(KEY_FORMAT_STRING, formatString);

        SiteSettingsFormatDialog dialog = new SiteSettingsFormatDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.site_settings_format_dialog, null);
        TextView txtTitle = view.findViewById(R.id.text_title);
        TextView txtHelp = view.findViewById(R.id.text_help);
        mEditText = view.findViewById(R.id.edit_custom);
        mRadioGroup = view.findViewById(R.id.radio_group);

        Bundle args = getArguments();
        String formatString = args.getString(KEY_FORMAT_STRING);
        mEditText.setText(formatString);

        mFormatType = (FormatType) args.getSerializable(KEY_FORMAT_TYPE);
        @StringRes int titleRes = mFormatType == FormatType.DATE_FORMAT ?
                R.string.site_settings_date_format_title : R.string.site_settings_time_format_title;
        txtTitle.setText(titleRes);

        txtHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.openUrlExternal(v.getContext(), Constants.URL_DATETIME_FORMAT_HELP);
            }
        });

        switch (mFormatType) {
            case DATE_FORMAT:
                mEntries = getResources().getStringArray(R.array.date_format_entries);
                mValues = getResources().getStringArray(R.array.date_format_values);
                break;
            default:
                mEntries = getResources().getStringArray(R.array.time_format_entries);
                mValues = getResources().getStringArray(R.array.time_format_values);
                break;
        }


        createRadioButtons();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        return builder.create();
    }

    private void createRadioButtons() {
        int margin = getResources().getDimensionPixelSize(R.dimen.margin_small);

        for (int i = 0; i < mEntries.length; i++) {
            RadioButton radio = new RadioButton(getActivity());
            radio.setText(mEntries[i]);
            radio.setId(i);
            mRadioGroup.addView(radio);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) radio.getLayoutParams();
            params.topMargin = margin;
            params.bottomMargin = margin;
        }

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

            }
        });
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        String formatString = EditTextUtils.getText(mEditText);
        Fragment target = getTargetFragment();
        if (mConfirmed && target != null && !TextUtils.isEmpty(formatString)) {
            if (mConfirmed) {
                Intent intent = new Intent().putExtra(KEY_FORMAT_STRING, formatString);
                target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
            }
        }

        super.onDismiss(dialog);
    }

}
