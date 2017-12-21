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
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;

import java.util.ArrayList;

public class SiteSettingsTimezoneDialog extends DialogFragment implements DialogInterface.OnClickListener {

    public static final String KEY_TIMEZONE = "timezone";

    private boolean mConfirmed;
    private RecyclerView mRecycler;
    private final ArrayList<String> mTimezones = new ArrayList<>();

    public static SiteSettingsTimezoneDialog newInstance(@NonNull String timezone) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_TIMEZONE, timezone);

        SiteSettingsTimezoneDialog dialog = new SiteSettingsTimezoneDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.site_settings_format_dialog, null);
        mRecycler = view.findViewById(R.id.recycler);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(R.string.cancel, this);
        builder.setView(view);

        loadTimezones();

        return builder.create();
    }

    private void loadTimezones() {
        // https://public-api.wordpress.com/wpcom/v2/timezones
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mConfirmed = which == DialogInterface.BUTTON_POSITIVE;
        dismiss();
    }

    private String getSelectedTimezone() {
        return null;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        String timezone = getSelectedTimezone();
        Fragment target = getTargetFragment();
        if (mConfirmed && target != null && !TextUtils.isEmpty(timezone)) {
            Intent intent = new Intent().putExtra(KEY_TIMEZONE, timezone);
            target.onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        }

        super.onDismiss(dialog);
    }

}
