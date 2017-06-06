package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.fluxc.tools.FluxCImageLoader;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity.MediaBrowserType;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.LocationHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class EditPostSettingsFragment extends Fragment
        implements View.OnClickListener, TextView.OnEditorActionListener {
    private static final String KEY_POST = "KEY_POST";

    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final String CATEGORY_PREFIX_TAG = "category-";
    private static final int ACTIVITY_REQUEST_CODE_SELECT_TAGS = 6;

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private PostModel mPost;
    private SiteModel mSite;

    private EditText mPasswordEditText;
    private TextView mExcerptTextView;
    private TextView mSlugTextView;
    private TextView mTagsTextView;
    private TextView mStatusTextView;
    private TextView mPostFormatTextView;
    private TextView mPubDateText;
    private ViewGroup mSectionCategories;
    private NetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;

    private long mFeaturedImageId;
    private String mCurrentSlug;
    private String mCurrentExcerpt;

    private List<TermModel> mCategories;

    private PostLocation mPostLocation;
    private LocationHelper mLocationHelper;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private String mCustomPubDate = "";
    private boolean mIsCustomPubDate;

    private enum LocationStatus {NONE, FOUND, NOT_FOUND, SEARCHING}

    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;
    @Inject FluxCImageLoader mImageLoader;

    public static EditPostSettingsFragment newInstance(SiteModel site, PostModel post) {
        EditPostSettingsFragment fragment = new EditPostSettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        bundle.putSerializable(KEY_POST, post);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        mDispatcher.register(this);

        if (getActivity() != null) {
            PreferenceManager.setDefaultValues(getActivity(), R.xml.account_settings, false);
        }
        updateSiteOrFinishActivity(savedInstanceState);
    }

    private void updateSiteOrFinishActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
                mPost = (PostModel) getArguments().getSerializable(KEY_POST);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
                mPost = (PostModel) getActivity().getIntent().getSerializableExtra(KEY_POST);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mPost = (PostModel) savedInstanceState.getSerializable(KEY_POST);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }

        // Update post formats and categories, in case anything changed.
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(mSite));
        if (!mPost.isPage()) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(mSite));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(KEY_POST, mPost);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.edit_post_settings_fragment, container, false);

        if (rootView == null || mPost == null) {
            return null;
        }

        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);
        mHour = c.get(Calendar.HOUR_OF_DAY);
        mMinute = c.get(Calendar.MINUTE);

        mExcerptTextView = (TextView) rootView.findViewById(R.id.post_excerpt);
        mSlugTextView = (TextView) rootView.findViewById(R.id.post_slug);
        mTagsTextView = (TextView) rootView.findViewById(R.id.post_tags);
        mStatusTextView = (TextView) rootView.findViewById(R.id.post_status);
        mPostFormatTextView = (TextView) rootView.findViewById(R.id.post_format);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.post_password);
        mPubDateText = (TextView) rootView.findViewById(R.id.pubDate);
        mPubDateText.setOnClickListener(this);
        mSectionCategories = ((ViewGroup) rootView.findViewById(R.id.sectionCategories));

        TextView featuredImageLabel = (TextView) rootView.findViewById(R.id.featuredImageLabel);
        mFeaturedImageView = (NetworkImageView) rootView.findViewById(R.id.featuredImage);
        mFeaturedImageButton = (Button) rootView.findViewById(R.id.addFeaturedImage);

        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
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
                    launchFeaturedMediaPicker();
                }
            });
        } else {
            featuredImageLabel.setVisibility(View.GONE);
            mFeaturedImageView.setVisibility(View.GONE);
            mFeaturedImageButton.setVisibility(View.GONE);
        }

        final LinearLayout excerptContainer = (LinearLayout) rootView.findViewById(R.id.post_excerpt_container);
        excerptContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostExcerptDialog();
            }
        });

        final LinearLayout slugContainer = (LinearLayout) rootView.findViewById(R.id.post_slug_container);
        slugContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSlugDialog();
            }
        });

        final LinearLayout tagsContainer = (LinearLayout) rootView.findViewById(R.id.post_tags_container);
        tagsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTagsActivity();
            }
        });

        final LinearLayout statusContainer = (LinearLayout) rootView.findViewById(R.id.post_status_container);
        statusContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStatusDialog();
            }
        });

        final LinearLayout formatContainer = (LinearLayout) rootView.findViewById(R.id.post_format_container);
        formatContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostFormatDialog();
            }
        });

        if (mPost.isPage()) { // remove post specific views
            excerptContainer.setVisibility(View.GONE);
            rootView.findViewById(R.id.sectionTags).setVisibility(View.GONE);
            rootView.findViewById(R.id.sectionCategories).setVisibility(View.GONE);
            formatContainer.setVisibility(View.GONE);
        }

        initSettingsFields();
        populateSelectedCategories();
        initLocation(rootView);
        return rootView;
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
                launchFeaturedMediaPicker();
                return true;
            case CLEAR_FEATURED_IMAGE_MENU_POSITION:
                mFeaturedImageId = 0;
                mFeaturedImageView.setVisibility(View.GONE);
                mFeaturedImageButton.setVisibility(View.VISIBLE);
                return true;
            default:
                return false;
        }
    }

    private void initSettingsFields() {
        mCurrentExcerpt = mPost.getExcerpt();
        mCurrentSlug = mPost.getSlug();
        mExcerptTextView.setText(mCurrentExcerpt);
        mSlugTextView.setText(mCurrentSlug);
        updateTagsTextView();

        String pubDate = mPost.getDateCreated();
        if (StringUtils.isNotEmpty(pubDate)) {
            try {
                int flags = 0;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
                flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
                flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
                String formattedDate = DateUtils.formatDateTime(getActivity(),
                        DateTimeUtils.timestampFromIso8601Millis(pubDate), flags);
                mPubDateText.setText(formattedDate);
            } catch (RuntimeException e) {
                AppLog.e(T.POSTS, e);
            }
        }

        if (!TextUtils.isEmpty(mPost.getPassword())) {
            mPasswordEditText.setText(mPost.getPassword());
        }

        updateStatusTextView();
        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
            updateFeaturedImage(mPost.getFeaturedImageId());
        }
    }

    private void updateTagsTextView() {
        String tags = TextUtils.join(",", mPost.getTagNameList());
        if (!TextUtils.isEmpty(tags)) {
            mTagsTextView.setText(tags);
        } else {
            mTagsTextView.setText(R.string.not_set);
        }
    }

    public void updateStatusTextView() {
        String[] statuses = getResources().getStringArray(R.array.post_settings_statuses);
        switch (PostStatus.fromPost(mPost)) {
            case PUBLISHED:
            case SCHEDULED:
            case UNKNOWN:
                mStatusTextView.setText(statuses[0]);
                break;
            case DRAFT:
                mStatusTextView.setText(statuses[1]);
                break;
            case PENDING:
                mStatusTextView.setText(statuses[2]);
                break;
            case PRIVATE:
                mStatusTextView.setText(statuses[3]);
                break;
        }
    }

    private PostStatus getCurrentPostStatus() {
        int index = getCurrentPostStatusIndex();
        switch (index) {
            case 0:
                return PostStatus.PUBLISHED;
            case 1:
                return PostStatus.DRAFT;
            case 2:
                return PostStatus.PENDING;
            case 3:
                return PostStatus.PRIVATE;
            default:
                return PostStatus.UNKNOWN;
        }
    }

    private int getCurrentPostStatusIndex() {
        String[] statuses = getResources().getStringArray(R.array.post_settings_statuses);
        String currentStatus = mStatusTextView.getText().toString();
        for (int i = 0; i < statuses.length; i++) {
            if (currentStatus.equalsIgnoreCase(statuses[i])) {
                return i;
            }
        }
        return -1;
    }

    public long getFeaturedImageId() {
        return mFeaturedImageId;
    }

    public void updateFeaturedImage(long id) {
        if (mFeaturedImageId != id) {
            mFeaturedImageId = id;
            if (mFeaturedImageId > 0) {
                MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mFeaturedImageId);

                if (media == null || !isAdded()) {
                    return;
                }

                mFeaturedImageView.setVisibility(View.VISIBLE);
                mFeaturedImageButton.setVisibility(View.GONE);

                // Get max width for photon thumbnail
                int width = DisplayUtils.getDisplayPixelWidth(getActivity());
                int height = DisplayUtils.getDisplayPixelHeight(getActivity());
                int size = Math.max(width, height);

                String mediaUri = media.getThumbnailUrl();
                if (SiteUtils.isPhotonCapable(mSite)) {
                    mediaUri = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0);
                }

                WordPressMediaUtils.loadNetworkImage(mediaUri, mFeaturedImageView, mImageLoader);
            } else {
                mFeaturedImageView.setVisibility(View.GONE);
                mFeaturedImageButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void launchFeaturedMediaPicker() {
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, mSite);
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.SINGLE_SELECT_PICKER);
        intent.putExtra(MediaBrowserActivity.ARG_IMAGES_ONLY, true);
        startActivityForResult(intent, RequestCodes.SINGLE_SELECT_MEDIA_PICKER);
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
                        @SuppressWarnings("unchecked")
                        List<TermModel> categoryList = (List<TermModel>) extras.getSerializable("selectedCategories");
                        mCategories = categoryList;
                        populateSelectedCategories();
                    }
                    break;
                case ACTIVITY_REQUEST_CODE_SELECT_TAGS:
                    extras = data.getExtras();
                    if (resultCode == Activity.RESULT_OK && extras != null) {
                        String selectedTags = extras.getString(PostSettingsTagsActivity.KEY_SELECTED_TAGS);
                        if (selectedTags != null) {
                            String tags = selectedTags.replace("\n", " ");
                            mPost.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
                            updateTagsTextView();
                        }
                    }
                    break;
                case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                    if (resultCode == Activity.RESULT_OK) {
                        ArrayList<Long> ids = ListUtils.
                                fromLongArray(data.getLongArrayExtra(MediaBrowserActivity.RESULT_IDS));
                        if (ids == null || ids.size() == 0) {
                            return;
                        }

                        updateFeaturedImage(ids.get(0));
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
            Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
            categoriesIntent.putExtra(WordPress.SITE, mSite);

            // Make sure the PostModel is up to date with current category selections
            updatePostSettings(mPost);
            categoriesIntent.putExtra(SelectCategoriesActivity.KEY_POST, mPost);

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
        final DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), null, mYear, mMonth, mDay);
        datePickerDialog.setTitle(R.string.select_date);
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getText(android.R.string.ok),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mYear = datePickerDialog.getDatePicker().getYear();
                mMonth = datePickerDialog.getDatePicker().getMonth();
                mDay = datePickerDialog.getDatePicker().getDayOfMonth();
                showPostTimeSelectionDialog();
            }
        });
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getText(R.string.immediately),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mIsCustomPubDate = true;
                mPubDateText.setText(R.string.immediately);
                updatePostSettingsAndSaveButton();
            }
        });
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getText(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        datePickerDialog.show();
    }

    private void showPostTimeSelectionDialog() {
        final TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                mHour = selectedHour;
                mMinute = selectedMinute;

                Date javaDate = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute);
                long javaTimestamp = javaDate.getTime();

                try {
                    int flags = 0;
                    flags |= DateUtils.FORMAT_SHOW_DATE;
                    flags |= DateUtils.FORMAT_ABBREV_MONTH;
                    flags |= DateUtils.FORMAT_SHOW_YEAR;
                    flags |= DateUtils.FORMAT_SHOW_TIME;
                    String formattedDate = DateUtils.formatDateTime(getActivity(), javaTimestamp, flags);
                    mCustomPubDate = DateTimeUtils.iso8601FromDate(javaDate);
                    mPubDateText.setText(formattedDate);
                    mIsCustomPubDate = true;

                    updatePostSettingsAndSaveButton();
                } catch (RuntimeException e) {
                    AppLog.e(T.POSTS, e);
                }
            }
        }, mHour, mMinute, DateFormat.is24HourFormat(getActivity()));
        timePickerDialog.setTitle(R.string.select_time);
        timePickerDialog.show();
    }

    /**
     * Updates given post object with current status of settings fields
     */
    public void updatePostSettings(PostModel post) {
        if (!isAdded() || post == null) {
            return;
        }

        String password = EditTextUtils.getText(mPasswordEditText);
        boolean publishImmediately = EditTextUtils.getText(mPubDateText).equals(getText(R.string.immediately));

        String publicationDateIso8601 = "";
        if (mIsCustomPubDate && publishImmediately && !post.isLocalDraft()) {
            publicationDateIso8601 = DateTimeUtils.iso8601FromDate(new Date());
        } else if (!publishImmediately) {
            if (mIsCustomPubDate) {
                publicationDateIso8601 = mCustomPubDate;
            } else if (StringUtils.isNotEmpty(post.getDateCreated())) {
                publicationDateIso8601 = post.getDateCreated();
            }
        }

        post.setDateCreated(publicationDateIso8601);

        if (post.supportsLocation()) {
            if (mPostLocation == null) {
                post.clearLocation();
            } else {
                post.setLocation(mPostLocation);
            }
        }

        if (mCategories != null) {
            List<Long> categoryIds = new ArrayList<>();
            for (TermModel category : mCategories) {
                categoryIds.add(category.getRemoteTermId());
            }
            post.setCategoryIdList(categoryIds);
        }

        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
            post.setFeaturedImageId(mFeaturedImageId);
        }

        post.setExcerpt(mCurrentExcerpt);
        post.setSlug(mCurrentSlug);
        post.setStatus(getCurrentPostStatus().toString());
        post.setPassword(password);
    }

    /*
     * Saves settings to post object and updates save button text in the ActionBar
     */
    private void updatePostSettingsAndSaveButton() {
        if (isAdded()) {
            updatePostSettings(mPost);
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

        // if this post has location attached to it, look up the location address
        if (mPost.hasLocation()) {
            showLocationView();
            PostLocation location = mPost.getLocation();
            setLocation(location.getLatitude(), location.getLongitude());
        } else {
            showLocationAdd();
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
        if (!isAdded() || mLocationEditText == null) return;

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
        mPost.clearLocation();

        updateLocationText("");
        setLocationStatus(LocationStatus.NONE);
    }

    private void viewLocation() {
        if (mPostLocation != null && mPostLocation.isValid()) {
            String locationString = "geo:" + mPostLocation.getLatitude() + "," + mPostLocation.getLongitude();
            ActivityLauncher.openUrlExternal(getActivity(), locationString);
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

    private void showPostExcerptDialog() {
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mCurrentExcerpt, getString(R.string.post_excerpt), getString(R.string.post_excerpt_dialog_hint), false);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        mCurrentExcerpt = input;
                        mExcerptTextView.setText(mCurrentExcerpt);
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void showSlugDialog() {
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mCurrentSlug, getString(R.string.post_slug), getString(R.string.post_slug_dialog_hint), true);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        mCurrentSlug = input;
                        mSlugTextView.setText(mCurrentSlug);
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void showTagsActivity() {
        // Fetch/refresh the tags in preparation for the the PostSettingsTagsActivity
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(mSite));

        Intent tagsIntent = new Intent(getActivity(), PostSettingsTagsActivity.class);
        tagsIntent.putExtra(WordPress.SITE, mSite);
        String tags = TextUtils.join(",", mPost.getTagNameList());
        tagsIntent.putExtra(PostSettingsTagsActivity.KEY_TAGS, tags);
        startActivityForResult(tagsIntent, ACTIVITY_REQUEST_CODE_SELECT_TAGS);
    }

    private void showStatusDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.post_settings_status);
        int checkedItem = getCurrentPostStatusIndex();
        // Current index should never be -1, but if if is, we don't want to crash
        if (checkedItem == -1) {
            checkedItem = 0;
        }
        builder.setSingleChoiceItems(R.array.post_settings_statuses, checkedItem, null);
        builder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ListView lw = ((AlertDialog)dialog).getListView();
                String newStatus = (String) lw.getAdapter().getItem(lw.getCheckedItemPosition());
                mStatusTextView.setText(newStatus);
                updatePostSettingsAndSaveButton();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showPostFormatDialog() {
        // Default values
        ArrayList<String> postFormatKeys = new ArrayList<>(Arrays.asList(getResources()
                .getStringArray(R.array.post_format_keys)));
        ArrayList<String> postFormatNames = new ArrayList<>(
                Arrays.asList(getResources().getStringArray(R.array.post_format_display_names)));

        // If we have specific values for this site, use them
        List<PostFormatModel> postFormatModels = mSiteStore.getPostFormats(mSite);
        for (PostFormatModel postFormatModel : postFormatModels) {
            if (!postFormatKeys.contains(postFormatModel.getSlug())) {
                postFormatKeys.add(postFormatModel.getSlug());
                postFormatNames.add(postFormatModel.getDisplayName());
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.post_settings_format);
        builder.setSingleChoiceItems(postFormatNames.toArray(new CharSequence[0]), 0, null);
        builder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
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
                drawableId = R.drawable.ic_location_found_black_translucent_40_32dp;
                break;
            case NOT_FOUND:
                drawableId = R.drawable.ic_location_off_black_translucent_40_32dp;
                break;
            case SEARCHING:
                drawableId = R.drawable.ic_location_searching_black_40_32dp;
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
            if (mCategories.get(i).getName().equals(categoryName)) {
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
        List<View> viewsToRemove = new ArrayList<>();
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
            for (TermModel category : mCategories) {
                AppCompatButton buttonCategory = (AppCompatButton) layoutInflater.inflate(R.layout.category_button,
                        null);
                if (category != null && category.getName() != null && buttonCategory != null) {
                    buttonCategory.setText(Html.fromHtml(category.getName()));
                    buttonCategory.setTag(CATEGORY_PREFIX_TAG + category.getName());
                    buttonCategory.setOnClickListener(this);
                    mSectionCategories.addView(buttonCategory);
                }
            }
        }

        // Add select category button once the category list has been initialized
        Button selectCategory = (Button) layoutInflater.inflate(R.layout.category_select_button, null);
        if (selectCategory != null && mCategories != null) {
            selectCategory.setOnClickListener(this);
            mSectionCategories.addView(selectCategory);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        switch (event.causeOfChange) {
            case FETCH_CATEGORIES:
                mCategories = mTaxonomyStore.getCategoriesForPost(mPost, mSite);
                populateSelectedCategories();
                break;
        }
    }
}
