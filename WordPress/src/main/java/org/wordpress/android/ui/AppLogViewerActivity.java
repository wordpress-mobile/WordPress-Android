package org.wordpress.android.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.text.HtmlCompat;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.Locale;

import static java.lang.String.format;

/**
 * views the activity log (see utils/AppLog.java)
 */
public class AppLogViewerActivity extends LocaleAwareActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logviewer_activity);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.reader_title_applog);
        }

        final ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(new LogAdapter(this));
    }

    private class LogAdapter extends BaseAdapter {
        private final ArrayList<String> mEntries;
        private final LayoutInflater mInflater;

        private LogAdapter(Context context) {
            mEntries = AppLog.toHtmlList(context);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Override
        public Object getItem(int position) {
            return mEntries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LogViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.logviewer_listitem, parent, false);
                holder = new LogViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (LogViewHolder) convertView.getTag();
            }

            // take the header lines (app version & device name) into account or else the
            // line numbers shown here won't match the line numbers when the log is shared
            int lineNum = position - AppLog.HEADER_LINE_COUNT + 1;
            if (lineNum > 0) {
                holder.mTxtLineNumber.setText(format(Locale.US, "%02d", lineNum));
                holder.mTxtLineNumber.setVisibility(View.VISIBLE);
            } else {
                holder.mTxtLineNumber.setVisibility(View.GONE);
            }

            holder.mTxtLogEntry.setText(HtmlCompat.fromHtml(mEntries.get(position), HtmlCompat.FROM_HTML_MODE_LEGACY));

            return convertView;
        }

        private class LogViewHolder {
            private final TextView mTxtLineNumber;
            private final TextView mTxtLogEntry;

            LogViewHolder(View view) {
                mTxtLineNumber = (TextView) view.findViewById(R.id.text_line);
                mTxtLogEntry = (TextView) view.findViewById(R.id.text_log);
            }
        }
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

    private void copyAppLogToClipboard() {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("AppLog", AppLog.toPlainText(this)));
            ToastUtils.showToast(this, R.string.logs_copied_to_clipboard);
        } catch (Exception e) {
            AppLog.e(T.UTILS, e);
            ToastUtils.showToast(this, R.string.error_copy_to_clipboard);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_log_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.app_log_share:
                shareAppLog();
                return true;
            case R.id.app_log_copy_to_clipboard:
                copyAppLogToClipboard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
