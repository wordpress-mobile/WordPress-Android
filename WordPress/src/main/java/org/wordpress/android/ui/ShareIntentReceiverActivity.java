package org.wordpress.android.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
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
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPermissionUtils;

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

    private static final int ADD_TO_NEW_POST = 0;
    private static final int ADD_TO_MEDIA_LIBRARY = 1;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    private Spinner mBlogSpinner;
    private RadioGroup mActionGroup;
    private String mSiteNames[];
    private int mSiteIds[];
    private int mSelectedSiteLocalId;
    private int mActionIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.share_intent_receiver_dialog);

        TextView blogSpinnerTitle = (TextView) findViewById(R.id.blog_spinner_title);
        mBlogSpinner = (Spinner) findViewById(R.id.blog_spinner);
        mActionGroup = (RadioGroup) findViewById(R.id.share_actions);

        initSiteLists();

        if (mSiteNames == null) {
            finishIfNoVisibleBlogs();
            return;
        }

        if (mSiteNames.length == 1) {
            mBlogSpinner.setVisibility(View.GONE);
            blogSpinnerTitle.setVisibility(View.GONE);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_menu_dropdown_item, mSiteNames);
            mBlogSpinner.setAdapter(adapter);
            mBlogSpinner.setOnItemSelectedListener(this);
        }

        loadLastUsed();

        // If type is text/plain hide Media Gallery option
        if (isSharingText()) {
            mActionIndex = ADD_TO_NEW_POST;
            mActionGroup.setVisibility(View.GONE);
            findViewById(R.id.action_spinner_title).setVisibility(View.GONE);
            // if text/plain and only one blog, then don't show this fragment, share it directly to a new post
            if (mSiteNames.length == 1) {
                // Single site, startActivityAndFinish will pick the first one by default
                startActivityAndFinish(new Intent(this, EditPostActivity.class));
            }
        } else {
            mActionGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                    mActionIndex = getActionIndex(checkedId);
                }
            });
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.blog_spinner) {
            mSelectedSiteLocalId = mSiteIds[position];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // nop
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.SHARE_MEDIA_PERMISSION_REQUEST_CODE) {
            shareIt();
        }
    }

    /**
     * Callback for "Share" button.
     */
    public void onShareClicked(View view) {
        shareIt();
    }

    /**
     * Callback for "Cancel" button.
     */
    public void onCancelClicked(View view) {
        finish();
    }

    private void finishIfNoVisibleBlogs() {
        // If not logged in, then ask to log in, else inform the user to set at least one blog visible
        if (!FluxCUtils.isSignedInWPComOrHasWPOrgSite(mAccountStore, mSiteStore)) {
            ToastUtils.showToast(getBaseContext(), R.string.no_account, ToastUtils.Duration.LONG);
            startActivity(new Intent(this, SignInActivity.class));
            finish();
        } else {
            ToastUtils.showToast(getBaseContext(), R.string.cant_share_no_visible_blog, ToastUtils.Duration.LONG);
            finish();
        }
    }

    private int getActionIndex(int actionId) {
        if (actionId == R.id.media_library_share_action) {
            return ADD_TO_MEDIA_LIBRARY;
        }
        return ADD_TO_NEW_POST;
    }

    private int getActionId(int actionIndex) {
        if (actionIndex == ADD_TO_MEDIA_LIBRARY) {
            return R.id.media_library_share_action;
        }
        return R.id.new_post_share_action;
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
        if (localBlogId != -1) {
            int position = getPositionBySiteId(localBlogId);
            if (position != -1) {
                mBlogSpinner.setSelection(position);
            }
        }
        mActionIndex = settings.getInt(SHARE_LAST_USED_ADDTO_KEY, ADD_TO_NEW_POST);
        if (mActionIndex < 0 || mActionIndex >= mActionGroup.getChildCount()) {
            mActionIndex = ADD_TO_NEW_POST;
        }
        mActionGroup.check(getActionId(mActionIndex));
    }

    private boolean isSharingText() {
        return "text/plain".equals(getIntent().getType());
    }

    private void initSiteLists() {
        List<SiteModel> sites = mSiteStore.getVisibleSites();
        if (sites.size() > 0) {
            mSiteNames = new String[sites.size()];
            mSiteIds = new int[sites.size()];
            int i = 0;
            for (SiteModel site : sites) {
                mSiteNames[i] = SiteUtils.getSiteNameOrHomeURL(site);
                mSiteIds[i] = site.getId();
                i += 1;
            }
            // default selected site to the first one
            mSelectedSiteLocalId = mSiteIds[0];
        }
    }

    private void startActivityAndFinish(@NonNull Intent intent) {
        String action = getIntent().getAction();
        intent.setAction(action);
        intent.setType(getIntent().getType());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        intent.putExtra(WordPress.SITE, mSiteStore.getSiteByLocalId(mSelectedSiteLocalId));
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserActivity.MediaBrowserType.BROWSER);

        intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
        intent.putExtra(Intent.EXTRA_SUBJECT, getIntent().getStringExtra(Intent.EXTRA_SUBJECT));

        if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> extra = getIntent().getParcelableArrayListExtra((Intent.EXTRA_STREAM));
            intent.putExtra(Intent.EXTRA_STREAM, extra);
        } else {
            Uri extra = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            intent.putExtra(Intent.EXTRA_STREAM, extra);
        }

        // save preferences
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(SHARE_LAST_USED_BLOG_ID_KEY, mSelectedSiteLocalId)
                .putInt(SHARE_LAST_USED_ADDTO_KEY, mActionIndex)
                .apply();

        startActivity(intent);
        finish();
    }

    /**
     * Start the correct activity if permissions are granted.
     */
    private void shareIt() {
        if (!isSharingText()) {
            // If we're sharing media, we must check we have Storage permission (needed for media upload).
            if (!PermissionUtils.checkAndRequestStoragePermission(this, WPPermissionUtils.SHARE_MEDIA_PERMISSION_REQUEST_CODE)) {
                return;
            }
        }

        if (mActionIndex == ADD_TO_NEW_POST) {
            startActivityAndFinish(new Intent(this, EditPostActivity.class));
        } else if (mActionIndex == ADD_TO_MEDIA_LIBRARY) {
            startActivityAndFinish(new Intent(this, MediaBrowserActivity.class));
        } else {
            ToastUtils.showToast(this, R.string.cant_share_unknown_action);
            finish();
        }
    }
}
