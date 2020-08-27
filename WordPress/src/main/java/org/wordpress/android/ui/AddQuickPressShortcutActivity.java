package org.wordpress.android.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AddQuickPressShortcutActivity extends LocaleAwareActivity {
    public String[] blogNames;
    public int[] siteIds;
    public String[] blogUrls;
    public String[] blavatars;
    public List<String> accountNames = new ArrayList<>();

    @Inject SiteStore mSiteStore;
    @Inject FluxCImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.quickpress_widget_configure_activity);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        displayAccounts();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayAccounts() {
        List<SiteModel> sites = mSiteStore.getVisibleSites();

        ListView listView = (ListView) findViewById(android.R.id.list);

        listView.setVerticalFadingEdgeEnabled(false);
        listView.setVerticalScrollBarEnabled(true);

        if (sites.size() > 0) {
            blogNames = new String[sites.size()];
            siteIds = new int[sites.size()];
            blogUrls = new String[sites.size()];
            blavatars = new String[sites.size()];
            for (int i = 0; i < sites.size(); i++) {
                SiteModel site = sites.get(i);
                blogNames[i] = SiteUtils.getSiteNameOrHomeURL(site);
                blogUrls[i] = site.getUrl();
                siteIds[i] = site.getId();
                blavatars[i] = SiteUtils.getSiteIconUrl(site, 60);
                accountNames.add(i, blogNames[i]);
            }

            listView.setAdapter(new HomeListAdapter());

            listView.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View row, int position, long id) {
                    AddQuickPressShortcutActivity.this.buildDialog(position);
                }
            });

            if (sites.size() == 1) {
                AddQuickPressShortcutActivity.this.buildDialog(0);
            }
        } else {
            // no account, load new account view
            ActivityLauncher.showSignInForResult(AddQuickPressShortcutActivity.this);
        }
    }

    private void buildDialog(final int position) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(R.string.quickpress_add_alert_title);

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        //noinspection InflateParams
        View dialogView = layoutInflater.inflate(R.layout.quick_press_input_dialog, null);

        TextInputEditText quickPressShortcutName = dialogView.findViewById(R.id.quick_press_input_dialog_edit_text);
        quickPressShortcutName.setText(getString(R.string.quickpress_shortcut_with_account_param,
                StringEscapeUtils.unescapeHtml4(accountNames.get(position))));

        dialogBuilder.setView(dialogView);

        dialogBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (TextUtils.isEmpty(quickPressShortcutName.getText())) {
                    ToastUtils.showToast(AddQuickPressShortcutActivity.this, R.string.quickpress_add_error,
                            ToastUtils.Duration.LONG);
                } else {
                    Intent shortcutIntent = new Intent(getApplicationContext(), EditPostActivity.class);
                    shortcutIntent.setAction(Intent.ACTION_MAIN);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_QUICKPRESS_BLOG_ID, siteIds[position]);
                    shortcutIntent.putExtra(EditPostActivity.EXTRA_IS_QUICKPRESS, true);

                    String shortcutName = quickPressShortcutName.getText().toString();

                    WordPress.wpDB.addQuickPressShortcut(siteIds[position], shortcutName);

                    ShortcutInfoCompat pinShortcutInfo =
                            new ShortcutInfoCompat.Builder(getApplicationContext(), shortcutName)
                                    .setIcon(IconCompat.createWithResource(getApplicationContext(), R.mipmap.app_icon))
                                    .setShortLabel(shortcutName)
                                    .setIntent(shortcutIntent)
                                    .build();

                    ShortcutManagerCompat.requestPinShortcut(getApplicationContext(), pinShortcutInfo, null);

                    AddQuickPressShortcutActivity.this.finish();
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            // just let the dialog close
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.ADD_ACCOUNT:
                if (resultCode == RESULT_OK) {
                    if (mSiteStore.getVisibleSitesCount() > 0) {
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
            return mSiteStore.getVisibleSitesCount();
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
                view = (RelativeLayout) inflater.inflate(R.layout.quick_press_widget_configure_list_row, parent, false);
            }
            view.setId(siteIds[position]);

            TextView blogName = (TextView) view.findViewById(R.id.blogName);
            TextView blogUrl = (TextView) view.findViewById(R.id.blogUrl);
            NetworkImageView blavatar = (NetworkImageView) view.findViewById(R.id.blavatar);
            blavatar.setDefaultImageResId(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp);

            blogName.setText(
                    StringEscapeUtils.unescapeHtml4(blogNames[position]));
            blogUrl.setText(
                    StringEscapeUtils.unescapeHtml4(blogUrls[position]));
            blavatar.setErrorImageResId(R.drawable.bg_rectangle_placeholder_globe_32dp);
            blavatar.setImageUrl(blavatars[position], mImageLoader);

            return view;
        }
    }
}
