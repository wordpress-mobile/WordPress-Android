package org.wordpress.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

public class AztecEditorOptionsReceiver extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action) && uri != null) {
            String available = uri.getQueryParameter("available");
            String enabled = uri.getQueryParameter("enabled");

            // Note: doesn't allow to deactivate visual editor
            if ("1".equals(available)) {
                AppLog.i(T.EDITOR, "Aztec Editor is now Available");
                AppPrefs.setAztecEditorAvailable(true);
            }

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            if ("1".equals(enabled)) {
                AppLog.i(T.EDITOR, "Aztec Editor Enabled");
                ToastUtils.showToast(this, R.string.aztec_editor_enabled);

                AppPrefs.setAztecEditorEnabled(true);
                AppPrefs.setVisualEditorEnabled(false);

                prefs.edit().putString(getString(R.string.pref_key_editor_type), "2").apply();
            } else if ("0".equals(enabled)) {
                AppLog.i(T.EDITOR, "Aztec Editor Disabled");
                ToastUtils.showToast(this, R.string.aztec_editor_disabled);

                AppPrefs.setAztecEditorEnabled(false);
                AppPrefs.setVisualEditorEnabled(true);

                prefs.edit().putString(getString(R.string.pref_key_editor_type), "1").apply();
            } else {
                ToastUtils.showToast(this, R.string.aztec_editor_available);
            }
        }

        Intent intent = new Intent(this, WPLaunchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
