package org.wordpress.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.FluxCUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * An activity to handle share intents, since there are multiple actions possible.
 * If there are multiple blogs, it lets the user choose which blog to share to.
 * It lists what actions that the user can perform and redirects them to the activity,
 * along with the content passed in the intent
 */
public class ShareIntentReceiverActivity extends AppCompatActivity implements OnItemSelectedListener {
    public static final String SHARE_LAST_USED_BLOG_ID_KEY = "wp-settings-share-last-used-text-blogid";
    public static final String SHARE_LAST_USED_ADDTO_KEY = "wp-settings-share-last-used-image-addto";

    public static final int ADD_TO_NEW_POST = 0;
    public static final int ADD_TO_MEDIA_LIBRARY = 1;
    public static final int SHARE_MEDIA_PERMISSION_REQUEST_CODE = 1;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private Spinner mBlogSpinner;
    private Spinner mActionSpinner;
    private int mSiteIds[];
    private int mSelectedSiteLocalId;
    private TextView mBlogSpinnerTitle;
    private int mActionIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.share_intent_receiver_dialog);

        mBlogSpinnerTitle = (TextView) findViewById(R.id.blog_spinner_title);
        mBlogSpinner = (Spinner) findViewById(R.id.blog_spinner);
        String[] blogNames = getBlogNames();
        if (blogNames == null) {
            finishIfNoVisibleBlogs();
            return;
        }

        if (blogNames.length == 1) {
            mBlogSpinner.setVisibility(View.GONE);
            mBlogSpinnerTitle.setVisibility(View.GONE);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                    R.layout.spinner_menu_dropdown_item, blogNames);
            mBlogSpinner.setAdapter(adapter);
            mBlogSpinner.setOnItemSelectedListener(this);
        }

        // If type is text/plain hide Media Gallery option
        mActionSpinner = (Spinner) findViewById(R.id.action_spinner);
        if (isSharingText()) {
            mActionSpinner.setVisibility(View.GONE);
            findViewById(R.id.action_spinner_title).setVisibility(View.GONE);
            // if text/plain and only one blog, then don't show this fragment, share it directly to a new post
            if (blogNames.length == 1) {
                Intent intent = new Intent(this, EditPostActivity.class);
                // Single site, pick the first one
                intent.putExtra(WordPress.SITE, mSiteStore.getSiteByLocalId(mSiteIds[0]));
                startActivity(intent);
                finish();
            }
        } else {
            String[] actions = new String[]{getString(R.string.share_action_post), getString(
                    R.string.share_action_media)};
            ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(this,
                    R.layout.spinner_menu_dropdown_item, actions);
            mActionSpinner.setAdapter(actionAdapter);
            mActionSpinner.setOnItemSelectedListener(this);
        }
        loadLastUsed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // noop
    }

    private void finishIfNoVisibleBlogs() {
        // If not logged in, then ask to log in, else inform the user to set at least one blog
        // visible
        if (FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            ToastUtils.showToast(getBaseContext(), R.string.no_account, ToastUtils.Duration.LONG);
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            ToastUtils.showToast(getBaseContext(), R.string.cant_share_no_visible_blog, ToastUtils.Duration.LONG);
            finish();
        }
    }

    private int getPositionBySiteId(long localBlogId) {
        for (int i = 0; i < mSiteIds.length; i++) {
            if (mSiteIds[i] == localBlogId) {
                return i;
            }
        }
        return -1;
    }

    private void loadLastUsed() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int localBlogId = settings.getInt(SHARE_LAST_USED_BLOG_ID_KEY, -1);
        int actionPosition = settings.getInt(SHARE_LAST_USED_ADDTO_KEY, -1);
        if (localBlogId != -1) {
            int position = getPositionBySiteId(localBlogId);
            if (position != -1) {
                mBlogSpinner.setSelection(position);
            }
        }
        if (actionPosition >= 0 && actionPosition < mActionSpinner.getCount()) {
            mActionSpinner.setSelection(actionPosition);
        }
    }

    private boolean isSharingText() {
        return "text/plain".equals(getIntent().getType());
    }

    private String[] getBlogNames() {
        List<SiteModel> sites = mSiteStore.getVisibleSites();
        if (sites.size() > 0) {
            final String siteNames[] = new String[sites.size()];
            mSiteIds = new int[sites.size()];
            int i = 0;
            for (SiteModel site : sites) {
                siteNames[i] = SiteUtils.getSiteNameOrHomeURL(site);
                mSiteIds[i] = site.getId();
                i += 1;
            }
            return siteNames;
        }
        return null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.blog_spinner) {
            mSelectedSiteLocalId = mSiteIds[position];
        } else if (parent.getId() == R.id.action_spinner) {
            mActionIndex = position;
        }
    }

    private void startActivityAndFinish(Intent intent) {
        String action = getIntent().getAction();
        if (intent != null) {
            intent.setAction(action);
            intent.setType(getIntent().getType());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            intent.putExtra(WordPress.SITE, mSiteStore.getSiteByLocalId(mSelectedSiteLocalId));

            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
            intent.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT));

            if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                ArrayList<Uri> extra = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
                intent.putExtra(Intent.EXTRA_STREAM, extra);
            } else {
                Uri extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                intent.putExtra(Intent.EXTRA_STREAM, extra);
            }
            savePreferences();
            startActivity(intent);
            finish();
        }
    }

    public void onShareClicked(View view) {
        shareIt();
    }

    /**
     * Start the correct activity if permissions are granted
     *
     * @return true if the activity has been started, false else.
     */
    private boolean shareIt() {
        Intent intent = null;
        if (!isSharingText()) {
            // If we're sharing media, we must check we have Storage permission (needed for media upload).
            if (!PermissionUtils.checkAndRequestStoragePermission(this, SHARE_MEDIA_PERMISSION_REQUEST_CODE)) {
                return false;
            }
        }
        if (mActionIndex == ADD_TO_NEW_POST) {
            // new post
            intent = new Intent(this, EditPostActivity.class);
        } else if (mActionIndex == ADD_TO_MEDIA_LIBRARY) {
            // add to media gallery
            intent = new Intent(this, MediaBrowserActivity.class);
        }
        startActivityAndFinish(intent);
        return true;
    }

    private void savePreferences() {
        // If current blog is not set don't save preferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        // Save last used settings
        editor.putInt(SHARE_LAST_USED_BLOG_ID_KEY, mSelectedSiteLocalId);
        editor.putInt(SHARE_LAST_USED_ADDTO_KEY, mActionIndex);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case SHARE_MEDIA_PERMISSION_REQUEST_CODE:
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        ToastUtils.showToast(this, getString(R.string.add_media_permission_required));
                        return;
                    }
                }
                shareIt();
                break;
            default:
                break;
        }
    }
}
