package org.wordpress.android.ui.prefs.notifications;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.AccountStore.UpdateSubscriptionPayload.SubscriptionFrequency;

/**
 * A {@link DialogFragment} displaying notification settings for followed blogs.
 */
public class NotificationSettingsFollowedDialog extends DialogFragment implements DialogInterface.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    public static final String ARG_EMAIL_COMMENTS = "EXTRA_EMAIL_COMMENTS";
    public static final String ARG_EMAIL_POSTS = "EXTRA_EMAIL_POSTS";
    public static final String ARG_EMAIL_POSTS_FREQUENCY = "EXTRA_EMAIL_POSTS_FREQUENCY";
    public static final String ARG_NOTIFICATION_POSTS = "EXTRA_NOTIFICATION_POSTS";
    public static final String KEY_EMAIL_COMMENTS = "KEY_EMAIL_COMMENTS";
    public static final String KEY_EMAIL_POSTS = "KEY_EMAIL_POSTS";
    public static final String KEY_EMAIL_POSTS_FREQUENCY = "KEY_EMAIL_POSTS_FREQUENCY";
    public static final String KEY_NOTIFICATION_POSTS = "KEY_NOTIFICATION_POSTS";
    public static final String TAG = "notification-settings-followed-dialog";

    private static final String EMAIL_POSTS_FREQUENCY_DAILY = SubscriptionFrequency.DAILY.toString();
    private static final String EMAIL_POSTS_FREQUENCY_INSTANTLY = SubscriptionFrequency.INSTANTLY.toString();
    private static final String EMAIL_POSTS_FREQUENCY_WEEKLY = SubscriptionFrequency.WEEKLY.toString();

    private RadioButton mRadioButtonFrequencyDaily;
    private RadioButton mRadioButtonFrequencyInstantly;
    private RadioButton mRadioButtonFrequencyWeekly;
    private RadioGroup mRadioGroupEmailPosts;
    private String mRadioButtonSelected;
    private SwitchCompat mSwitchEmailComments;
    private SwitchCompat mSwitchEmailPosts;
    private SwitchCompat mSwitchNotificationPosts;
    private boolean mConfirmed;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams")
        View layout = inflater.inflate(R.layout.followed_sites_dialog, null);

        mRadioGroupEmailPosts = layout.findViewById(R.id.email_new_posts_radio_group);
        mRadioButtonFrequencyInstantly = layout.findViewById(R.id.email_new_posts_radio_button_instantly);
        mRadioButtonFrequencyDaily = layout.findViewById(R.id.email_new_posts_radio_button_daily);
        mRadioButtonFrequencyWeekly = layout.findViewById(R.id.email_new_posts_radio_button_weekly);
        mSwitchNotificationPosts = layout.findViewById(R.id.notification_new_posts_switch);
        mSwitchEmailPosts = layout.findViewById(R.id.email_new_posts_switch);
        mSwitchEmailComments = layout.findViewById(R.id.email_new_comments_switch);

        mRadioButtonFrequencyInstantly.setOnCheckedChangeListener(this);
        mRadioButtonFrequencyDaily.setOnCheckedChangeListener(this);
        mRadioButtonFrequencyWeekly.setOnCheckedChangeListener(this);
        mSwitchNotificationPosts.setOnCheckedChangeListener(this);
        mSwitchEmailPosts.setOnCheckedChangeListener(this);
        mSwitchEmailComments.setOnCheckedChangeListener(this);

        Bundle args = getArguments();

        if (args != null) {
            mSwitchNotificationPosts.setChecked(args.getBoolean(ARG_NOTIFICATION_POSTS, false));
            mSwitchEmailPosts.setChecked(args.getBoolean(ARG_EMAIL_POSTS, false));
            mSwitchEmailComments.setChecked(args.getBoolean(ARG_EMAIL_COMMENTS, false));
            mRadioButtonSelected = args.getString(ARG_EMAIL_POSTS_FREQUENCY, "");

            if (mRadioButtonSelected.equalsIgnoreCase(EMAIL_POSTS_FREQUENCY_INSTANTLY)) {
                mRadioButtonFrequencyInstantly.setChecked(true);
            } else if (mRadioButtonSelected.equalsIgnoreCase(EMAIL_POSTS_FREQUENCY_DAILY)) {
                mRadioButtonFrequencyDaily.setChecked(true);
            } else if (mRadioButtonSelected.equalsIgnoreCase(EMAIL_POSTS_FREQUENCY_WEEKLY)) {
                mRadioButtonFrequencyWeekly.setChecked(true);
            }
        }

        AlertDialog.Builder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(getString(R.string.notification_settings_followed_dialog_title));
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(layout);

        return builder.create();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            if (buttonView == mRadioButtonFrequencyInstantly) {
                mRadioButtonSelected = EMAIL_POSTS_FREQUENCY_INSTANTLY;
            } else if (buttonView == mRadioButtonFrequencyDaily) {
                mRadioButtonSelected = EMAIL_POSTS_FREQUENCY_DAILY;
            } else if (buttonView == mRadioButtonFrequencyWeekly) {
                mRadioButtonSelected = EMAIL_POSTS_FREQUENCY_WEEKLY;
            }
        }

        if (buttonView == mSwitchEmailPosts) {
            toggleEmailFrequencyButtons(isChecked);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        android.app.Fragment target = getTargetFragment();

        if (target != null) {
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getResultIntent());
        }

        super.onDismiss(dialog);
    }

    private Intent getResultIntent() {
        if (mConfirmed) {
            Intent intent = new Intent();
            intent.putExtra(KEY_NOTIFICATION_POSTS, mSwitchNotificationPosts.isChecked());
            intent.putExtra(KEY_EMAIL_POSTS, mSwitchEmailPosts.isChecked());
            intent.putExtra(KEY_EMAIL_COMMENTS, mSwitchEmailComments.isChecked());

            if (mSwitchEmailPosts.isChecked()) {
                intent.putExtra(KEY_EMAIL_POSTS_FREQUENCY, mRadioButtonSelected);
            }

            return intent;
        }

        return null;
    }

    private void toggleEmailFrequencyButtons(boolean enabled) {
        mRadioGroupEmailPosts.setVisibility(enabled ? View.VISIBLE : View.GONE);

        if (enabled && mRadioButtonSelected != null && mRadioButtonSelected.equalsIgnoreCase("")) {
            mRadioButtonFrequencyInstantly.setChecked(true);
        }
    }
}
