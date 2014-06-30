package org.wordpress.android.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

/**
 * views the activity log (see utils/AppLog.java)
 */
public class AppLogViewerActivity extends Activity {
    private TextView mTxtLogViewer;
    private static final int ID_SHARE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logviewer_activity);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mTxtLogViewer = (TextView) findViewById(R.id.text_log);
        mTxtLogViewer.setText(Html.fromHtml(AppLog.toHtml(this)));

        // this is necessary to enable the textView to scroll vertically
        mTxtLogViewer.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    private void shareAppLog() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, AppLog.toPlainText(this));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + getTitle());
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_btn_share)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(Menu.NONE, ID_SHARE, Menu.NONE, R.string.reader_btn_share);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ab_icon_share);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case ID_SHARE:
                shareAppLog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
