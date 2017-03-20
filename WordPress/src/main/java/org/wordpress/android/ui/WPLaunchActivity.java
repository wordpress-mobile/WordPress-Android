package org.wordpress.android.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;

public class WPLaunchActivity extends AppCompatActivity {
    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */

    private ProgressDialog mMigrationProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfilingUtils.split("WPLaunchActivity.onCreate");

        if (WordPress.sIsMigrationInProgress) {
            mMigrationProgressDialog = new ProgressDialog(this);
            mMigrationProgressDialog.setMessage(this.getResources().getString(R.string.migration_message));
            mMigrationProgressDialog.setCancelable(false);
            mMigrationProgressDialog.show();
            WordPress.registerMigrationListener(new WordPress.MigrationListener() {
                @Override
                public void onCompletion() {
                    if (mMigrationProgressDialog != null) {
                        mMigrationProgressDialog.dismiss();
                        mMigrationProgressDialog = null;
                    }
                    launchWPMainActivity();
                }

                @Override
                public void onError() {
                    AppLog.d(T.DB, "Show a Migration Error toast.");
                    mMigrationProgressDialog.dismiss();
                    mMigrationProgressDialog = null;
                    ToastUtils.showToast(WPLaunchActivity.this, getString(R.string.migration_error_not_connected),
                            ToastUtils.Duration.LONG);
                }
            });
        } else {
            launchWPMainActivity();
        }
    }

    private void launchWPMainActivity() {
        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mMigrationProgressDialog != null) {
            mMigrationProgressDialog.dismiss();
            mMigrationProgressDialog = null;
        }
        super.onDestroy();
    }
}
