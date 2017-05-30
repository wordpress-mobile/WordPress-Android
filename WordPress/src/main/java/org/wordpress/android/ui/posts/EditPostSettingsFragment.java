package org.wordpress.android.ui.posts;

import android.app.Activity;
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
import android.widget.EditText;
import android.widget.Spinner;
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
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager;
import org.wordpress.android.ui.suggestion.util.SuggestionUtils;
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
import org.wordpress.android.widgets.SuggestionAutoCompleteText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class EditPostSettingsFragment extends Fragment
        implements View.OnClickListener, TextView.OnEditorActionListener {
    private static final String KEY_POST = "KEY_POST";
    private static final String POST_FORMAT_STANDARD_KEY = "standard";

    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final String CATEGORY_PREFIX_TAG = "category-";

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private PostModel mPost;
    private SiteModel mSite;

    private Spinner mStatusSpinner, mPostFormatSpinner;
    private EditText mPasswordEditText, mExcerptEditText;
    private TextView mPubDateText;
    private ViewGroup mSectionCategories;
    private NetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;
    private SuggestionAutoCompleteText mTagsEditText;

    private SuggestionServiceConnectionManager mSuggestionServiceConnectionManager;

    private long mFeaturedImageId;

    private List<TermModel> mCategories;

    private PostLocation mPostLocation;
    private LocationHelper mLocationHelper;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private String mCustomPubDate = "";
    private boolean mIsCustomPubDate;

    private ArrayList<String> mPostFormatKeys;
    private ArrayList<String> mPostFormatNames;

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
        if (mSuggestionServiceConnectionManager != null) {
            mSuggestionServiceConnectionManager.unbindFromService();
        }
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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

        mExcerptEditText = (EditText) rootView.findViewById(R.id.postExcerpt);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.post_password);
        mPubDateText = (TextView) rootView.findViewById(R.id.pubDate);
        mPubDateText.setOnClickListener(this);
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

        if (mPost.isPage()) { // remove post specific views
            mExcerptEditText.setVisibility(View.GONE);
            rootView.findViewById(R.id.sectionTags).setVisibility(View.GONE);
            rootView.findViewById(R.id.sectionCategories).setVisibility(View.GONE);
            rootView.findViewById(R.id.postFormatLabel).setVisibility(View.GONE);
            rootView.findViewById(R.id.postFormat).setVisibility(View.GONE);
        } else {
            // Default values
            mPostFormatKeys = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.post_format_keys)));
            mPostFormatNames = new ArrayList<>(
                    Arrays.asList(getResources().getStringArray(R.array.post_format_display_names)));
            // If we have specific values for this site, use them
            List<PostFormatModel> postFormatModels = mSiteStore.getPostFormats(mSite);
            for (PostFormatModel postFormatModel : postFormatModels) {
                if (!mPostFormatKeys.contains(postFormatModel.getSlug())) {
                    mPostFormatKeys.add(postFormatModel.getSlug());
                    mPostFormatNames.add(postFormatModel.getDisplayName());
                }
            }

            // Set up the Post Format spinner
            mPostFormatSpinner = (Spinner) rootView.findViewById(R.id.postFormat);
            ArrayAdapter<String> pfAdapter = new ArrayAdapter<>(getActivity(), R.layout.simple_spinner_item,
                    mPostFormatNames);
            pfAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mPostFormatSpinner.setAdapter(pfAdapter);
            String activePostFormat = POST_FORMAT_STANDARD_KEY;

            if (!TextUtils.isEmpty(mPost.getPostFormat())) {
                activePostFormat = mPost.getPostFormat();
            }

            for (int i = 0; i < mPostFormatKeys.size(); i++) {
                if (mPostFormatKeys.get(i).equals(activePostFormat))
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

            mTagsEditText = (SuggestionAutoCompleteText) rootView.findViewById(R.id.tags);
            if (mTagsEditText != null) {
                mTagsEditText.setTokenizer(new SuggestionAutoCompleteText.CommaTokenizer());

                setupSuggestionServiceAndAdapter();
            }
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

        long remoteBlogId = mSite.getSiteId();
        mSuggestionServiceConnectionManager = new SuggestionServiceConnectionManager(getActivity(), remoteBlogId);
        TagSuggestionAdapter tagSuggestionAdapter = SuggestionUtils.setupTagSuggestions(mSite, getActivity(),
                mSuggestionServiceConnectionManager);
        if (tagSuggestionAdapter != null) {
            mTagsEditText.setAdapter(tagSuggestionAdapter);
        }
    }

    private void initSettingsFields() {
        mExcerptEditText.setText(mPost.getExcerpt());

        String[] items = new String[]{getResources().getString(R.string.publish_post),
                getResources().getString(R.string.draft),
                getResources().getString(R.string.pending_review),
                getResources().getString(R.string.post_private)};

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

        updateStatusSpinner();
    }

    public void updateStatusSpinner() {
        switch (PostStatus.fromPost(mPost)) {
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

        String tags = TextUtils.join(",", mPost.getTagNameList());
        if (!tags.equals("") && mTagsEditText != null) {
            mTagsEditText.setText(tags);
        }

        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
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

    private PostStatus getPostStatusForSpinnerPosition(int position) {
        switch (position) {
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
        String excerpt = EditTextUtils.getText(mExcerptEditText);
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

        String tags = "", postFormat = "";
        if (!post.isPage()) {
            tags = EditTextUtils.getText(mTagsEditText);
            // since mTagsEditText is a `textMultiLine` field, we should replace "\n" with space
            tags = tags.replace("\n", " ");

            // post format
            if (mPostFormatKeys != null && mPostFormatSpinner != null &&
                mPostFormatSpinner.getSelectedItemPosition() < mPostFormatKeys.size()) {
                postFormat = mPostFormatKeys.get(mPostFormatSpinner.getSelectedItemPosition());
            }
        }

        String status;
        if (mStatusSpinner != null) {
            status = getPostStatusForSpinnerPosition(mStatusSpinner.getSelectedItemPosition()).toString();
        } else {
            status = post.getStatus();
        }

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

        post.setExcerpt(excerpt);
        post.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
        post.setStatus(status);
        post.setPassword(password);
        post.setPostFormat(postFormat);
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
