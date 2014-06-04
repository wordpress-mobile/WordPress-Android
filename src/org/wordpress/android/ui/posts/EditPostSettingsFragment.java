package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.LocationHelper;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;
import org.xmlrpc.android.ApiHelper;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

public class EditPostSettingsFragment extends Fragment implements View.OnClickListener {
    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;

    private static final String CATEGORY_PREFIX_TAG = "category-";

    private EditPostActivity mActivity;

    private Spinner mStatusSpinner, mPostFormatSpinner;
    private EditText mPasswordEditText, mTagsEditText, mExcerptEditText;
    private TextView mLocationText, mPubDateText;
    private ViewGroup mSectionCategories;

    private ArrayList<String> mCategories;

    private PostLocation mPostLocation;
    private LocationHelper mLocationHelper;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private long mCustomPubDate = 0;
    private boolean mIsCustomPubDate;

    private String[] mPostFormats;
    private String[] mPostFormatTitles;

    private static enum LocationStatus {NONE, FOUND, NOT_FOUND, SEARCHING}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mCategories = new ArrayList<String>();

        ViewGroup rootView = (ViewGroup) inflater
                .inflate(R.layout.fragment_edit_post_settings, container, false);

        if (rootView == null)
            return null;

        mActivity = (EditPostActivity) getActivity();

