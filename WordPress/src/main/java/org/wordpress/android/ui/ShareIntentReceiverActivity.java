package org.wordpress.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An activity to handle share intents, since there are multiple actions possible.
 * If there are multiple blogs, it lets the user choose which blog to share to.
 * It lists what actions that the user can perform and redirects them to the activity,
 * along with the content passed in the intent
 */
public class ShareIntentReceiverActivity extends Activity implements OnItemSelectedListener {
    public static final String SHARE_TEXT_BLOG_ID_KEY = "wp-settings-share-text-blogid";
    public static final String SHARE_IMAGE_BLOG_ID_KEY = "wp-settings-share-image-blogid";
    public static final String SHARE_IMAGE_ADDTO_KEY = "wp-settings-share-image-addto";
    public static final String SHARE_LAST_USED_BLOG_ID_KEY = "wp-settings-share-last-used-text-blogid";
    public static final String SHARE_LAST_USED_ADDTO_KEY = "wp-settings-share-last-used-image-addto";
    public static final int ADD_TO_NEW_POST = 0;
    public static final int ADD_TO_MEDIA_LIBRARY = 1;
    private Spinner mBlogSpinner;
    private Spinner mActionSpinner;
    private CheckedTextView mAlwaysUseCheckBox;
    private int mAccountIDs[];
    private TextView mBlogSpinnerTitle;
    private int mActionIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.share_intent_receiver_dialog);

        mBlogSpinnerTitle = (TextView) findViewById(R.id.blog_spinner_title);
        mBlogSpinner = (Spinner) findViewById(R.id.blog_spinner);
        mAlwaysUseCheckBox = (CheckedTextView) findViewById(R.id.always_use_checkbox);
        mAlwaysUseCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlwaysUseCheckBox.setChecked(!mAlwaysUseCheckBox.isChecked());
            }
        });
        String[] blogNames = getBlogNames();
        if (blogNames == null) {
            finishIfNoVisibleBlogs();
            return;
        }

        if (autoShareIfEnabled()) {
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
                startActivityAndFinish(new Intent(this, EditPostActivity.class));
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
        // If not signed in, then ask to sign in, else inform the user to set at least one blog
        // visible
        if (!WordPress.isSignedIn(getBaseContext())) {
            ToastUtils.showToast(getBaseContext(), R.string.no_account, ToastUtils.Duration.LONG);
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        } else {
            ToastUtils.showToast(getBaseContext(), R.string.cant_share_no_visible_blog, ToastUtils.Duration.LONG);
            finish();
        }
    }

    private int gepPositionByLocalBlogId(long localBlogId) {
        for (int i = 0; i < mAccountIDs.length; i++) {
            if (mAccountIDs[i] == localBlogId) {
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
            int position = gepPositionByLocalBlogId(localBlogId);
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
        List<Map<String, Object>> accounts = WordPress.wpDB.getVisibleAccounts();
        if (accounts.size() > 0) {
            final String blogNames[] = new String[accounts.size()];
            mAccountIDs = new int[accounts.size()];
            Blog blog;
            for (int i = 0; i < accounts.size(); i++) {
                Map<String, Object> account = accounts.get(i);
                blogNames[i] = BlogUtils.getBlogNameFromAccountMap(account);
                mAccountIDs[i] = (Integer) account.get("id");
                blog = WordPress.wpDB.instantiateBlogByLocalId(mAccountIDs[i]);
                if (blog == null) {
                    ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
                    return null;
                }
            }
            return blogNames;
        }
        return null;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.blog_spinner) {
            if (!selectBlog(mAccountIDs[position])) {
                ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
                finish();
            }
        } else if (parent.getId() == R.id.action_spinner) {
            mActionIndex = position;
        }
    }

    private boolean selectBlog(int blogId) {
        WordPress.currentBlog = WordPress.wpDB.instantiateBlogByLocalId(blogId);
        if (WordPress.currentBlog == null || WordPress.currentBlog.isHidden()) {
            return false;
        }
        WordPress.wpDB.updateLastBlogId(WordPress.currentBlog.getLocalTableBlogId());
        return true;
    }

    private void startActivityAndFinish(Intent intent) {
        String action = getIntent().getAction();
        if (intent != null) {
            intent.setAction(action);
            intent.setType(getIntent().getType());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

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

    private void shareIt() {
        Intent intent = null;
        if (mActionIndex == ADD_TO_NEW_POST) {
            // new post
            intent = new Intent(this, EditPostActivity.class);
        } else if (mActionIndex == ADD_TO_MEDIA_LIBRARY) {
            // add to media gallery
            intent = new Intent(this, MediaBrowserActivity.class);
        }
        startActivityAndFinish(intent);
    }

    private boolean autoShareIfEnabled() {
        if (isSharingText()) {
            return autoShareText();
        } else {
            return autoShareImage();
        }
    }

    private boolean autoShareText() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int blogId = settings.getInt(SHARE_TEXT_BLOG_ID_KEY, -1);
        if (blogId != -1) {
            mActionIndex = ADD_TO_NEW_POST;
            if (selectBlog(blogId)) {
                shareIt();
                return true;
            } else {
                // blog is hidden or has been deleted, reset settings
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.remove(SHARE_TEXT_BLOG_ID_KEY);
                editor.commit();
                ToastUtils.showToast(this, R.string.auto_sharing_preference_reset_caused_by_error,
                        ToastUtils.Duration.LONG);
            }
        }
        return false;
    }

    private boolean autoShareImage() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int blogId = settings.getInt(SHARE_IMAGE_BLOG_ID_KEY, -1);
        int addTo = settings.getInt(SHARE_IMAGE_ADDTO_KEY, -1);
        if (blogId != -1 && addTo != -1) {
            mActionIndex = addTo;
            if (selectBlog(blogId)) {
                shareIt();
                return true;
            } else {
                // blog is hidden or has been deleted, reset settings
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.remove(SHARE_IMAGE_BLOG_ID_KEY);
                editor.remove(SHARE_IMAGE_ADDTO_KEY);
                editor.commit();
                ToastUtils.showToast(this, R.string.auto_sharing_preference_reset_caused_by_error,
                        ToastUtils.Duration.LONG);
            }
        }
        return false;
    }

    private void savePreferences() {
        // If current blog is not set don't save preferences
        if (WordPress.currentBlog == null) {
            return ;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

        // Save last used settings
        editor.putInt(SHARE_LAST_USED_BLOG_ID_KEY, WordPress.currentBlog.getLocalTableBlogId());
        editor.putInt(SHARE_LAST_USED_ADDTO_KEY, mActionIndex);

        // Save "always use these settings"
        if (mAlwaysUseCheckBox.isChecked()) {
            if (isSharingText()) {
                editor.putInt(SHARE_TEXT_BLOG_ID_KEY, WordPress.currentBlog.getLocalTableBlogId());
            } else {
                editor.putInt(SHARE_IMAGE_BLOG_ID_KEY, WordPress.currentBlog.getLocalTableBlogId());
                // Add to new post or media
                editor.putInt(SHARE_IMAGE_ADDTO_KEY, mActionIndex);
            }
        }
        editor.commit();
    }
}
