package org.wordpress.android.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;

/**
 * views the activity log (see utils/AppLog.java)
 */
public class AppLogViewerActivity extends AppCompatActivity {
    private static final int ID_SHARE = 1;
    private static final int ID_COPY_TO_CLIPBOARD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logviewer_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
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
                holder.txtLineNumber.setText(String.format("%02d", lineNum));
                holder.txtLineNumber.setVisibility(View.VISIBLE);
            } else {
                holder.txtLineNumber.setVisibility(View.GONE);
            }

            holder.txtLogEntry.setText(Html.fromHtml(mEntries.get(position)));

            return convertView;
        }

        private class LogViewHolder {
            private final TextView txtLineNumber;
            private final TextView txtLogEntry;

            LogViewHolder(View view) {
                txtLineNumber = (TextView) view.findViewById(R.id.text_line);
                txtLogEntry = (TextView) view.findViewById(R.id.text_log);
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
        // Copy to clipboard button
        MenuItem item = menu.add(Menu.NONE, ID_COPY_TO_CLIPBOARD, Menu.NONE, android.R.string.copy);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ic_content_copy_white_24dp);
        // Share button
        item = menu.add(Menu.NONE, ID_SHARE, Menu.NONE, R.string.reader_btn_share);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        item.setIcon(R.drawable.ic_share_white_24dp);
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
            case ID_COPY_TO_CLIPBOARD:
                copyAppLogToClipboard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
