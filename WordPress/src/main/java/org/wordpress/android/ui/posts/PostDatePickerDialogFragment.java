package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.ContextThemeWrapper;
import android.widget.DatePicker;
import android.widget.TimePicker;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.PostModel;

import java.util.Calendar;

public class PostDatePickerDialogFragment extends DialogFragment {
    interface OnPostDatePickerDialogListener {
        void onPostDatePickerDialogPositiveButtonClicked(
                @NonNull PostDatePickerDialogFragment dialog,
                @NonNull Calendar calender);
    }

    enum PickerDialogType {
        DATE_PICKER,
        TIME_PICKER
    }

    public static final String TAG_DATE = "post_date_picker_dialog_fragment";
    public static final String TAG_TIME = "post_time_picker_dialog_fragment";

    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CAN_PUBLISH_IMMEDIATELY = "can_publish_immediately";

    private static final String ARG_DAY = "day";
    private static final String ARG_MONTH = "month";
    private static final String ARG_YEAR = "year";

    private static final String ARG_MINUTE = "minute";
    private static final String ARG_HOUR = "hour";

    private PickerDialogType mDialogType;
    private int mDay;
    private int mMonth;
    private int mYear;
    private int mHour;
    private int mMinute;

    private boolean mCanPublishImmediately;
    private boolean mPublishNow;

    private OnPostDatePickerDialogListener mListener;

    public static PostDatePickerDialogFragment newInstance(@NonNull PickerDialogType dialogType,
                                                           @NonNull PostModel post,
                                                           @NonNull Calendar calendar) {
        PostDatePickerDialogFragment fragment = new PostDatePickerDialogFragment();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        boolean canPublishImmediately = PostUtils.shouldPublishImmediatelyOptionBeAvailable(post);

        Bundle args = new Bundle();

        args.putInt(ARG_DAY, day);
        args.putInt(ARG_MONTH, month);
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MINUTE, minute);
        args.putInt(ARG_HOUR, hour);

        args.putBoolean(ARG_CAN_PUBLISH_IMMEDIATELY, canPublishImmediately);
        args.putSerializable(ARG_DIALOG_TYPE, dialogType);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        mDialogType = (PickerDialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCanPublishImmediately = args.getBoolean(ARG_CAN_PUBLISH_IMMEDIATELY);

        mDay = args.getInt(ARG_DAY);
        mMonth = args.getInt(ARG_MONTH);
        mYear = args.getInt(ARG_YEAR);

        mMinute = args.getInt(ARG_MINUTE);
        mHour = args.getInt(ARG_HOUR);
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

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        switch (mDialogType) {
            case DATE_PICKER:
                final DatePickerDialog datePickerDialog = new DatePickerDialog(
                        new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog),
                        null,
                        mYear,
                        mMonth,
                        mDay);
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
                                        PostDatePickerDialogFragment.this,
                                        getCalender());
                            }
                        });
                String neutralButtonTitle = mCanPublishImmediately ? getString(R.string.immediately)
                        : getString(R.string.now);
                datePickerDialog.setButton(
                        DialogInterface.BUTTON_NEUTRAL,
                        neutralButtonTitle,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Calendar now = Calendar.getInstance();
                                mPublishNow = true;
                                mListener.onPostDatePickerDialogPositiveButtonClicked(
                                        PostDatePickerDialogFragment.this,
                                        now);
                            }
                        });
                if (mCanPublishImmediately) {
                    // We shouldn't let the user pick a past date since we'll just override it to Immediately if they do
                    // We can't set the min date to now, so we need to subtract some amount of time
                    datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
                }
                return datePickerDialog;

            case TIME_PICKER:
                boolean is24HrFormat = DateFormat.is24HourFormat(getActivity());
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        new ContextThemeWrapper(getActivity(), R.style.Calypso_Dialog_Alert),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker,
                                                  int selectedHour,
                                                  int selectedMinute) {
                                mHour = selectedHour;
                                mMinute = selectedMinute;
                                getArguments().putInt(ARG_HOUR, mHour);
                                getArguments().putInt(ARG_MINUTE, mMinute);
                                mListener.onPostDatePickerDialogPositiveButtonClicked(
                                        PostDatePickerDialogFragment.this,
                                        getCalender());
                            }
                        },
                        mHour,
                        mMinute,
                        is24HrFormat);
                return timePickerDialog;

            default:
                // should never get here
                return null;
        }
    }

    @NonNull
    private Calendar getCalender() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(mYear, mMonth, mDay, mHour, mMinute);
        return calendar;
    }

    public @NonNull
    PickerDialogType getDialogType() {
        return mDialogType;
    }

    public boolean isPublishNow() {
        return mPublishNow;
    }
}
