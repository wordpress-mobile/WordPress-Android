package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.PostModel;

import java.util.Calendar;

public class PostDatePickerDialogFragment extends DialogFragment implements
        DatePickerDialog.OnDateSetListener {
    interface OnPostDatePickerDialogListener {
        void onPostDatePickerDialogPositiveButtonClicked(@NonNull PostDatePickerDialogFragment dialog);
    }

    enum DialogType {
        DATE_PICKER,
        TIME_PICKER
    }

    public static final String TAG = "post_date_picker_dialog_fragment";
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_DAY = "day";
    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";
    private static final String ARG_CAN_PUBLISH_IMMEDIATELY = "can_publish_immediately";

    private DialogType mDialogType;
    private int mDay;
    private int mMonth;
    private int mYear;
    private boolean mCanPublishImmediately;

    private OnPostDatePickerDialogListener mListener;

    public static PostDatePickerDialogFragment newInstance(@NonNull PostModel post,
                                                           @NonNull Calendar calendar) {
        PostDatePickerDialogFragment fragment = new PostDatePickerDialogFragment();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        boolean canPublishImmediately = PostUtils.shouldPublishImmediatelyOptionBeAvailable(post);

        Bundle args = new Bundle();
        args.putInt(ARG_DAY, day);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        args.putBoolean(ARG_CAN_PUBLISH_IMMEDIATELY, canPublishImmediately);
        args.putSerializable(ARG_DIALOG_TYPE, DialogType.DATE_PICKER);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        mDialogType = (DialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCanPublishImmediately = args.getBoolean(ARG_CAN_PUBLISH_IMMEDIATELY);

        mDay = args.getInt(ARG_DAY);
        mMonth = args.getInt(ARG_MONTH);
        mYear = args.getInt(ARG_YEAR);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPostDatePickerDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPostPubDatePickerDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final DatePickerDialog datePickerDialog = new DatePickerDialog(
                getActivity(),
                null,
                mYear,
                mMonth,
                mDay);
        datePickerDialog.setTitle(R.string.select_date);
        datePickerDialog.setButton(
                DialogInterface.BUTTON_POSITIVE,
                getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DatePicker datePicker = datePickerDialog.getDatePicker();
                        mDay = datePicker.getDayOfMonth();
                        mMonth = datePicker.getMonth();
                        mYear = datePicker.getYear();
                        getArguments().putInt(ARG_DAY, mDay);
                        getArguments().putInt(ARG_MONTH, mMonth);
                        getArguments().putInt(ARG_YEAR, mYear);
                        mListener.onPostDatePickerDialogPositiveButtonClicked(
                                PostDatePickerDialogFragment.this
                        );
                        // showPostTimeSelectionDialog(mYear, mMonth, mDay);
                    }
                });
        datePickerDialog.setButton(
                DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.immediately),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Calendar now = Calendar.getInstance();
                        // updatePublishDate(now);
                    }
                });
        datePickerDialog.setButton(
                DialogInterface.BUTTON_NEGATIVE,
                getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // noop
                    }
                });

        if (mCanPublishImmediately) {
            // We shouldn't let the user pick a past date since we'll just override it to Immediately if they do
            // We can't set the min date to now, so we need to subtract some amount of time
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        }

        return datePickerDialog;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        // TODO
    }
}
