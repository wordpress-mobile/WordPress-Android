package org.wordpress.android.ui.posts;

import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.CardView;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

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
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity.MediaBrowserType;
import org.wordpress.android.ui.media.WordPressMediaUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;

public class EditPostSettingsFragment extends Fragment {
    private static final String KEY_POST = "KEY_POST";
    private static final String POST_FORMAT_STANDARD_KEY = "standard";

    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final int ACTIVITY_REQUEST_CODE_SELECT_TAGS = 6;
    private static final int ACTIVITY_REQUEST_CODE_PICK_LOCATION = 7;

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private PostModel mPost;
    private SiteModel mSite;

    private SiteSettingsInterface mSiteSettings;

    private TextView mExcerptTextView;
    private TextView mSlugTextView;
    private TextView mLocationTextView;
    private TextView mCategoriesTextView;
    private TextView mTagsTextView;
    private TextView mStatusTextView;
    private TextView mPostFormatTextView;
    private TextView mPasswordTextView;
    private TextView mPubDateText;
    private NetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;

    private long mFeaturedImageId;
    private String mCurrentSlug;
    private String mCurrentExcerpt;

    private List<TermModel> mCategories = new ArrayList<>();

    private PostLocation mPostLocation;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private String mCustomPubDate = "";
    private boolean mIsCustomPubDate;

    private ArrayList<String> mPostFormatKeys;
    private ArrayList<String> mPostFormatNames;

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
        updatePostFormatKeysAndNames();
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
        fetchSiteSettingsAndUpdatePostFormat();
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
        mLocationTextView = (TextView) rootView.findViewById(R.id.post_location);
        mCategoriesTextView = (TextView) rootView.findViewById(R.id.post_categories);
        mTagsTextView = (TextView) rootView.findViewById(R.id.post_tags);
        mStatusTextView = (TextView) rootView.findViewById(R.id.post_status);
        mPostFormatTextView = (TextView) rootView.findViewById(R.id.post_format);
        mPasswordTextView = (TextView) rootView.findViewById(R.id.post_password);
        mPubDateText = (TextView) rootView.findViewById(R.id.publish_date);

        mFeaturedImageView = (NetworkImageView) rootView.findViewById(R.id.post_featured_image);
        mFeaturedImageButton = (Button) rootView.findViewById(R.id.post_add_featured_image_button);
        CardView featuredImageCardView = (CardView) rootView.findViewById(R.id.post_featured_image_card_view);

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
            featuredImageCardView.setVisibility(View.GONE);
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

