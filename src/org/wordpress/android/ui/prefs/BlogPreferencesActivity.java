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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blog_preferences);

        Integer id = getIntent().getIntExtra("id", -1);
        blog = WordPress.getBlog(id);

        getSupportActionBar().setTitle(EscapeUtils.unescapeHtml(blog.getBlogName()));
        loadSettingsForBlog();
    }

    @Override
    protected void onPause() {
        EditText usernameET = (EditText) findViewById(R.id.username);
        blog.setUsername(usernameET.getText().toString());
        EditText passwordET = (EditText) findViewById(R.id.password);
        blog.setPassword(passwordET.getText().toString());
        EditText httpuserET = (EditText) findViewById(R.id.httpuser);
        blog.setHttpuser(httpuserET.getText().toString());
        EditText httppasswordET = (EditText) findViewById(R.id.httppassword);
        blog.setHttppassword(httppasswordET.getText().toString());

        CheckBox fullSize = (CheckBox) findViewById(R.id.fullSizeImage);
        blog.setFullSizeImage(fullSize.isChecked());
        CheckBox scaledImage = (CheckBox) findViewById(R.id.scaledImage);
        blog.setScaledImage(scaledImage.isChecked());
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
        Spinner spinner = (Spinner) findViewById(R.id.maxImageWidth);
        blog.setMaxImageWidth(spinner.getSelectedItem().toString());

        long maxImageWidthId = spinner.getSelectedItemId();
        int maxImageWidthIdInt = (int) maxImageWidthId;

        blog.setMaxImageWidthId(maxImageWidthIdInt);

        CheckBox locationCB = (CheckBox) findViewById(R.id.location);
        blog.setLocation(locationCB.isChecked());

        blog.save(originalUsername);
        // exit settings screen
        Bundle bundle = new Bundle();

        bundle.putString("returnStatus", "SAVE");
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        loadSettingsForBlog();

    }

    private void loadSettingsForBlog() {
        // Set header labels to upper case
        ((TextView) findViewById(R.id.l_section1)).setText(getResources().getString(R.string.account_details).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_section2)).setText(getResources().getString(R.string.media).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_section3)).setText(getResources().getString(R.string.location).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_maxImageWidth)).setText(getResources().getString(R.string.max_thumbnail_px_width).toUpperCase(Locale.getDefault()));
        ((TextView) findViewById(R.id.l_httpuser)).setText(getResources().getString(R.string.http_credentials).toUpperCase(Locale.getDefault()));

        Spinner spinner = (Spinner) this.findViewById(R.id.maxImageWidth);
        ArrayAdapter<Object> spinnerArrayAdapter = new ArrayAdapter<Object>(this,
                R.layout.spinner_textview, new String[] {
                        "Original Size", "100", "200", "300", "400", "500", "600", "700", "800",
                        "900", "1000", "1100", "1200", "1300", "1400", "1500", "1600", "1700",
                        "1800", "1900", "2000"
                });
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        EditText usernameET = (EditText) findViewById(R.id.username);
        usernameET.setText(blog.getUsername());
        originalUsername = blog.getUsername();

        EditText passwordET = (EditText) findViewById(R.id.password);
        passwordET.setText(blog.getPassword());

        EditText httpUserET = (EditText) findViewById(R.id.httpuser);
        httpUserET.setText(blog.getHttpuser());

        EditText httpPasswordET = (EditText) findViewById(R.id.httppassword);
        httpPasswordET.setText(blog.getHttppassword());
        TextView httpUserLabel = (TextView) findViewById(R.id.l_httpuser);
        if (blog.isDotcomFlag()) {
            httpPasswordET.setVisibility(View.GONE);
            httpUserLabel.setVisibility(View.GONE);
            httpUserET.setVisibility(View.GONE);
        } else {
            httpPasswordET.setVisibility(View.VISIBLE);
            httpUserLabel.setVisibility(View.VISIBLE);
            httpUserET.setVisibility(View.VISIBLE);
        }

        CheckBox fullSize = (CheckBox) findViewById(R.id.fullSizeImage);
        fullSize.setChecked(blog.isFullSizeImage());
        CheckBox scaledImage = (CheckBox) findViewById(R.id.scaledImage);
        scaledImage.setChecked(blog.isScaledImage());
        EditText scaledImageWidth = (EditText) findViewById(R.id.scaledImageWidth);
        scaledImageWidth.setText("" + blog.getScaledImageWidth());
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

        CheckBox locationCB = (CheckBox) findViewById(R.id.location);
        if (hasLocationProvider) {
            locationCB.setChecked(blog.isLocation());
        } else {
            locationCB.setChecked(false);
            RelativeLayout locationLayout = (RelativeLayout) findViewById(R.id.section3);
            locationLayout.setVisibility(View.GONE);
        }

        spinner.setSelection(blog.getMaxImageWidthId());
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
