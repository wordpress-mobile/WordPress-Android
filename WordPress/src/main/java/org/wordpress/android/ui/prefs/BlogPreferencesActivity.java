package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.CoreEvents.UserSignedOutCompletely;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.networking.ConnectionChangeReceiver;

import de.greenrobot.event.EventBus;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends AppCompatActivity {
    public static final String ARG_LOCAL_BLOG_ID = SiteSettingsFragment.ARG_LOCAL_BLOG_ID;
    public static final int RESULT_BLOG_REMOVED = RESULT_FIRST_USER;

    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";

    // The blog this activity is managing settings for.
    private Blog blog;
    private boolean mBlogDeleted;
    private EditText mUsernameET;
    private EditText mPasswordET;
    private EditText mHttpUsernameET;
    private EditText mHttpPasswordET;
    private CheckBox mFullSizeCB;
    private CheckBox mScaledCB;
    private Spinner mImageWidthSpinner;
    private EditText mScaledImageWidthET;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Integer id = getIntent().getIntExtra(ARG_LOCAL_BLOG_ID, -1);
        blog = WordPress.getBlog(id);
        if (WordPress.getBlog(id) == null) {
            Toast.makeText(this, getString(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (blog.isDotcomFlag()) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            FragmentManager fragmentManager = getFragmentManager();
            Fragment siteSettingsFragment = fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

            if (siteSettingsFragment == null) {
                siteSettingsFragment = new SiteSettingsFragment();
                siteSettingsFragment.setArguments(getIntent().getExtras());
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, siteSettingsFragment, KEY_SETTINGS_FRAGMENT)
                        .commit();
            }
        } else {
            setContentView(R.layout.blog_preferences);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(StringUtils.unescapeHTML(blog.getNameOrHostUrl()));
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            mUsernameET = (EditText) findViewById(R.id.username);
            mPasswordET = (EditText) findViewById(R.id.password);
            mHttpUsernameET = (EditText) findViewById(R.id.httpuser);
            mHttpPasswordET = (EditText) findViewById(R.id.httppassword);
            mScaledImageWidthET = (EditText) findViewById(R.id.scaledImageWidth);
            mFullSizeCB = (CheckBox) findViewById(R.id.fullSizeImage);
            mScaledCB = (CheckBox) findViewById(R.id.scaledImage);
            mImageWidthSpinner = (Spinner) findViewById(R.id.maxImageWidth);
            Button removeBlogButton = (Button) findViewById(R.id.remove_account);

            // remove blog & credentials apply only to dot org
            if (blog.isDotcomFlag()) {
                View credentialsRL = findViewById(R.id.sectionContent);
                credentialsRL.setVisibility(View.GONE);
                removeBlogButton.setVisibility(View.GONE);
            } else {
                removeBlogButton.setVisibility(View.VISIBLE);
                removeBlogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        removeBlogWithConfirmation();
                    }
                });
            }

            loadSettingsForBlog();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (blog.isDotcomFlag() || mBlogDeleted) {
            return;
        }

        blog.setUsername(mUsernameET.getText().toString());
        blog.setPassword(mPasswordET.getText().toString());
        blog.setHttpuser(mHttpUsernameET.getText().toString());
        blog.setHttppassword(mHttpPasswordET.getText().toString());

        blog.setFullSizeImage(mFullSizeCB.isChecked());
        blog.setScaledImage(mScaledCB.isChecked());
        if (blog.isScaledImage()) {
            EditText scaledImgWidth = (EditText) findViewById(R.id.scaledImageWidth);

            boolean error = false;
            int width = 0;
            try {
                width = Integer.parseInt(scaledImgWidth.getText().toString().trim());
            } catch (NumberFormatException e) {
                error = true;
            }

            if (width == 0) {
                error = true;
            }

            if (error) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(BlogPreferencesActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.error));
                dialogBuilder.setMessage(getResources().getText(R.string.scaled_image_error));
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                dialogBuilder.setCancelable(true);
                dialogBuilder.create().show();
                return;
            } else {
                blog.setScaledImageWidth(width);
            }
        }

        blog.setMaxImageWidth(mImageWidthSpinner.getSelectedItem().toString());

        WordPress.wpDB.saveBlog(blog);

        if (WordPress.getCurrentBlog().getLocalTableBlogId() == blog.getLocalTableBlogId()) {
            WordPress.currentBlog = blog;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        FragmentManager fragmentManager = getFragmentManager();
        SiteSettingsFragment siteSettingsFragment =
                (SiteSettingsFragment) fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

        if (siteSettingsFragment != null) {
            if (!event.isConnected()) {
                ToastUtils.showToast(this, getString(R.string.site_settings_disconnected_toast));
            }
            siteSettingsFragment.setEditingEnabled(event.isConnected());

            // TODO: add this back when delete blog is back
            //https://github.com/wordpress-mobile/WordPress-Android/commit/6a90e3fe46e24ee40abdc4a7f8f0db06f157900c
            // Checks for stats widgets that were synched with a blog that could be gone now.
//            StatsWidgetProvider.updateWidgetsOnLogout(this);
        }
    }

    private void loadSettingsForBlog() {
        ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
                R.layout.simple_spinner_item, new String[]{
                "Original Size", "100", "200", "300", "400", "500", "600", "700", "800",
                "900", "1000", "1100", "1200", "1300", "1400", "1500", "1600", "1700",
                "1800", "1900", "2000"
        });
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mImageWidthSpinner.setAdapter(spinnerArrayAdapter);
        mImageWidthSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CheckBox fullSizeImageCheckBox = (CheckBox) findViewById(R.id.fullSizeImage);
                // Original size selected. Do not show the link to full image.
                if (id == 0) {
                    fullSizeImageCheckBox.setVisibility(View.GONE);
                } else {
                    fullSizeImageCheckBox.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        mUsernameET.setText(blog.getUsername());
        mPasswordET.setText(blog.getPassword());
        mHttpUsernameET.setText(blog.getHttpuser());
        mHttpPasswordET.setText(blog.getHttppassword());
        TextView httpUserLabel = (TextView) findViewById(R.id.l_httpuser);
        if (blog.isDotcomFlag()) {
            mHttpUsernameET.setVisibility(View.GONE);
            mHttpPasswordET.setVisibility(View.GONE);
            httpUserLabel.setVisibility(View.GONE);
        } else {
            mHttpUsernameET.setVisibility(View.VISIBLE);
            mHttpPasswordET.setVisibility(View.VISIBLE);
            httpUserLabel.setVisibility(View.VISIBLE);
        }

        mFullSizeCB.setChecked(blog.isFullSizeImage());
        mScaledCB.setChecked(blog.isScaledImage());

        this.mScaledImageWidthET.setText("" + blog.getScaledImageWidth());
        showScaledSetting(blog.isScaledImage());

        CheckBox scaledImage = (CheckBox) findViewById(R.id.scaledImage);
        scaledImage.setChecked(false);
        scaledImage.setVisibility(View.GONE);

        // sets up a state listener for the full-size checkbox
        CheckBox fullSizeImageCheckBox = (CheckBox) findViewById(R.id.fullSizeImage);
        fullSizeImageCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox fullSize = (CheckBox) findViewById(R.id.fullSizeImage);
                if (fullSize.isChecked()) {
                    CheckBox scaledImage = (CheckBox) findViewById(R.id.scaledImage);
                    if (scaledImage.isChecked()) {
                        scaledImage.setChecked(false);
                        showScaledSetting(false);
                    }
                }
            }
        });

        int imageWidthPosition = spinnerArrayAdapter.getPosition(blog.getMaxImageWidth());
        mImageWidthSpinner.setSelection((imageWidthPosition >= 0) ? imageWidthPosition : 0);
        if (mImageWidthSpinner.getSelectedItemPosition() ==
                0) //Original size selected. Do not show the link to full image.
        {
            fullSizeImageCheckBox.setVisibility(View.GONE);
        } else {
            fullSizeImageCheckBox.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hides / shows the scaled image settings
     */
    private void showScaledSetting(boolean show) {
        TextView tw = (TextView) findViewById(R.id.l_scaledImage);
        EditText et = (EditText) findViewById(R.id.scaledImageWidth);
        tw.setVisibility(show ? View.VISIBLE : View.GONE);
        et.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Remove the blog this activity is managing settings for.
     */
    private void removeBlogWithConfirmation() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                removeBlog();
            }
        });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }

    private void removeBlog() {
        if (WordPress.wpDB.deleteBlog(this, blog.getLocalTableBlogId())) {
            StatsTable.deleteStatsForBlog(this,blog.getLocalTableBlogId()); // Remove stats data
            AnalyticsUtils.refreshMetadata();
            ToastUtils.showToast(this, R.string.blog_removed_successfully);
            WordPress.wpDB.deleteLastBlogId();
            WordPress.currentBlog = null;
            mBlogDeleted = true;
            setResult(RESULT_BLOG_REMOVED);

            // If the last blog is removed and the user is not signed in wpcom, broadcast a UserSignedOut event
            if (!AccountHelper.isSignedIn()) {
                EventBus.getDefault().post(new UserSignedOutCompletely());
            }

            // Checks for stats widgets that were synched with a blog that could be gone now.
            StatsWidgetProvider.updateWidgetsOnLogout(this);

            finish();
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            dialogBuilder.setTitle(getResources().getText(R.string.error));
            dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
            dialogBuilder.setPositiveButton("OK", null);
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        }
    }
}
