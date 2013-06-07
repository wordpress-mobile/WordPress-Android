package org.wordpress.android.ui.prefs;

import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.EscapeUtils;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends SherlockFragmentActivity {
    protected static Intent svc = null;
    private String originalUsername;

    /** The blog this activity is managing settings for. */
    private Blog blog;
    private boolean mBlogDeleted;
    private EditText mUsernameET;
    private EditText mPasswordET;
    private EditText mHttpUsernameET;
    private EditText mHttpPasswordET;
    private CheckBox mFullSizeCB;
    private CheckBox mScaledCB;
    private CheckBox mLocationCB;
    private Spinner mImageWidthSpinner;
    private EditText mScaledImageWidthET;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blog_preferences);

        Integer id = getIntent().getIntExtra("id", -1);
        blog = WordPress.getBlog(id);

        if (blog == null) {
            Toast.makeText(this, getString(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        getSupportActionBar().setTitle(EscapeUtils.unescapeHtml(blog.getBlogName()));
        
        mUsernameET = (EditText) findViewById(R.id.username);
        mPasswordET = (EditText) findViewById(R.id.password);
        mHttpUsernameET = (EditText) findViewById(R.id.httpuser);
        mHttpPasswordET = (EditText) findViewById(R.id.httppassword);
        mScaledImageWidthET = (EditText) findViewById(R.id.scaledImageWidth);
        mFullSizeCB = (CheckBox) findViewById(R.id.fullSizeImage);
        mScaledCB = (CheckBox) findViewById(R.id.scaledImage);
        mLocationCB = (CheckBox) findViewById(R.id.location);
        mImageWidthSpinner = (Spinner) findViewById(R.id.maxImageWidth);
        
        if (blog.isDotcomFlag()) {
            // Hide credentials section
            RelativeLayout credentialsRL = (RelativeLayout)findViewById(R.id.section1);
            credentialsRL.setVisibility(View.GONE);
        }
        
        loadSettingsForBlog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        if (mBlogDeleted)
            return;
        
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

            if (width == 0)
                error = true;

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

        long maxImageWidthId = mImageWidthSpinner.getSelectedItemId();
        int maxImageWidthIdInt = (int) maxImageWidthId;

        blog.setMaxImageWidthId(maxImageWidthIdInt);

        blog.setLocation(mLocationCB.isChecked());

        blog.save(originalUsername);
        
        if (WordPress.getCurrentBlog().getId() == blog.getId())
            WordPress.currentBlog = blog;
        
        // exit settings screen
        Bundle bundle = new Bundle();

        bundle.putString("returnStatus", "SAVE");
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    private void loadSettingsForBlog() {
        // Set header labels to upper case
        ((TextView) findViewById(R.id.l_section1)).setText(getResources().getString(R.string.account_details).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_section2)).setText(getResources().getString(R.string.media).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_section3)).setText(getResources().getString(R.string.location).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_maxImageWidth)).setText(getResources().getString(R.string.max_thumbnail_px_width).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_httpuser)).setText(getResources().getString(R.string.http_credentials).toUpperCase(Locale.getDefault()));

        ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
                R.layout.spinner_textview, new String[] {
                        "Original Size", "100", "200", "300", "400", "500", "600", "700", "800",
                        "900", "1000", "1100", "1200", "1300", "1400", "1500", "1600", "1700",
                        "1800", "1900", "2000"
                });
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mImageWidthSpinner.setAdapter(spinnerArrayAdapter);

        mUsernameET.setText(blog.getUsername());
        originalUsername = blog.getUsername();
        mPasswordET.setText(blog.getPassword());
        mHttpUsernameET.setText(blog.getHttpuser());
        mHttpPasswordET.setText(blog.getHttppassword());
        TextView httpUserLabel = (TextView)findViewById(R.id.l_httpuser);
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
        // sets up a state listener for the scaled image checkbox
        ((CheckBox) findViewById(R.id.scaledImage)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox scaledImage = (CheckBox) findViewById(R.id.scaledImage);
                showScaledSetting(scaledImage.isChecked());
                if (scaledImage.isChecked()) {
                    CheckBox fullSize = (CheckBox) findViewById(R.id.fullSizeImage);
                    fullSize.setChecked(false);
                }
            }
        });
        // sets up a state listener for the fullsize checkbox
        ((CheckBox) findViewById(R.id.fullSizeImage)).setOnClickListener(new View.OnClickListener() {
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
        // don't show location option for devices that have no location support.
        boolean hasLocationProvider = false;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        for (String providerName : providers) {
            if (providerName.equals(LocationManager.GPS_PROVIDER) || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
                hasLocationProvider = true;
            }
        }

        if (hasLocationProvider) {
            mLocationCB.setChecked(blog.isLocation());
        } else {
            mLocationCB.setChecked(false);
            RelativeLayout locationLayout = (RelativeLayout) findViewById(R.id.section3);
            locationLayout.setVisibility(View.GONE);
        }

        mImageWidthSpinner.setSelection(blog.getMaxImageWidthId());
    }

    /**
     * Hides / shows the scaled image settings
     *
     * @param show
     */
    private void showScaledSetting(boolean show) {
        TextView tw = (TextView) findViewById(R.id.l_scaledImage);
        EditText et = (EditText) findViewById(R.id.scaledImageWidth);
        tw.setVisibility(show ? View.VISIBLE : View.GONE);
        et.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    /**
     * Remove the blog this activity is managing settings for.
     */
    public void removeBlog(View view) {
        final BlogPreferencesActivity activity = this;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                boolean deleteSuccess = WordPress.wpDB.deleteAccount(BlogPreferencesActivity.this, blog.getId());
                if (deleteSuccess) {
                    Toast.makeText(activity, getResources().getText(R.string.blog_removed_successfully), Toast.LENGTH_SHORT)
                            .show();
                    WordPress.wpDB.deleteLastBlogId();
                    WordPress.currentBlog = null;
                    mBlogDeleted = true;
                    activity.finish();
                } else {
                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
                    dialogBuilder.setTitle(getResources().getText(R.string.error));
                    dialogBuilder.setMessage(getResources().getText(R.string.could_not_remove_account));
                    dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // just close the dialog
                        }
                    });
                    dialogBuilder.setCancelable(true);
                    dialogBuilder.create().show();
                }
            }
        });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // just close the window
            }
        });
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }
}
