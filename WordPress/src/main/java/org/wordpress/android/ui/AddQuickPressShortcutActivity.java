package org.wordpress.android.ui;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Vector;

public class AddQuickPressShortcutActivity extends ListActivity {
    static final int ADD_ACCOUNT_REQUEST = 0;

    public List<Map<String, Object>> accounts;
    public String[] blogNames;
    public int[] accountIDs;
    public String[] accountUsers;
    public String[] blavatars;
    public List<String> accountNames = new Vector<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.add_quickpress_shortcut);
        setTitle(getResources().getText(R.string.quickpress_window_title));

        displayAccounts();
    }

    private void displayAccounts() {
        accounts = WordPress.wpDB.getVisibleBlogs();

        ListView listView = (ListView) findViewById(android.R.id.list);

        ImageView iv = new ImageView(this);
        iv.setBackgroundDrawable(getResources().getDrawable(R.drawable.list_divider));
        listView.addFooterView(iv);
        listView.setVerticalFadingEdgeEnabled(false);
        listView.setVerticalScrollBarEnabled(true);

        if (accounts.size() > 0) {
            ScrollView sv = new ScrollView(this);
            sv.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
            LinearLayout layout = new LinearLayout(this);
            layout.setPadding(10, 10, 10, 0);
            layout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

            layout.setOrientation(LinearLayout.VERTICAL);

            blogNames = new String[accounts.size()];
            accountIDs = new int[accounts.size()];
            accountUsers = new String[accounts.size()];
            blavatars = new String[accounts.size()];
            int validBlogCtr = 0;
            for (int i = 0; i < accounts.size(); i++) {
                Map<String, Object> curHash = accounts.get(i);
                blogNames[validBlogCtr] = curHash.get("blogName").toString();
                accountUsers[validBlogCtr] = curHash.get("username").toString();
                accountIDs[validBlogCtr] = (Integer)curHash.get("id");
                String url = curHash.get("url").toString();
                if (url != null) {
                    blavatars[validBlogCtr] = GravatarUtils.blavatarFromUrl(url, 60);
                } else {
                    blavatars[validBlogCtr] = "";
                }
                accountNames.add(validBlogCtr, blogNames[i]);
                validBlogCtr++;
            }

            if (validBlogCtr < accounts.size()){
                accounts = WordPress.wpDB.getVisibleBlogs();
            }

            setListAdapter(new HomeListAdapter());

            listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
                    AddQuickPressShortcutActivity.this.buildDialog(position);
                }
            });

            if(accounts.size() == 1) {
                AddQuickPressShortcutActivity.this.buildDialog(0);
            }

        } else {
            // no account, load new account view
            Intent i = new Intent(AddQuickPressShortcutActivity.this, SignInActivity.class);
            startActivityForResult(i, ADD_ACCOUNT_REQUEST);
        }
    }

    private void buildDialog(int positionParam) {
        final int position = positionParam;

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AddQuickPressShortcutActivity.this);
        dialogBuilder.setTitle(R.string.quickpress_add_alert_title);

        final EditText quickPressShortcutName = new EditText(AddQuickPressShortcutActivity.this);
        quickPressShortcutName.setText("QP " + StringUtils.unescapeHTML(accountNames.get(position)));
        dialogBuilder.setView(quickPressShortcutName);

        dialogBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(quickPressShortcutName.getText())) {
                    Toast t = Toast.makeText(AddQuickPressShortcutActivity.this, R.string.quickpress_add_error, Toast.LENGTH_LONG);
                    t.show();
                } else {
                    Intent shortcutIntent = new Intent(getApplicationContext(), EditPostActivity.class);
                    shortcutIntent.setAction(Intent.ACTION_MAIN);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_QUICKPRESS_BLOG_ID, accountIDs[position]);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_IS_QUICKPRESS, true);

                    Intent addIntent = new Intent();
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, quickPressShortcutName.getText().toString());
                    addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext
                            (AddQuickPressShortcutActivity.this, R.mipmap.app_icon));

                    WordPress.wpDB.addQuickPressShortcut(accountIDs[position], quickPressShortcutName.getText().toString());

                    if (WordPress.currentBlog == null) {
                        WordPress.currentBlog = WordPress.wpDB.instantiateBlogByLocalId(accountIDs[position]);
                        WordPress.wpDB.updateLastBlogId(accountIDs[position]);
                    }

                    addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    AddQuickPressShortcutActivity.this.sendBroadcast(addIntent);
                    AddQuickPressShortcutActivity.this.finish();
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            // just let the dialog close
            public void onClick(DialogInterface dialog, int which) {}
        });

        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADD_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    accounts = WordPress.wpDB.getVisibleBlogs();
                    if (accounts.size() > 0) {
                        displayAccounts();
                        break;
                    }
                }
                finish();
                break;
        }
    }

    protected class HomeListAdapter extends BaseAdapter {
        public HomeListAdapter() {
        }

        public int getCount() {
            return accounts.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            RelativeLayout view = (RelativeLayout) convertView;
            if (view == null) {
                LayoutInflater inflater = getLayoutInflater();
                view = (RelativeLayout)inflater.inflate(R.layout.home_row, parent, false);
            }
            String username = accountUsers[position];
            view.setId(Integer.valueOf(accountIDs[position]));

            TextView blogName = (TextView)view.findViewById(R.id.blogName);
            TextView blogUsername = (TextView)view.findViewById(R.id.blogUser);
            NetworkImageView blavatar = (NetworkImageView)view.findViewById(R.id.blavatar);

            blogName.setText(
                    StringUtils.unescapeHTML(blogNames[position]));
            blogUsername.setText(
                    StringUtils.unescapeHTML(username));
            blavatar.setErrorImageResId(R.drawable.blavatar_placeholder);
            blavatar.setImageUrl(blavatars[position], WordPress.imageLoader);

            return view;

        }

    }
}
