package org.wordpress.android.ui.prefs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.WPPrefUtils;

public class NumberPickerDialog extends DialogFragment
        implements DialogInterface.OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    public static final String SHOW_SWITCH_KEY    = "show-switch";
    public static final String SWITCH_ENABLED_KEY = "switch-enabled";
    public static final String SWITCH_TITLE_KEY   = "switch-title";
    public static final String SWITCH_DESC_KEY    = "switch-description";
    public static final String TITLE_KEY          = "dialog-title";
    public static final String HEADER_TEXT_KEY    = "header-text";
    public static final String MIN_VALUE_KEY      = "min-value";
    public static final String MAX_VALUE_KEY      = "max-value";
    public static final String CUR_VALUE_KEY      = "cur-value";

    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 99;

    private SwitchCompat mSwitch;
    private TextView mHeaderText;
    private NumberPicker mNumberPicker;
    private NumberPicker.Formatter mFormat;
    private int mMinValue;
    private int mMaxValue;
    private boolean mConfirmed;

    public NumberPickerDialog() {
        mMinValue = DEFAULT_MIN_VALUE;
        mMaxValue = DEFAULT_MAX_VALUE;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        View view = View.inflate(getActivity(), R.layout.number_picker_dialog, null);
        TextView switchText = (TextView) view.findViewById(R.id.number_picker_text);
        mSwitch = (SwitchCompat) view.findViewById(R.id.number_picker_switch);
        mHeaderText = (TextView) view.findViewById(R.id.number_picker_header);
        mNumberPicker = (NumberPicker) view.findViewById(R.id.number_picker);
        int value = mMinValue;

        Bundle args = getArguments();
        if (args != null) {
            if (args.getBoolean(SHOW_SWITCH_KEY, false)) {
                mSwitch.setVisibility(View.VISIBLE);
                mSwitch.setText(args.getString(SWITCH_TITLE_KEY, ""));
                mSwitch.setChecked(args.getBoolean(SWITCH_ENABLED_KEY, false));
                final View toggleContainer = view.findViewById(R.id.number_picker_toggleable);
                toggleContainer.setEnabled(mSwitch.isChecked());
                mNumberPicker.setEnabled(mSwitch.isChecked());
            } else {
                mSwitch.setVisibility(View.GONE);
            }
            switchText.setText(args.getString(SWITCH_DESC_KEY, ""));
            mHeaderText.setText(args.getString(HEADER_TEXT_KEY, ""));
            mMinValue = args.getInt(MIN_VALUE_KEY, DEFAULT_MIN_VALUE);
            mMaxValue = args.getInt(MAX_VALUE_KEY, DEFAULT_MAX_VALUE);
            value = args.getInt(CUR_VALUE_KEY, mMinValue);

            builder.setCustomTitle(getDialogTitleView(args.getString(TITLE_KEY, "")));
        }

        mNumberPicker.setFormatter(mFormat);
        mNumberPicker.setMinValue(mMinValue);
        mNumberPicker.setMaxValue(mMaxValue);
        mNumberPicker.setValue(value);

        mSwitch.setOnCheckedChangeListener(this);

        // hide empty text views
        if (TextUtils.isEmpty(switchText.getText())) {
            switchText.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(mHeaderText.getText())) {
            mHeaderText.setVisibility(View.GONE);
        }

        builder.setPositiveButton(R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positive != null) WPPrefUtils.layoutAsFlatButton(positive);
        if (negative != null) WPPrefUtils.layoutAsFlatButton(negative);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mNumberPicker.setEnabled(isChecked);
        mHeaderText.setEnabled(isChecked);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Fragment target = getTargetFragment();
        if (target != null) {
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getResultIntent());
        }

        super.onDismiss(dialog);
    }

    public void setNumberFormat(NumberPicker.Formatter format) {
        mFormat = format;
    }

    private View getDialogTitleView(String title) {
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams")
        View titleView = inflater.inflate(R.layout.detail_list_preference_title, null);
        TextView titleText = ((TextView) titleView.findViewById(R.id.title));
        titleText.setText(title);
        titleText.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));
        return titleView;
    }

    private Intent getResultIntent() {
        if (mConfirmed) {
            return new Intent()
                    .putExtra(SWITCH_ENABLED_KEY, mSwitch.isChecked())
                    .putExtra(CUR_VALUE_KEY, mNumberPicker.getValue());
        }

        return null;
    }
}
