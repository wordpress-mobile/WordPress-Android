package org.wordpress.android.ui.reader_native;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.ToastUtils;

/**
 * Created by nbradbury on 7/16/13.
 * views the activity log (ReaderLog.java)
 */
public class ReaderLogViewerActivity extends FragmentActivity {
    private TextView mTxtLogViewer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader_logviewer);

        mTxtLogViewer = (TextView) findViewById(R.id.text_log);
        mTxtLogViewer.setText(Html.fromHtml(ReaderLog.toHtml()));

        Button btnShare = (Button) findViewById(R.id.btn_share);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareAppLog();
            }
        });
    }

    private void shareAppLog() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mTxtLogViewer.getText().toString());
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.reader_btn_share)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(this, R.string.reader_toast_err_share_intent);
        }

    }
}
