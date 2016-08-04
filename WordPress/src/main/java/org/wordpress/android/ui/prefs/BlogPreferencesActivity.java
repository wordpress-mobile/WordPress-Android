package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends AppCompatActivity {
    public static final String ARG_LOCAL_BLOG_ID = SiteSettingsFragment.ARG_LOCAL_BLOG_ID;
    public static final int RESULT_BLOG_REMOVED = RESULT_FIRST_USER;

    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";

    // The blog this activity is managing settings for.
    private boolean mBlogDeleted;
    private EditText mUsernameET;
    private EditText mPasswordET;
    private CheckBox mFullSizeCB;
    private CheckBox mScaledCB;
    private Spinner mImageWidthSpinner;
    private EditText mScaledImageWidthET;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;
    public @NonNull SiteModel getSelectedSite() {
        return mSite;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(ActivityLauncher.EXTRA_SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(ActivityLauncher.EXTRA_SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        if (mSite.isWPCom()) {
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
                actionBar.setTitle(StringUtils.unescapeHTML(SiteUtils.getSiteNameOrHomeURL(mSite)));
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            mUsernameET = (EditText) findViewById(R.id.username);
            mPasswordET = (EditText) findViewById(R.id.password);
            mScaledImageWidthET = (EditText) findViewById(R.id.scaledImageWidth);
            mFullSizeCB = (CheckBox) findViewById(R.id.fullSizeImage);
            mScaledCB = (CheckBox) findViewById(R.id.scaledImage);
            mImageWidthSpinner = (Spinner) findViewById(R.id.maxImageWidth);
            Button removeBlogButton = (Button) findViewById(R.id.remove_account);

            // remove blog & credentials apply only to dot org
            if (mSite.isWPCom()) {
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
    protected void onPause() {
        super.onPause();

        if (mSite.isWPCom() || mBlogDeleted) {
            return;
        }

        mSite.setUsername(mUsernameET.getText().toString());
        mSite.setPassword(mPasswordET.getText().toString());
        // TODO: STORES: setFullSizeImage
        // mSite.setFullSizeImage(mFullSizeCB.isChecked());
        // TODO: STORES: setScaledImage
        // mSite.setScaledImage(mScaledCB.isChecked());
        if (mScaledCB.isChecked()) {
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
                // TODO: STORES: setScaledImageWidth
                // mSite.setScaledImageWidth(width);
            }
        }
        // TODO: STORES: setMaxImageWidth
        // mSite.setMaxImageWidth(mImageWidthSpinner.getSelectedItem().toString());
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

        mUsernameET.setText(mSite.getUsername());
        mPasswordET.setText(mSite.getPassword());

        // TODO: STORES: isFullSizeImage
        // mFullSizeCB.setChecked(mSite.isFullSizeImage());
        // mScaledCB.setChecked(mSite.isScaledImage());
        // mScaledImageWidthET.setText("" + mSite.getScaledImageWidth());
        // showScaledSetting(mSite.isScaledImage());

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

        // TODO: STORES: getMaxImageWidth
        // int imageWidthPosition = spinnerArrayAdapter.getPosition(mSite.getMaxImageWidth());
        // mImageWidthSpinner.setSelection((imageWidthPosition >= 0) ? imageWidthPosition : 0);
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
                mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(mSite));
                mBlogDeleted = true;
            }
        });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }
}