        final LinearLayout locationContainer = (LinearLayout) rootView.findViewById(R.id.post_location_container);
        locationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocationPickerOrPopupMenu(view);
            }
        });

        final LinearLayout categoriesContainer = (LinearLayout) rootView.findViewById(R.id.post_categories_container);
        categoriesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCategoriesActivity();
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

        final LinearLayout passwordContainer = (LinearLayout) rootView.findViewById(R.id.post_password_container);
        passwordContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostPasswordDialog();
            }
        });

        final LinearLayout publishDateContainer = (LinearLayout) rootView.findViewById(R.id.publish_date_container);
        publishDateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostDateSelectionDialog();
            }
        });

        if (mPost.isPage()) { // remove post specific views
            excerptContainer.setVisibility(View.GONE);
            rootView.findViewById(R.id.post_categories_container).setVisibility(View.GONE);
            rootView.findViewById(R.id.post_tags_container).setVisibility(View.GONE);
            formatContainer.setVisibility(View.GONE);
        }

        initSettingsFields();
        populateSelectedCategories();
        initLocation();
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
        mPostFormatTextView.setText(getPostFormatNameFromKey(mPost.getPostFormat()));
        mPasswordTextView.setText(mPost.getPassword());
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

        updateStatusTextView();
        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
            updateFeaturedImage(mPost.getFeaturedImageId());
        }
    }

    private void fetchSiteSettingsAndUpdatePostFormat() {
        // we need to fetch site settings in order to get the latest default post format
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mSite, new SiteSettingsListener() {
            @Override
            public void onSettingsUpdated(Exception error) {
                if (error == null && TextUtils.isEmpty(mPost.getPostFormat())) {
                    mPost.setPostFormat(mSiteSettings.getDefaultPostFormat());
                    if (mPostFormatKeys != null && mPostFormatNames != null && mPostFormatTextView != null) {
                        int idx = mPostFormatKeys.indexOf(mPost.getPostFormat());
                        if (idx != -1) {
                            mPostFormatTextView.setText(getPostFormatNameFromKey(mPost.getPostFormat()));
                        }
                    }
                }
            }

            @Override
            public void onSettingsSaved(Exception error) {
                // no-op
            }

            @Override
            public void onCredentialsValidated(Exception error) {
                // no-op
            }
        });
        if (mSiteSettings != null) {
            // init will fetch remote settings for us
            mSiteSettings.init(true);
        }
    }

    private void updateTagsTextView() {
        String tags = TextUtils.join(",", mPost.getTagNameList());
        // If `tags` is empty, the hint "Not Set" will be shown instead
        mTagsTextView.setText(tags);
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
                case ACTIVITY_REQUEST_CODE_PICK_LOCATION:
                    if (resultCode == RESULT_OK) {
                        Place place = PlacePicker.getPlace(getActivity(), data);
                        setLocation(place);
                    }
                    break;
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
                    if (resultCode == RESULT_OK && extras != null) {
                        String selectedTags = extras.getString(PostSettingsTagsActivity.KEY_SELECTED_TAGS);
                        if (selectedTags != null) {
                            String tags = selectedTags.replace("\n", " ");
                            mPost.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
                            updateTagsTextView();
                        }
                    }
                    break;
                case RequestCodes.SINGLE_SELECT_MEDIA_PICKER:
                    if (resultCode == RESULT_OK) {
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
     * retrieves and displays the friendly address for a lat/long location
     */
    private class FetchAndSetAddressAsyncTask extends AsyncTask<Double, Void, Address> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Address doInBackground(Double... args) {
            // args will be the latitude, longitude to look up
            double latitude = args[0];
            double longitude = args[1];
            try {
                return GeocoderUtils.getAddressFromCoords(getActivity(), latitude, longitude);
            } catch (IllegalArgumentException iae) {
                return null;
            }
        }

        protected void onPostExecute(@Nullable Address address) {
            if (address == null) {
                return;
            }
            if (address.getMaxAddressLineIndex() > 0) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; ; ++i) {
                    sb.append(address.getAddressLine(i));
                    if (i == address.getMaxAddressLineIndex()) {
                        sb.append(".");
                        break;
                    } else {
                        sb.append(", ");
                    }
                }
                mLocationTextView.setText(sb.toString());
            }
            // Else, do nothing (keep the "lat, long" format).
        }
    }

    private void showPostExcerptDialog() {
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mCurrentExcerpt, getString(R.string.post_settings_excerpt), getString(R.string.post_settings_excerpt_dialog_hint), false);
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
                mCurrentSlug, getString(R.string.post_settings_slug), getString(R.string.post_settings_slug_dialog_hint), true);
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

    private void showLocationPicker() {
        if (!isAdded()) {
            return;
        }
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        // Pre-pick the previous selected location if any
        LatLng latLng = null;
        if (mPostLocation != null) {
            latLng = new LatLng(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        } else if (mPost.hasLocation()) {
            PostLocation location = mPost.getLocation();
            latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        if (latLng != null) {
            builder.setLatLngBounds(new LatLngBounds(latLng, latLng));
        }
        // Show the picker
        try {
            startActivityForResult(builder.build(getActivity()), ACTIVITY_REQUEST_CODE_PICK_LOCATION);
        } catch (GooglePlayServicesNotAvailableException nae) {
            ToastUtils.showToast(getActivity(), R.string.post_settings_error_placepicker_missing_play_services);
        } catch (GooglePlayServicesRepairableException re) {
            GooglePlayServicesUtil.getErrorDialog(re.getConnectionStatusCode(), getActivity(), 0);
        }
    }

    private void showCategoriesActivity() {
        Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
        categoriesIntent.putExtra(WordPress.SITE, mSite);

        // Make sure the PostModel is up to date with current category selections
        updatePostSettings(mPost);
        categoriesIntent.putExtra(SelectCategoriesActivity.KEY_POST, mPost);

        startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.post_settings_status);
        int checkedItem = getCurrentPostStatusIndex();
        // Current index should never be -1, but if if is, we don't want to crash
        if (checkedItem == -1) {
            checkedItem = 0;
        }
        builder.setSingleChoiceItems(R.array.post_settings_statuses, checkedItem, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ListView listView = ((AlertDialog)dialog).getListView();
                String newStatus = (String) listView.getAdapter().getItem(listView.getCheckedItemPosition());
                mStatusTextView.setText(newStatus);
                updatePostSettingsAndSaveButton();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showPostFormatDialog() {
        int checkedItem = 0;
        if (!TextUtils.isEmpty(mPost.getPostFormat())) {
            for (int i = 0; i < mPostFormatKeys.size(); i++) {
                if (mPost.getPostFormat().equals(mPostFormatKeys.get(i))) {
                    checkedItem = i;
                    break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.post_settings_post_format);
        builder.setSingleChoiceItems(mPostFormatNames.toArray(new CharSequence[0]), checkedItem, null);
        builder.setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ListView listView = ((AlertDialog)dialog).getListView();
                String formatName = (String) listView.getAdapter().getItem(listView.getCheckedItemPosition());
                mPostFormatTextView.setText(formatName);
                mPost.setPostFormat(getPostFormatKeyFromName(formatName));
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showPostPasswordDialog() {
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mPost.getPassword(), getString(R.string.password),
                getString(R.string.post_settings_password_dialog_hint), false);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        mPost.setPassword(input);
                        mPasswordTextView.setText(mPost.getPassword());
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void updatePostFormatKeysAndNames() {
        // Default values
        mPostFormatKeys = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.post_format_keys)));
        mPostFormatNames = new ArrayList<>(Arrays.asList(getResources()
                .getStringArray(R.array.post_format_display_names)));

        // If we have specific values for this site, use them
        List<PostFormatModel> postFormatModels = mSiteStore.getPostFormats(mSite);
        for (PostFormatModel postFormatModel : postFormatModels) {
            if (!mPostFormatKeys.contains(postFormatModel.getSlug())) {
                mPostFormatKeys.add(postFormatModel.getSlug());
                mPostFormatNames.add(postFormatModel.getDisplayName());
            }
        }
    }

    private String getPostFormatKeyFromName(String postFormatName) {
        for (int i = 0; i < mPostFormatNames.size(); i++) {
            if (postFormatName.equalsIgnoreCase(mPostFormatNames.get(i))) {
                return mPostFormatKeys.get(i);
            }
        }
        return POST_FORMAT_STANDARD_KEY;
    }

    private String getPostFormatNameFromKey(String postFormatKey) {
        if (TextUtils.isEmpty(postFormatKey)) {
            postFormatKey = POST_FORMAT_STANDARD_KEY;
        }

        for (int i = 0; i < mPostFormatKeys.size(); i++) {
            if (postFormatKey.equalsIgnoreCase(mPostFormatKeys.get(i))) {
                return mPostFormatNames.get(i);
            }
        }
        // Since this is only used as a display name, if we can't find the key, we should just
        // return the capitalized key as the name which should be better than returning `null`
        return StringUtils.capitalize(postFormatKey);
    }

    private void populateSelectedCategories() {
        StringBuilder sb = new StringBuilder();
        Iterator<TermModel> it = mCategories.iterator();
        if (it.hasNext()) {
            sb.append(it.next().getName());
            while (it.hasNext()) {
                sb.append(", ");
                sb.append(it.next().getName());
            }
        }
        // If `sb` is empty, the hint "Not Set" will be shown instead
        mCategoriesTextView.setText(sb);
    }

    private void setLocation(@Nullable Place place) {
        if (mPostLocation == null) {
            mPostLocation = new PostLocation();
        }
        if (place == null) {
            mLocationTextView.setText(R.string.post_settings_not_set);
            mPost.clearLocation();
            mPostLocation = null;
            return;
        }
        mPostLocation.setLatitude(place.getLatLng().latitude);
        mPostLocation.setLongitude(place.getLatLng().longitude);
        mPost.setLocation(mPostLocation);
        mLocationTextView.setText(place.getAddress());
    }

    private void initLocation() {
        if (!mPost.hasLocation()) {
            mPostLocation = null;
            mLocationTextView.setText(getString(R.string.post_settings_not_set));
        } else {
            mPostLocation = mPost.getLocation();
            mLocationTextView.setText(mPost.getLocation().getLatitude() + ", " + mPost.getLocation().getLongitude());
            // Asynchronously get the address from the location coordinates
            new FetchAndSetAddressAsyncTask().execute(mPost.getLocation().getLatitude(), mPost.getLocation().getLongitude());
        }
    }

    private void showLocationPickerOrPopupMenu(@NonNull final View view) {
        // If the post doesn't have location set, show the picker directly
        if (!mPost.hasLocation()) {
            showLocationPicker();
            return;
        }
        if (!isAdded()) {
            return;
        }

        // If the post have a location set, show a context menu to change or remove the location
        PopupMenu popupMenu = new PopupMenu(getActivity(), view);
        popupMenu.inflate(R.menu.post_settings_location_popup);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.menu_change_location) {
                    showLocationPicker();
                } else if (menuItem.getItemId() == R.id.menu_remove_location) {
                    setLocation(null);
                }
                return true;
            }
        });

        // Using android internal MenuPopupHelper class trick to show the icons
        try {
            Field fieldPopup = popupMenu.getClass().getDeclaredField("mPopup");
            fieldPopup.setAccessible(true);
            Object menuPopupHelper = fieldPopup.get(popupMenu);
            MenuPopupHelper popupHelper = (MenuPopupHelper) fieldPopup.get(popupMenu);
            Class<?> classPopupHelper = Class.forName(popupHelper.getClass().getName());
            Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            // no op, icons won't show
        }
        popupMenu.show();
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

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostFormatsChanged(SiteStore.OnPostFormatsChanged event) {
        if (event.isError()) {
            AppLog.e(T.POSTS, "An error occurred while updating the post formats with type: " + event.error.type);
            return;
        }
        updatePostFormatKeysAndNames();
    }
}