        mExcerptEditText = (EditText) rootView.findViewById(R.id.postExcerpt);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.post_password);
        Button mPubDateButton = (Button) rootView.findViewById(R.id.pubDateButton);
        mPubDateText = (TextView) rootView.findViewById(R.id.pubDate);
        mStatusSpinner = (Spinner) rootView.findViewById(R.id.status);
        mStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePostSettingsAndSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mTagsEditText = (EditText) rootView.findViewById(R.id.tags);
        mSectionCategories = ((ViewGroup) rootView.findViewById(R.id.sectionCategories));

        mPubDateButton.setOnClickListener(this);

        // Set header labels to upper case
        ((TextView) rootView.findViewById(R.id.categoryLabel)).setText(getResources().getString(R.string.categories).toUpperCase());
        ((TextView) rootView.findViewById(R.id.statusLabel)).setText(getResources().getString(R.string.status).toUpperCase());
        ((TextView) rootView.findViewById(R.id.postFormatLabel)).setText(getResources().getString(R.string.post_format).toUpperCase());
        ((TextView) rootView.findViewById(R.id.pubDateLabel)).setText(getResources().getString(R.string.publish_date).toUpperCase());

        if (mActivity.getPost().isPage()) { // remove post specific views
            mExcerptEditText.setVisibility(View.GONE);
            (rootView.findViewById(R.id.sectionTags)).setVisibility(View.GONE);
            (rootView.findViewById(R.id.sectionCategories)).setVisibility(View.GONE);
            (rootView.findViewById(R.id.postFormatLabel)).setVisibility(View.GONE);
            (rootView.findViewById(R.id.postFormat)).setVisibility(View.GONE);
        } else {
            mPostFormatTitles = getResources().getStringArray(R.array.post_formats_array);
            mPostFormats =
                    new String[]{"aside", "audio", "chat", "gallery", "image", "link", "quote", "standard", "status",
                                 "video"};
            if (WordPress.getCurrentBlog().getPostFormats().equals("")) {
                List<Object> args = new Vector<Object>();
                args.add(WordPress.getCurrentBlog());
                args.add(mActivity);
                new ApiHelper.GetPostFormatsTask().execute(args);
            } else {
                try {
                    Gson gson = new Gson();
                    Type type = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> jsonPostFormats = gson.fromJson(WordPress.getCurrentBlog().getPostFormats(),
                            type);
                    mPostFormats = new String[jsonPostFormats.size()];
                    mPostFormatTitles = new String[jsonPostFormats.size()];
                    int i = 0;
                    for (Map.Entry<String, String> entry : jsonPostFormats.entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue();
                        mPostFormats[i] = key;
                        mPostFormatTitles[i] = StringEscapeUtils.unescapeHtml(val);
                        i++;
                    }
                } catch (RuntimeException e) {
                    AppLog.e(T.POSTS, e);
                }
            }
            mPostFormatSpinner = (Spinner) rootView.findViewById(R.id.postFormat);
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, mPostFormatTitles);
            pfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPostFormatSpinner.setAdapter(pfAdapter);
            String activePostFormat = "standard";


            if (!TextUtils.isEmpty(mActivity.getPost().getPostFormat())) {
                activePostFormat = mActivity.getPost().getPostFormat();
            }

            for (int i = 0; i < mPostFormats.length; i++) {
                if (mPostFormats[i].equals(activePostFormat))
                    mPostFormatSpinner.setSelection(i);
            }

            mPostFormatSpinner.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    }
            );
        }

        Post post = mActivity.getPost();
        if (post != null) {
            mExcerptEditText.setText(post.getPostExcerpt());

            String[] items = new String[]{getResources().getString(R.string.publish_post), getResources().getString(R.string.draft),
                    getResources().getString(R.string.pending_review), getResources().getString(R.string.post_private)};

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mStatusSpinner.setAdapter(adapter);
            mStatusSpinner.setOnTouchListener(
                    new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            return false;
                        }
                    }
            );

            if (post.isUploaded()) {
                items = new String[]{
                        getResources().getString(R.string.publish_post),
                        getResources().getString(R.string.draft),
                        getResources().getString(R.string.pending_review),
                        getResources().getString(R.string.post_private)
                };
                adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, items);
                mStatusSpinner.setAdapter(adapter);
            }

            long pubDate = post.getDate_created_gmt();
            if (pubDate != 0) {
                try {
                    int flags = 0;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                    flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                    flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                    String formattedDate = DateUtils.formatDateTime(getActivity(), pubDate,
                            flags);
                    mPubDateText.setText(formattedDate);
                } catch (RuntimeException e) {
                    AppLog.e(T.POSTS, e);
                }
            }

            if (!TextUtils.isEmpty(post.getPassword()))
                mPasswordEditText.setText(post.getPassword());

            switch (post.getStatusEnum()) {
                case PUBLISHED:
                case SCHEDULED:
                case UNKNOWN:
                    mStatusSpinner.setSelection(0, true);
                    break;
                case DRAFT:
                    mStatusSpinner.setSelection(1, true);
                    break;
                case PENDING:
                    mStatusSpinner.setSelection(2, true);
                    break;
                case PRIVATE:
                    mStatusSpinner.setSelection(3, true);
                    break;
            }

            if (!post.isPage()) {
                if (post.getJSONCategories() != null) {
                    mCategories = JSONUtil.fromJSONArrayToStringList(post.getJSONCategories());
                }

                // if this post has location attached to it, look up the location address
                if (post.hasLocation()) {
                    PostLocation location = post.getLocation();

                    setLocationStatus(LocationStatus.SEARCHING);
                    new GetAddressTask().execute(location.getLatitude(), location.getLongitude());
                }
            }
            String tags = post.getKeywords();
            if (!tags.equals("")) {
                mTagsEditText.setText(tags);
            }

            populateSelectedCategories();
        }

        initLocation(rootView);

        return rootView;
    }



    private String getPostStatusForSpinnerPosition(int position) {
        switch (position) {
            case 0:
                return PostStatus.toString(PostStatus.PUBLISHED);
            case 1:
                return PostStatus.toString(PostStatus.DRAFT);
            case 2:
                return PostStatus.toString(PostStatus.PENDING);
            case 3:
                return PostStatus.toString(PostStatus.PRIVATE);
            default:
                return PostStatus.toString(PostStatus.UNKNOWN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_PHOTO || requestCode == MediaUtils.RequestCode.ACTIVITY_REQUEST_CODE_TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES:
                    extras = data.getExtras();
                    if (extras != null && extras.containsKey("selectedCategories")) {
                        mCategories = (ArrayList<String>) extras.getSerializable("selectedCategories");
                        populateSelectedCategories();
                    }
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pubDateButton) {
            showPostDateSelectionDialog();
        } else if (id == R.id.selectCategories) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", WordPress.getCurrentBlog().getLocalTableBlogId());
            if (mCategories.size() > 0) {
                bundle.putSerializable("categories", new HashSet<String>(mCategories));
            }
            Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
            categoriesIntent.putExtras(bundle);
            startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
        } else if (id == R.id.categoryButton) {
            onCategoryButtonClick(v);
        } else if (id == R.id.viewMap) {
            viewLocation();
        } else if (id == R.id.updateLocation) {
            getLocation();
        } else if (id == R.id.removeLocation) {
            removeLocation();
        }
    }

    private void showPostDateSelectionDialog() {
        final DatePicker datePicker = new DatePicker(getActivity());
        datePicker.init(mYear, mMonth, mDay, null);
        datePicker.setCalendarViewShown(false);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_date)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mYear = datePicker.getYear();
                        mMonth = datePicker.getMonth();
                        mDay = datePicker.getDayOfMonth();
                        showPostTimeSelectionDialog();
                    }
                })
                .setNeutralButton(getResources().getText(R.string.immediately),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface,
                                                int i) {
                                mIsCustomPubDate = true;
                                mPubDateText.setText(R.string.immediately);
                                updatePostSettingsAndSaveButton();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        }
                ).setView(datePicker).show();

    }

    private void showPostTimeSelectionDialog() {
        final TimePicker timePicker = new TimePicker(getActivity());
        timePicker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
        timePicker.setCurrentHour(mHour);
        timePicker.setCurrentMinute(mMinute);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.select_time)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mHour = timePicker.getCurrentHour();
                        mMinute = timePicker.getCurrentMinute();

                        Date d = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute);
                        long timestamp = d.getTime();

                        try {
                            int flags = 0;
                            flags |= DateUtils.FORMAT_SHOW_DATE;
                            flags |= DateUtils.FORMAT_ABBREV_MONTH;
                            flags |= DateUtils.FORMAT_SHOW_YEAR;
                            flags |= DateUtils.FORMAT_SHOW_TIME;
                            String formattedDate = DateUtils.formatDateTime(getActivity(), timestamp, flags);
                            mCustomPubDate = timestamp;
                            mPubDateText.setText(formattedDate);
                            mIsCustomPubDate = true;

                            updatePostSettingsAndSaveButton();
                            AnalyticsTracker.track(AnalyticsTracker.Stat.EDITOR_SCHEDULED_POST);
                        } catch (RuntimeException e) {
                            AppLog.e(T.POSTS, e);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                            }
                        }).setView(timePicker).show();
    }

    /**
     * Updates post object with content of this fragment
     */
    public void updatePostSettings() {
        Post post = mActivity.getPost();
        if (post == null)
            return;

        String password = (mPasswordEditText.getText() != null) ? mPasswordEditText.getText().toString() : "";
        String pubDate = (mPubDateText.getText() != null) ? mPubDateText.getText().toString() : "";
        String excerpt = (mExcerptEditText.getText() != null) ? mExcerptEditText.getText().toString() : "";

        long pubDateTimestamp = 0;
        if (mIsCustomPubDate && pubDate.equals(getResources().getText(R.string.immediately)) && !post.isLocalDraft()) {
            Date d = new Date();
            pubDateTimestamp = d.getTime();
        } else if (!pubDate.equals(getResources().getText(R.string.immediately))) {
            if (mIsCustomPubDate)
                pubDateTimestamp = mCustomPubDate;
            else if (post.getDate_created_gmt() > 0)
                pubDateTimestamp = post.getDate_created_gmt();
        } else if (pubDate.equals(getResources().getText(R.string.immediately)) && post.isLocalDraft()) {
            post.setDate_created_gmt(0);
            post.setDateCreated(0);
        }

        String tags = "", postFormat = "";
        if (!post.isPage()) {
            tags = (mTagsEditText.getText() != null) ? mTagsEditText.getText().toString() : "";

            // post format
            if (mPostFormats != null && mPostFormatSpinner.getSelectedItemPosition() < mPostFormats.length) {
                postFormat = mPostFormats[mPostFormatSpinner.getSelectedItemPosition()];
            }
        }

        String status = getPostStatusForSpinnerPosition(mStatusSpinner.getSelectedItemPosition());

        // We want to flag this post as having changed statuses from draft to published so that we
        // properly track stats we care about for when users first publish posts.
        if (post.isUploaded() && post.getPostStatus().equals(PostStatus.toString(PostStatus.DRAFT))
                && status.equals(PostStatus.toString(PostStatus.PUBLISHED))) {
            post.setChangedFromLocalDraftToPublished(true);
        }

        if (WordPress.getCurrentBlog().isLocation() && mActivity.getPost().supportsLocation()) {
            if (mPostLocation == null && post.hasLocation()) {
                mPostLocation = post.getLocation();
            }
            post.setLocation(mPostLocation);
        }

        post.setPostExcerpt(excerpt);
        post.setDate_created_gmt(pubDateTimestamp);
        post.setJSONCategories(new JSONArray(mCategories));
        post.setKeywords(tags);
        post.setPostStatus(status);
        post.setPassword(password);
        post.setPostFormat(postFormat);
    }

    /*
     * Saves settings to post object and updates save button text in the ActionBar
     */
    private void updatePostSettingsAndSaveButton() {
        if (mActivity != null) {
            updatePostSettings();
            mActivity.invalidateOptionsMenu();
        }
    }

    /**
     * Location methods
     */

    /*
     * retrieves and displays the friendly address for a lat/long location
     */
    private class GetAddressTask extends AsyncTask<Double, Void, String> {
        double latitude;
        double longitude;

        @Override
        protected String doInBackground(Double... args) {
            // args will be the latitude, longitude to look up
            latitude = args[0];
            longitude = args[1];

            // first make sure a Geocoder service exists on this device (requires API 9)
            if (!Geocoder.isPresent())
                return null;

            Geocoder gcd;
            try {
                gcd = new Geocoder(getActivity(), Locale.getDefault());
            } catch (NullPointerException cannotIstantiateEx) {
                AppLog.e(T.EDITOR, "Cannot Istantiate Geocoder", cannotIstantiateEx);
                return null;
            }

            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(latitude, longitude, 1);

                // addresses may be null or empty if network isn't connected
                if (addresses == null || addresses.size() == 0)
                    return null;

                String locality = "", adminArea = "", country = "";
                if (addresses.get(0).getLocality() != null)
                    locality = addresses.get(0).getLocality();
                if (addresses.get(0).getAdminArea() != null)
                    adminArea = addresses.get(0).getAdminArea();
                if (addresses.get(0).getCountryName() != null)
                    country = addresses.get(0).getCountryName();

                return ((locality.equals("")) ? locality : locality + ", ")
                        + ((adminArea.equals("")) ? adminArea : adminArea + " ") + country;
            } catch (IOException e) {
                // may get "Unable to parse response from server" IOException here if Geocoder
                // service is hit too frequently
                AppLog.e(T.EDITOR, "Unable to parse response from server. Is Geocoder service hitting the server too frequently?", e);
                return null;
            }
        }

        protected void onPostExecute(String result) {
            setLocationStatus(LocationStatus.FOUND);
            if (result == null || result.isEmpty()) {
                // show lat/long when Geocoder fails (ugly, but better than not showing anything
                // or showing an error since the location has been assigned to the post already)
                mLocationText.setText(Double.toString(latitude) + ", " + Double.toString(longitude));
            } else {
                mLocationText.setText(result);
            }
        }
    }

    private LocationHelper.LocationResult locationResult = new LocationHelper.LocationResult() {
        @Override
        public void gotLocation(final Location location) {
            if (getActivity() == null)
                return;
            // note that location will be null when requesting location fails
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    setLocation(location);
                }
            });
        }
    };

    /*
     * called when activity is created to initialize the location provider, show views related
     * to location if enabled for this blog, and retrieve the current location if necessary
     */
    private void initLocation(ViewGroup rootView) {

        boolean hasLocationProvider = false;
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Activity.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        if (providers != null) {
            for (String providerName : providers) {
                if (providerName.equals(LocationManager.GPS_PROVIDER) || providerName.equals(LocationManager.NETWORK_PROVIDER)) {
                    hasLocationProvider = true;
                }
            }
        }

        // show the location views if a provider was found and this is a post on a blog that has location enabled
        if (hasLocationProvider && WordPress.getCurrentBlog().isLocation() && mActivity.getPost().supportsLocation()) {
            View locationRootView = ((ViewStub) rootView.findViewById(R.id.stub_post_location_settings)).inflate();

            ((TextView) locationRootView.findViewById(R.id.locationLabel)).setText(getResources().getString(R.string.location).toUpperCase());

            mLocationText = (TextView) locationRootView.findViewById(R.id.locationText);

            Button viewMap = (Button) locationRootView.findViewById(R.id.viewMap);
            Button updateLocation = (Button) locationRootView.findViewById(R.id.updateLocation);
            Button removeLocation = (Button) locationRootView.findViewById(R.id.removeLocation);
            updateLocation.setOnClickListener(this);
            removeLocation.setOnClickListener(this);
            viewMap.setOnClickListener(this);

            // if the post doesn't have a location, get the user's current location
            if (!mActivity.getPost().hasLocation()) {
                getLocation();
            }
        }
    }

    /*
     * get the current location
     */
    private void getLocation() {
        if (mLocationHelper == null)
            mLocationHelper = new LocationHelper();
        boolean canGetLocation = mLocationHelper.getLocation(getActivity(), locationResult);
        if (canGetLocation) {
            setLocationStatus(LocationStatus.SEARCHING);
            mLocationText.setText(getString(R.string.loading));
        } else {
            setLocation(null);
        }
    }

    /*
     * called when location is retrieved/updated for this post - looks up the address to
     * display for the lat/long
     */
    private void setLocation(Location location) {
        if (location != null) {
            mPostLocation = new PostLocation(location.getLatitude(), location.getLongitude());
            new GetAddressTask().execute(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        } else {
            mLocationText.setText(getString(R.string.location_not_found));
            setLocationStatus(LocationStatus.NOT_FOUND);
        }
    }

    private void removeLocation() {
        mPostLocation = null;
        mActivity.getPost().unsetLocation();

        mLocationText.setText("");
        setLocationStatus(LocationStatus.NONE);
    }

    private void viewLocation() {
        if (mPostLocation != null && mPostLocation.isValid()) {
            String uri = "geo:" + mPostLocation.getLatitude() + "," + mPostLocation.getLongitude();
            startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
        } else {
            Toast.makeText(getActivity(), getResources().getText(R.string.location_toast), Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * changes the left drawable on the location text to match the passed status
     */
    private void setLocationStatus(LocationStatus status) {
        // animate location text when searching
        if (status == LocationStatus.SEARCHING) {
            Animation aniBlink = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
            if (aniBlink != null)
                mLocationText.startAnimation(aniBlink);
        } else {
            mLocationText.clearAnimation();
        }

        final int drawableId;
        switch (status) {
            case FOUND:
                drawableId = R.drawable.ic_action_location_found;
                break;
            case NOT_FOUND:
                drawableId = R.drawable.ic_action_location_off;
                break;
            case SEARCHING:
                drawableId = R.drawable.ic_action_location_searching;
                break;
            case NONE:
                drawableId = 0;
                break;
            default:
                return;
        }

        mLocationText.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0);
    }

    /**
     * Categories
     */

    private void onCategoryButtonClick(View v) {
        // Get category name by removing prefix from the tag
        boolean listChanged = false;
        String categoryName = (String) v.getTag();
        categoryName = categoryName.replaceFirst(CATEGORY_PREFIX_TAG, "");

        // Remove clicked category from list
        for (int i = 0; i < mCategories.size(); i++) {
            if (mCategories.get(i).equals(categoryName)) {
                mCategories.remove(i);
                listChanged = true;
                break;
            }
        }

        // Recreate category views
        if (listChanged) {
            populateSelectedCategories();
        }
    }

    private void populateSelectedCategories() {
        // Remove previous category buttons if any + select category button
        List<View> viewsToRemove = new ArrayList<View>();
        for (int i = 0; i < mSectionCategories.getChildCount(); i++) {
            View v = mSectionCategories.getChildAt(i);
            if (v == null)
                return;
            Object tag = v.getTag();
            if (tag != null && tag.getClass() == String.class &&
                    (((String) tag).startsWith(CATEGORY_PREFIX_TAG) || tag.equals("select-category"))) {
                viewsToRemove.add(v);
            }
        }
        for (View viewToRemove : viewsToRemove) {
            mSectionCategories.removeView(viewToRemove);
        }
        viewsToRemove.clear();

        // New category buttons
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        for (String categoryName : mCategories) {
            Button buttonCategory = (Button) layoutInflater.inflate(R.layout.category_button, null);
            if (categoryName != null && buttonCategory != null) {
                buttonCategory.setText(Html.fromHtml(categoryName));
                buttonCategory.setTag(CATEGORY_PREFIX_TAG + categoryName);
                buttonCategory.setOnClickListener(this);
                mSectionCategories.addView(buttonCategory);
            }
        }

        // Add select category button
        Button selectCategory = (Button) layoutInflater.inflate(R.layout.category_select_button, null);
        if (selectCategory != null) {
            selectCategory.setOnClickListener(this);
            mSectionCategories.addView(selectCategory);
        }
    }
}
