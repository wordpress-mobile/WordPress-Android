package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.AppCompatButton;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostLocation;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaGalleryPickerActivity;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.LocationHelper;
import org.wordpress.android.widgets.SuggestionAutoCompleteText;
import org.xmlrpc.android.ApiHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class EditPostSettingsFragment extends Fragment
        implements View.OnClickListener, TextView.OnEditorActionListener {
    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final String CATEGORY_PREFIX_TAG = "category-";

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private Post mPost;

    private Spinner mStatusSpinner, mPostFormatSpinner;
    private EditText mPasswordEditText, mExcerptEditText;
    private TextView mPubDateText;
    private ViewGroup mSectionCategories;
    private ViewGroup mRootView;
    private TextView mFeaturedImageLabel;
    private NetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;
    private SuggestionAutoCompleteText mTagsEditText;

    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private long mFeaturedImageId;

    private ArrayList<String> mCategories;

    private PostLocation mPostLocation;
    private LocationHelper mLocationHelper;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private long mCustomPubDate = 0;
    private boolean mIsCustomPubDate;

    private String[] mPostFormats;
    private String[] mPostFormatTitles;

    private enum LocationStatus {NONE, FOUND, NOT_FOUND, SEARCHING}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            PreferenceManager.setDefaultValues(getActivity(), R.xml.account_settings, false);
        }
    }

    @Override
    public void onDestroy() {
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mPost = ((EditPostActivity) getActivity()).getPost();
        mRootView = (ViewGroup) inflater.inflate(R.layout.edit_post_settings_fragment, container, false);

        if (mRootView == null || mPost == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);
        mCategories = new ArrayList<String>();

        mExcerptEditText = (EditText) mRootView.findViewById(R.id.postExcerpt);
        mPasswordEditText = (EditText) mRootView.findViewById(R.id.post_password);
        mPubDateText = (TextView) mRootView.findViewById(R.id.pubDate);
        mPubDateText.setOnClickListener(this);
        mStatusSpinner = (Spinner) mRootView.findViewById(R.id.status);
        mStatusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePostSettingsAndSaveButton();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSectionCategories = ((ViewGroup) mRootView.findViewById(R.id.sectionCategories));

        mFeaturedImageLabel = (TextView) mRootView.findViewById(R.id.featuredImageLabel);
        mFeaturedImageView = (NetworkImageView) mRootView.findViewById(R.id.featuredImage);
        mFeaturedImageButton = (Button) mRootView.findViewById(R.id.addFeaturedImage);

        if (AppPrefs.isVisualEditorEnabled()) {
            registerForContextMenu(mFeaturedImageView);
            mFeaturedImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.showContextMenu();
                }
            });

            mFeaturedImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchMediaGalleryActivity();
                }
            });
        } else {
            mFeaturedImageLabel.setVisibility(View.GONE);
            mFeaturedImageView.setVisibility(View.GONE);
            mFeaturedImageButton.setVisibility(View.GONE);
        }

        if (mPost.isPage()) { // remove post specific views
            mExcerptEditText.setVisibility(View.GONE);
            mRootView.findViewById(R.id.sectionTags).setVisibility(View.GONE);
            mRootView.findViewById(R.id.sectionCategories).setVisibility(View.GONE);
            mRootView.findViewById(R.id.postFormatLabel).setVisibility(View.GONE);
            mRootView.findViewById(R.id.postFormat).setVisibility(View.GONE);
        } else {
            mPostFormatTitles = getResources().getStringArray(R.array.post_formats_array);
            mPostFormats =
                    new String[]{"aside", "audio", "chat", "gallery", "image", "link", "quote", "standard", "status",
                                 "video"};
            if (WordPress.getCurrentBlog().getPostFormats().equals("")) {
                new ApiHelper.GetPostFormatsTask().execute(WordPress.getCurrentBlog());
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
            mPostFormatSpinner = (Spinner) mRootView.findViewById(R.id.postFormat);
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item,
                    mPostFormatTitles);
            pfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPostFormatSpinner.setAdapter(pfAdapter);
            String activePostFormat = "standard";

            if (!TextUtils.isEmpty(mPost.getPostFormat())) {
                activePostFormat = mPost.getPostFormat();
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

            mTagsEditText = (SuggestionAutoCompleteText) mRootView.findViewById(R.id.tags);
            if (mTagsEditText != null) {
                mTagsEditText.setTokenizer(new SuggestionAutoCompleteText.CommaTokenizer());

                setupSuggestionServiceAndAdapter();
            }
        }

        initSettingsFields();
        populateSelectedCategories();
        initLocation(mRootView);
        return mRootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(0, SELECT_LIBRARY_MENU_POSITION, 0, getResources().getText(R.string.select_from_media_library));
        menu.add(0, CLEAR_FEATURED_IMAGE_MENU_POSITION, 0, "Remove featured image");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SELECT_LIBRARY_MENU_POSITION:
                launchMediaGalleryActivity();
                return true;
            case CLEAR_FEATURED_IMAGE_MENU_POSITION:
                mFeaturedImageId = -1;
                mFeaturedImageView.setVisibility(View.GONE);
                mFeaturedImageButton.setVisibility(View.VISIBLE);
                return true;
            default:
                return false;
        }
    }

    private void setupSuggestionServiceAndAdapter() {
        if (!isAdded()) return;

        int remoteBlogId = -1;
        String blogID = WordPress.getCurrentRemoteBlogId();
        remoteBlogId = StringUtils.stringToInt(blogID, -1);

        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), remoteBlogId);
        TagSuggestionAdapter tagSuggestionAdapter = SuggestionUtils.setupTagSuggestions(remoteBlogId, getActivity(), mSuggestionServiceConnectionManager);
        if (tagSuggestionAdapter != null) {
            mTagsEditText.setAdapter(tagSuggestionAdapter);
        }
    }

    private void initSettingsFields() {
        mExcerptEditText.setText(mPost.getPostExcerpt());

        String[] items = new String[]{ getResources().getString(R.string.publish_post),
                                       getResources().getString(R.string.draft),
                                       getResources().getString(R.string.pending_review),
                                       getResources().getString(R.string.post_private) };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, items);
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

        long pubDate = mPost.getDate_created_gmt();
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

        if (!TextUtils.isEmpty(mPost.getPassword())) {
            mPasswordEditText.setText(mPost.getPassword());
        }

        switch (mPost.getStatusEnum()) {
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

        if (!mPost.isPage()) {
            if (mPost.getJSONCategories() != null) {
                mCategories = JSONUtils.fromJSONArrayToStringList(mPost.getJSONCategories());
            }
        }
        String tags = mPost.getKeywords();
        if (!tags.equals("")) {
            mTagsEditText.setText(tags);
        }

        if (AppPrefs.isVisualEditorEnabled()) {
            updateFeaturedImage(mPost.getFeaturedImageId());
        }
    }

    public long getFeaturedImageId() {
        return mFeaturedImageId;
    }

    public void updateFeaturedImage(long id) {
        if (mFeaturedImageId != id) {
            mFeaturedImageId = id;
            if (mFeaturedImageId > 0) {
                int blogId = WordPress.getCurrentBlog().getLocalTableBlogId();
                Cursor cursor = WordPress.wpDB.getMediaFile(String.valueOf(blogId), String.valueOf(mFeaturedImageId));
                if (cursor != null && cursor.moveToFirst()) {
                    mFeaturedImageView.setVisibility(View.VISIBLE);
                    mFeaturedImageButton.setVisibility(View.GONE);

                    // Get max width for photon thumbnail
                    int maxWidth = getResources().getDisplayMetrics().widthPixels;
                    int padding = DisplayUtils.dpToPx(getActivity(), 16);
                    int imageWidth = (maxWidth - padding);

                    String thumbUrl = WordPressMediaUtils.getNetworkThumbnailUrl(cursor, imageWidth);
                    WordPressMediaUtils.loadNetworkImage(thumbUrl, mFeaturedImageView);
                }

                if (cursor != null) {
                    cursor.close();
                }
            } else {
                mFeaturedImageView.setVisibility(View.GONE);
                mFeaturedImageButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void launchMediaGalleryActivity() {
        Intent intent = new Intent(getActivity(), MediaGalleryPickerActivity.class);
        intent.putExtra(MediaGalleryPickerActivity.PARAM_SELECT_ONE_ITEM, true);
        startActivityForResult(intent, MediaGalleryPickerActivity.REQUEST_CODE);
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

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES:
                    extras = data.getExtras();
                    if (extras != null && extras.containsKey("selectedCategories")) {
                        mCategories = (ArrayList<String>) extras.getSerializable("selectedCategories");
                        populateSelectedCategories();
                    }
                    break;
                case MediaGalleryPickerActivity.REQUEST_CODE:
                    if (resultCode == Activity.RESULT_OK) {
                        ArrayList<String> ids = data.getStringArrayListExtra(MediaGalleryPickerActivity.RESULT_IDS);
                        if (ids == null || ids.size() == 0) {
                            return;
                        }

                        updateFeaturedImage(Long.parseLong(ids.get(0)));
                    }
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pubDate) {
            showPostDateSelectionDialog();
        } else if (id == R.id.selectCategories) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", WordPress.getCurrentBlog().getLocalTableBlogId());
            if (mCategories != null && mCategories.size() > 0) {
                bundle.putSerializable("categories", new HashSet<String>(mCategories));
            }
            Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
            categoriesIntent.putExtras(bundle);
            startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
        } else if (id == R.id.categoryButton) {
            onCategoryButtonClick(v);
        } else if (id == R.id.locationText) {
            viewLocation();
        } else if (id == R.id.updateLocation) {
            showLocationSearch();
        } else if (id == R.id.removeLocation) {
            removeLocation();
            showLocationAdd();
        } else if (id == R.id.addLocation) {
            // Init Location settings when we switch to the fragment, that could trigger the opening of
            // a dialog asking the user to enable the Geolocation permission (starting Android 6.+).
            if (checkForLocationPermission()) {
                showLocationSearch();
            }
        } else if (id == R.id.searchLocation) {
            if (checkForLocationPermission()) {
                searchLocation();
            }
        }
    }

    @Override
    public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        boolean handled = false;
        int id = view.getId();
        if (id == R.id.searchLocationText && actionId == EditorInfo.IME_ACTION_SEARCH && checkForLocationPermission()) {
            searchLocation();
            handled = true;
        }
        return handled;
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
        if (!isAdded() || mPost == null) {
            return;
        }

        String password = EditTextUtils.getText(mPasswordEditText);
        String pubDate = EditTextUtils.getText(mPubDateText);
        String excerpt = EditTextUtils.getText(mExcerptEditText);

        long pubDateTimestamp = 0;
        if (mIsCustomPubDate && pubDate.equals(getText(R.string.immediately)) && !mPost.isLocalDraft()) {
            Date d = new Date();
            pubDateTimestamp = d.getTime();
        } else if (!pubDate.equals(getText(R.string.immediately))) {
            if (mIsCustomPubDate)
                pubDateTimestamp = mCustomPubDate;
            else if (mPost.getDate_created_gmt() > 0)
                pubDateTimestamp = mPost.getDate_created_gmt();
        } else if (pubDate.equals(getText(R.string.immediately)) && mPost.isLocalDraft()) {
            mPost.setDate_created_gmt(0);
            mPost.setDateCreated(0);
        }

        String tags = "", postFormat = "";
        if (!mPost.isPage()) {
            tags = EditTextUtils.getText(mTagsEditText);

            // post format
            if (mPostFormats != null && mPostFormatSpinner != null &&
                mPostFormatSpinner.getSelectedItemPosition() < mPostFormats.length) {
                postFormat = mPostFormats[mPostFormatSpinner.getSelectedItemPosition()];
            }
        }

        String status;
        if (mStatusSpinner != null) {
            status = getPostStatusForSpinnerPosition(mStatusSpinner.getSelectedItemPosition());
        } else {
            status = mPost.getPostStatus();
        }

        // We want to flag this post as having changed statuses from draft to published so that we
        // properly track stats we care about for when users first publish posts.
        if (!mPost.isLocalDraft() && mPost.getPostStatus().equals(PostStatus.toString(PostStatus.DRAFT))
                && status.equals(PostStatus.toString(PostStatus.PUBLISHED))) {
            mPost.setChangedFromDraftToPublished(true);
        }

        if (mPost.supportsLocation()) {
            mPost.setLocation(mPostLocation);
        }

        if (mCategories != null) {
            mPost.setJSONCategories(new JSONArray(mCategories));
        }

        if (AppPrefs.isVisualEditorEnabled()) {
            mPost.setFeaturedImageId(mFeaturedImageId);
        }

        mPost.setPostExcerpt(excerpt);
        mPost.setDate_created_gmt(pubDateTimestamp);
        mPost.setKeywords(tags);
        mPost.setPostStatus(status);
        mPost.setPassword(password);
        mPost.setPostFormat(postFormat);
    }

    /*
     * Saves settings to post object and updates save button text in the ActionBar
     */
    private void updatePostSettingsAndSaveButton() {
        if (isAdded()) {
            updatePostSettings();
            getActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Location methods
     */

    /*
     * retrieves and displays the friendly address for a lat/long location
     */
    private class GetAddressTask extends AsyncTask<Double, Void, Address> {
        double latitude;
        double longitude;

        @Override
        protected void onPreExecute() {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        }

        @Override
        protected Address doInBackground(Double... args) {
            // args will be the latitude, longitude to look up
            latitude = args[0];
            longitude = args[1];

            return GeocoderUtils.getAddressFromCoords(getActivity(), latitude, longitude);
        }

        protected void onPostExecute(Address address) {
            setLocationStatus(LocationStatus.FOUND);
            if (address == null) {
                // show lat/long when Geocoder fails (ugly, but better than not showing anything
                // or showing an error since the location has been assigned to the post already)
                updateLocationText(Double.toString(latitude) + ", " + Double.toString(longitude));
            } else {
                String locationName = GeocoderUtils.getLocationNameFromAddress(address);
                updateLocationText(locationName);
            }
        }
    }

    private class GetCoordsTask extends AsyncTask<String, Void, Address> {
        @Override
        protected void onPreExecute() {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        }

        @Override
        protected Address doInBackground(String... args) {
            String locationName = args[0];

            return GeocoderUtils.getAddressFromLocationName(getActivity(), locationName);
        }

        @Override
        protected void onPostExecute(Address address) {
            setLocationStatus(LocationStatus.FOUND);
            showLocationView();

            if (address != null) {
                double[] coordinates = GeocoderUtils.getCoordsFromAddress(address);
                setLocation(coordinates[0], coordinates[1]);

                String locationName = GeocoderUtils.getLocationNameFromAddress(address);
                updateLocationText(locationName);
            } else {
                showLocationNotAvailableError();
                showLocationSearch();
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

    private View mLocationAddSection;
    private View mLocationSearchSection;
    private View mLocationViewSection;
    private TextView mLocationText;
    private EditText mLocationEditText;
    private Button mButtonSearchLocation;

    private TextWatcher mLocationEditTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            String buttonText;
            if (s.length() > 0) {
                buttonText = getResources().getString(R.string.search_location);
            } else {
                buttonText = getResources().getString(R.string.search_current_location);
            }
            mButtonSearchLocation.setText(buttonText);
        }
    };

    /*
     * called when activity is created to initialize the location provider, show views related
     * to location if enabled for this blog, and retrieve the current location if necessary
     */
    private void initLocation(ViewGroup rootView) {
        if (!mPost.supportsLocation()) return;

        // show the location views if a provider was found and this is a post on a blog that has location enabled
        View locationRootView = ((ViewStub) rootView.findViewById(R.id.stub_post_location_settings)).inflate();

        TextView locationLabel = ((TextView) locationRootView.findViewById(R.id.locationLabel));
        locationLabel.setText(getResources().getString(R.string.location).toUpperCase());

        mLocationText = (TextView) locationRootView.findViewById(R.id.locationText);
        mLocationText.setOnClickListener(this);

        mLocationAddSection = locationRootView.findViewById(R.id.sectionLocationAdd);
        mLocationSearchSection = locationRootView.findViewById(R.id.sectionLocationSearch);
        mLocationViewSection = locationRootView.findViewById(R.id.sectionLocationView);

        Button addLocation = (Button) locationRootView.findViewById(R.id.addLocation);
        addLocation.setOnClickListener(this);

        mButtonSearchLocation = (Button) locationRootView.findViewById(R.id.searchLocation);
        mButtonSearchLocation.setOnClickListener(this);

        mLocationEditText = (EditText) locationRootView.findViewById(R.id.searchLocationText);
        mLocationEditText.setOnEditorActionListener(this);
        mLocationEditText.addTextChangedListener(mLocationEditTextWatcher);

        Button updateLocation = (Button) locationRootView.findViewById(R.id.updateLocation);
        Button removeLocation = (Button) locationRootView.findViewById(R.id.removeLocation);
        updateLocation.setOnClickListener(this);
        removeLocation.setOnClickListener(this);

        // this is where we ask for location permission when EditPostActivity is oppened
        if (SiteSettingsInterface.getGeotagging(getActivity()) && !checkForLocationPermission()) return;

        // if this post has location attached to it, look up the location address
        if (mPost.hasLocation()) {
            showLocationView();
            PostLocation location = mPost.getLocation();
            setLocation(location.getLatitude(), location.getLongitude());
        } else {
            // Search for current location to geotag post if preferences allow
            EditPostActivity activity = (EditPostActivity) getActivity();
            if (SiteSettingsInterface.getGeotagging(activity) && activity.isNewPost()) {
                searchLocation();
            } else {
                showLocationAdd();
            }
        }
    }

    private boolean checkForLocationPermission() {
        return isAdded() && PermissionUtils.checkLocationPermissions(getActivity(),
                EditPostActivity.LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void showLocationSearch() {
        mLocationAddSection.setVisibility(View.GONE);
        mLocationSearchSection.setVisibility(View.VISIBLE);
        mLocationViewSection.setVisibility(View.GONE);

        EditTextUtils.showSoftInput(mLocationEditText);
    }

    private void showLocationAdd() {
        mLocationAddSection.setVisibility(View.VISIBLE);
        mLocationSearchSection.setVisibility(View.GONE);
        mLocationViewSection.setVisibility(View.GONE);
    }

    private void showLocationView() {
        mLocationAddSection.setVisibility(View.GONE);
        mLocationSearchSection.setVisibility(View.GONE);
        mLocationViewSection.setVisibility(View.VISIBLE);
    }

    public void searchLocation() {
        if(!isAdded() || mLocationEditText == null) return;

        EditTextUtils.hideSoftInput(mLocationEditText);
        String location = EditTextUtils.getText(mLocationEditText);

        removeLocation();

        if (location.isEmpty()) {
            fetchCurrentLocation();
        } else {
            new GetCoordsTask().execute(location);
        }
    }

    /*
     * get the current location
     */
    private void fetchCurrentLocation() {
        if (!isAdded()) {
            return;
        }
        if (mLocationHelper == null) {
            mLocationHelper = new LocationHelper();
        }
        boolean canGetLocation = mLocationHelper.getLocation(getActivity(), locationResult);

        if (canGetLocation) {
            setLocationStatus(LocationStatus.SEARCHING);
            showLocationView();
        } else {
            setLocation(null);
            showLocationNotAvailableError();
            showLocationAdd();
        }
    }

    /*
     * called when location is retrieved/updated for this post - looks up the address to
     * display for the lat/long
     */
    private void setLocation(Location location) {
        if (location != null) {
            setLocation(location.getLatitude(), location.getLongitude());
        } else {
            updateLocationText(getString(R.string.location_not_found));
            setLocationStatus(LocationStatus.NOT_FOUND);
        }
    }

    private void setLocation(double latitude, double longitude) {
        mPostLocation = new PostLocation(latitude, longitude);
        new GetAddressTask().execute(mPostLocation.getLatitude(), mPostLocation.getLongitude());
    }

    private void removeLocation() {
        mPostLocation = null;
        mPost.unsetLocation();

        updateLocationText("");
        setLocationStatus(LocationStatus.NONE);
    }

    private void viewLocation() {
        if (mPostLocation != null && mPostLocation.isValid()) {
            String uri = "geo:" + mPostLocation.getLatitude() + "," + mPostLocation.getLongitude();
            startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));
        } else {
            showLocationNotAvailableError();
            showLocationAdd();
        }
    }

    private void showLocationNotAvailableError() {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(getActivity(), getResources().getText(R.string.location_not_found), Toast.LENGTH_SHORT).show();
    }

    private void updateLocationText(String locationName) {
        mLocationText.setText(locationName);
    }

    /*
     * changes the left drawable on the location text to match the passed status
     */
    private void setLocationStatus(LocationStatus status) {
        if (!isAdded()) {
            return;
        }

        // animate location text when searching
        if (status == LocationStatus.SEARCHING) {
            updateLocationText(getString(R.string.loading));

            Animation aniBlink = AnimationUtils.loadAnimation(getActivity(), R.anim.blink);
            if (aniBlink != null) {
                mLocationText.startAnimation(aniBlink);
            }
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
        if (mCategories == null) {
            ToastUtils.showToast(getActivity(), R.string.error_generic);
            return;
        }

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

        if (mCategories != null) {
            for (String categoryName : mCategories) {
                AppCompatButton buttonCategory = (AppCompatButton) layoutInflater.inflate(R.layout.category_button, null);
                if (categoryName != null && buttonCategory != null) {
                    buttonCategory.setText(Html.fromHtml(categoryName));
                    buttonCategory.setTag(CATEGORY_PREFIX_TAG + categoryName);
                    buttonCategory.setOnClickListener(this);
                    mSectionCategories.addView(buttonCategory);
                }
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
