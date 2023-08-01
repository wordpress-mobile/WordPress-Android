package org.wordpress.android.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.MissingSplitsUtils;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;

@SuppressLint("CustomSplashScreen")
public class WPLaunchActivity extends LocaleAwareActivity {
    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (MissingSplitsUtils.INSTANCE.isMissingSplits(this)) {
            // There are missing splits. Display a warning message.
            showMissingSplitsDialog();
            return;
        }
        ProfilingUtils.split("WPLaunchActivity.onCreate");
        launchWPMainActivity();
    }

    private void showMissingSplitsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(MissingSplitsUtils.DIALOG_TITLE)
                .setMessage(MissingSplitsUtils.DIALOG_MESSAGE)
                .setNegativeButton(MissingSplitsUtils.DIALOG_BUTTON, null)
                .setOnDismissListener(dialog -> finish())
                .show();
    }

    private void launchWPMainActivity() {
        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(getIntent().getAction());
        intent.setData(getIntent().getData());
        startActivity(intent);
        finish();
    }
}
