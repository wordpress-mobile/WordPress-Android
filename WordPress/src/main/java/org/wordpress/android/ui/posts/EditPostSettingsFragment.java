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
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.text.Editable;
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
import org.wordpress.android.fluxc.generated.PostActionBuilder;
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
import org.wordpress.android.fluxc.store.PostStore;
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
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID;
import static org.wordpress.android.ui.posts.SelectCategoriesActivity.KEY_SELECTED_CATEGORIES;

public class EditPostSettingsFragment extends Fragment
        implements View.OnClickListener, TextView.OnEditorActionListener {
    private static final String POST_FORMAT_STANDARD_KEY = "standard";

    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final int ACTIVITY_REQUEST_CODE_SELECT_TAGS = 6;

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private PostModel mPost;
    private SiteModel mSite;

    private SiteSettingsInterface mSiteSettings;

    private TextView mExcerptTextView;
    private TextView mSlugTextView;
    private TextView mCategoriesTextView;
    private TextView mTagsTextView;
    private TextView mStatusTextView;
    private TextView mPostFormatTextView;
    private TextView mPasswordTextView;
    private TextView mPublishDateTextView;
    private NetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;

    private LocationHelper mLocationHelper;

    private int mYear, mMonth, mDay, mHour, mMinute;
    private String mCustomPublishDate = "";
    private boolean mIsCustomPublishDate;

    private ArrayList<String> mPostFormatKeys;
    private ArrayList<String> mPostFormatNames;

    private enum LocationStatus {NONE, FOUND, NOT_FOUND, SEARCHING}

    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject MediaStore mMediaStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;
    @Inject FluxCImageLoader mImageLoader;

    public static EditPostSettingsFragment newInstance(SiteModel site, int localPostId) {
        EditPostSettingsFragment fragment = new EditPostSettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
        bundle.putSerializable(EXTRA_POST_LOCAL_ID, localPostId);
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
        updateSiteAndFetchPostOrFinishActivity(savedInstanceState);
        updatePostFormatKeysAndNames();
        fetchSiteSettingsAndUpdateDefaultPostFormat();

        // Update post formats and categories, in case anything changed.
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(mSite));
        if (!mPost.isPage()) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(mSite));
        }
    }

    private void updateSiteAndFetchPostOrFinishActivity(Bundle savedInstanceState) {
        int localPostId;
        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
                localPostId = getArguments().getInt(EXTRA_POST_LOCAL_ID);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
                localPostId = getActivity().getIntent().getIntExtra(EXTRA_POST_LOCAL_ID, 0);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            localPostId = savedInstanceState.getInt(EXTRA_POST_LOCAL_ID);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }

        if (localPostId != 0) {
            mPost = mPostStore.getPostByLocalPostId(localPostId);
        }
        if (mPost == null) {
            ToastUtils.showToast(getActivity(), R.string.post_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    private void fetchSiteSettingsAndUpdateDefaultPostFormat() {
        // we need to fetch site settings in order to get the latest default post format
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), mSite, new SiteSettingsListener() {
            @Override
            public void onSettingsUpdated(Exception error) {
                if (error == null && TextUtils.isEmpty(mPost.getPostFormat())) {
                    updatePostFormat(mSiteSettings.getDefaultPostFormat());
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(EXTRA_POST_LOCAL_ID, mPost.getId());
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
        mCategoriesTextView = (TextView) rootView.findViewById(R.id.post_categories);
        mTagsTextView = (TextView) rootView.findViewById(R.id.post_tags);
        mStatusTextView = (TextView) rootView.findViewById(R.id.post_status);
        mPostFormatTextView = (TextView) rootView.findViewById(R.id.post_format);
        mPasswordTextView = (TextView) rootView.findViewById(R.id.post_password);
        mPublishDateTextView = (TextView) rootView.findViewById(R.id.publish_date);

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
            categoriesContainer.setVisibility(View.GONE);
            tagsContainer.setVisibility(View.GONE);
            formatContainer.setVisibility(View.GONE);
        }

        initSettingsFields();
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
                clearFeaturedImage();
                return true;
            default:
                return false;
        }
    }

    private void initSettingsFields() {
        if (!isAdded()) {
            return;
        }
        mExcerptTextView.setText(mPost.getExcerpt());
        mSlugTextView.setText(mPost.getSlug());
        mPasswordTextView.setText(mPost.getPassword());
        updatePostFormatTextView();
        updateTagsTextView();
        updateStatusTextView();
        updatePublishDateTextView();
        updateCategoriesTextView();
        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
            updateFeaturedImageView();
        }
    }

    private int getDateTimeFlags() {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        return flags;
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
                    if (extras != null && extras.containsKey(KEY_SELECTED_CATEGORIES)) {
                        @SuppressWarnings("unchecked")
                        List<TermModel> categoryList = (List<TermModel>) extras.getSerializable(KEY_SELECTED_CATEGORIES);
                        updateCategories(categoryList);
                    }
                    break;
                case ACTIVITY_REQUEST_CODE_SELECT_TAGS:
                    extras = data.getExtras();
                    if (resultCode == Activity.RESULT_OK && extras != null) {
                        String selectedTags = extras.getString(PostSettingsTagsActivity.KEY_SELECTED_TAGS);
                        updateTags(selectedTags);
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
        if (id == R.id.locationText) {
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

    /**
     * Updates given post object with current status of settings fields
     */
    public void updatePostSettings(PostModel post) {
        if (!isAdded() || post == null) {
            return;
        }

        boolean publishImmediately = EditTextUtils.getText(mPublishDateTextView).equals(getText(R.string.immediately));

        String publicationDateIso8601 = "";
        if (mIsCustomPublishDate && publishImmediately && !post.isLocalDraft()) {
            publicationDateIso8601 = DateTimeUtils.iso8601FromDate(new Date());
        } else if (!publishImmediately) {
            if (mIsCustomPublishDate) {
                publicationDateIso8601 = mCustomPublishDate;
            } else if (StringUtils.isNotEmpty(post.getDateCreated())) {
                publicationDateIso8601 = post.getDateCreated();
            }
        }

        post.setDateCreated(publicationDateIso8601);
    }

    private void showPostExcerptDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mPost.getExcerpt(), getString(R.string.post_settings_excerpt),
                getString(R.string.post_settings_excerpt_dialog_hint), false);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        updateExcerpt(input);
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void showSlugDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mPost.getSlug(), getString(R.string.post_settings_slug),
                getString(R.string.post_settings_slug_dialog_hint), true);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        updateSlug(input);
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void showCategoriesActivity() {
        Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
        categoriesIntent.putExtra(WordPress.SITE, mSite);
        categoriesIntent.putExtra(EXTRA_POST_LOCAL_ID, mPost.getId());
        startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
    }

    private void showTagsActivity() {
        // Fetch/refresh the tags in preparation for the PostSettingsTagsActivity
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(mSite));

        Intent tagsIntent = new Intent(getActivity(), PostSettingsTagsActivity.class);
        tagsIntent.putExtra(WordPress.SITE, mSite);
        String tags = TextUtils.join(",", mPost.getTagNameList());
        tagsIntent.putExtra(PostSettingsTagsActivity.KEY_TAGS, tags);
        startActivityForResult(tagsIntent, ACTIVITY_REQUEST_CODE_SELECT_TAGS);
    }

    private void showStatusDialog() {
        if (!isAdded()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.post_settings_status);
        builder.setSingleChoiceItems(R.array.post_settings_statuses, getCurrentPostStatusIndex(), null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ListView listView = ((AlertDialog)dialog).getListView();
                int index = listView.getCheckedItemPosition();
                updatePostStatus(getPostStatusAtIndex(index).toString());
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showPostFormatDialog() {
        if (!isAdded()) {
            return;
        }
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
                updatePostFormat(getPostFormatKeyFromName(formatName));
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showPostPasswordDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                mPost.getPassword(), getString(R.string.password),
                getString(R.string.post_settings_password_dialog_hint), false);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        updatePassword(input);
                    }
                });
        dialog.show(getFragmentManager(), null);
    }

    private void showPostDateSelectionDialog() {
        if (!isAdded()) {
            return;
        }
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
                        mIsCustomPublishDate = true;
                        mPublishDateTextView.setText(R.string.immediately);
                        updateSaveButton();
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
        if (!isAdded()) {
            return;
        }
        final TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        mHour = selectedHour;
                        mMinute = selectedMinute;

                        Date javaDate = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute);
                        long javaTimestamp = javaDate.getTime();

                        try {
                            String formattedDate = DateUtils.formatDateTime(getActivity(), javaTimestamp, getDateTimeFlags());
                            mCustomPublishDate = DateTimeUtils.iso8601FromDate(javaDate);
                            mPublishDateTextView.setText(formattedDate);
                            mIsCustomPublishDate = true;

                            updateSaveButton();
                        } catch (RuntimeException e) {
                            AppLog.e(T.POSTS, e);
                        }
                    }
                }, mHour, mMinute, DateFormat.is24HourFormat(getActivity()));
        timePickerDialog.setTitle(R.string.select_time);
        timePickerDialog.show();
    }

    // Helpers

    private void updateSaveButton() {
        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void updateExcerpt(String excerpt) {
        if (mPost.getExcerpt().equals(excerpt)) {
            return;
        }
        mPost.setExcerpt(excerpt);
        dispatchUpdatePostAction();
        if (isAdded()) {
            mExcerptTextView.setText(mPost.getExcerpt());
        }
    }

    private void updateSlug(String slug) {
        if (mPost.getSlug().equalsIgnoreCase(slug)) {
            return;
        }
        mPost.setSlug(slug);
        dispatchUpdatePostAction();
        if (isAdded()) {
            mSlugTextView.setText(mPost.getSlug());
        }
    }

    private void updatePassword(String password) {
        if (mPost.getPassword().equals(password)) {
            return;
        }
        mPost.setPassword(password);
        dispatchUpdatePostAction();
        if (isAdded()) {
            mPasswordTextView.setText(mPost.getPassword());
        }
    }

    private void updateCategories(List<TermModel> categoryList) {
        if (categoryList == null) {
            return;
        }
        List<Long> categoryIds = new ArrayList<>();
        for (TermModel category : categoryList) {
            categoryIds.add(category.getRemoteTermId());
        }
        mPost.setCategoryIdList(categoryIds);
        dispatchUpdatePostAction();
        updateCategoriesTextView();
    }

    private void updatePostStatus(String postStatus) {
        if (mPost.getStatus().equals(postStatus)) {
            return;
        }
        mPost.setStatus(postStatus);
        dispatchUpdatePostAction();
        updateStatusTextView();
        updateSaveButton();
    }

    private void updatePostFormat(String postFormat) {
        if (mPost.getPostFormat().equals(postFormat)) {
            return;
        }
        mPost.setPostFormat(postFormat);
        dispatchUpdatePostAction();
        updatePostFormatTextView();
    }

    public void updateStatusTextView() {
        if (!isAdded()) {
            return;
        }
        String[] statuses = getResources().getStringArray(R.array.post_settings_statuses);
        int index = getCurrentPostStatusIndex();
        // We should never get an OutOfBoundsException here, but if we do,
        // we should let it crash so we can fix the underlying issue
        mStatusTextView.setText(statuses[index]);
    }

    private void updateLocationText(String locationName) {
        if (isAdded()) {
            mLocationText.setText(locationName);
        }
    }

    private void updateTags(String selectedTags) {
        if (!TextUtils.isEmpty(selectedTags)) {
            String tags = selectedTags.replace("\n", " ");
            mPost.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
        } else {
            mPost.setTagNameList(null);
        }
        dispatchUpdatePostAction();
        updateTagsTextView();
    }

    private void updateTagsTextView() {
        if (!isAdded()) {
            return;
        }
        String tags = TextUtils.join(",", mPost.getTagNameList());
        // If `tags` is empty, the hint "Not Set" will be shown instead
        mTagsTextView.setText(tags);
    }

    private void updatePostFormatTextView() {
        if (isAdded()) {
            mPostFormatTextView.setText(getPostFormatNameFromKey(mPost.getPostFormat()));
        }
    }

    private void updatePublishDateTextView() {
        if (!isAdded()) {
            return;
        }
        String pubDate = mPost.getDateCreated();
        if (StringUtils.isNotEmpty(pubDate)) {
            try {
                String formattedDate = DateUtils.formatDateTime(getActivity(),
                        DateTimeUtils.timestampFromIso8601Millis(pubDate), getDateTimeFlags());
                mPublishDateTextView.setText(formattedDate);
            } catch (RuntimeException e) {
                AppLog.e(T.POSTS, e);
            }
        }
    }

    private void updateCategoriesTextView() {
        if (!isAdded()) {
            return;
        }
        List<TermModel> categories = mTaxonomyStore.getCategoriesForPost(mPost, mSite);
        StringBuilder sb = new StringBuilder();
        Iterator<TermModel> it = categories.iterator();
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

    private void dispatchUpdatePostAction() {
        mPost.setIsLocallyChanged(true);
        mPost.setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(System.currentTimeMillis() / 1000));
        mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
    }

    // Post Status Helpers

    private PostStatus getPostStatusAtIndex(int index) {
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
        switch (PostStatus.fromPost(mPost)) {
            case DRAFT:
                return 1;
            case PENDING:
                return 2;
            case PRIVATE:
                return 3;
            default:
                // PUBLISHED, SCHEDULED, UNKNOWN
                return 0;
        }
    }

    // Post Format Helpers

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

    // Featured Image Helpers

    public void updateFeaturedImage(long featuredImageId) {
        if (mPost.getFeaturedImageId() == featuredImageId) {
            return;
        }

        mPost.setFeaturedImageId(featuredImageId);
        dispatchUpdatePostAction();
        updateFeaturedImageView();
    }

    private void clearFeaturedImage() {
        updateFeaturedImage(0);
    }

    private void updateFeaturedImageView() {
        if (!isAdded()) {
            return;
        }

        if (!mPost.hasFeaturedImage()) {
            mFeaturedImageView.setVisibility(View.GONE);
            mFeaturedImageButton.setVisibility(View.VISIBLE);
            return;
        }

        MediaModel media = mMediaStore.getSiteMediaWithId(mSite, mPost.getFeaturedImageId());
        if (media == null) {
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
    }

    private void launchFeaturedMediaPicker() {
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, mSite);
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.SINGLE_SELECT_PICKER);
        intent.putExtra(MediaBrowserActivity.ARG_IMAGES_ONLY, true);
        startActivityForResult(intent, RequestCodes.SINGLE_SELECT_MEDIA_PICKER);
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        if (event.isError()) {
            AppLog.e(T.POSTS, "An error occurred while updating taxonomy with type: " + event.error.type);
            return;
        }
        switch (event.causeOfChange) {
            case FETCH_CATEGORIES:
                updateCategoriesTextView();
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
            if (!isAdded()){
                return;
            }
            String buttonText;
            if (s.length() > 0) {
                buttonText = getResources().getString(R.string.post_settings_search_location);
            } else {
                buttonText = getResources().getString(R.string.post_settings_search_current_location);
            }
            mButtonSearchLocation.setText(buttonText);
        }
    };

    /*
     * called when activity is created to initialize the location provider, show views related
     * to location if enabled for this blog, and retrieve the current location if necessary
     */
    private void initLocation(ViewGroup rootView) {
        if (!isAdded() || !mPost.supportsLocation()) return;

        // show the location views if a provider was found and this is a post on a blog that has location enabled
        View locationRootView = ((ViewStub) rootView.findViewById(R.id.stub_post_location_settings)).inflate();

        TextView locationLabel = ((TextView) locationRootView.findViewById(R.id.locationLabel));
        locationLabel.setText(getResources().getString(R.string.post_settings_location).toUpperCase());

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
        return PermissionUtils.checkLocationPermissions(getActivity(),
                EditPostActivity.LOCATION_PERMISSION_REQUEST_CODE);
    }

    public void showLocationSearch() {
        if (!isAdded()) {
            return;
        }
        mLocationAddSection.setVisibility(View.GONE);
        mLocationSearchSection.setVisibility(View.VISIBLE);
        mLocationViewSection.setVisibility(View.GONE);

        EditTextUtils.showSoftInput(mLocationEditText);
    }

    private void showLocationAdd() {
        if (!isAdded()) {
            return;
        }
        mLocationAddSection.setVisibility(View.VISIBLE);
        mLocationSearchSection.setVisibility(View.GONE);
        mLocationViewSection.setVisibility(View.GONE);
    }

    private void showLocationView() {
        if (!isAdded()) {
            return;
        }
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

    /*
     * called when location is retrieved/updated for this post - looks up the address to
     * display for the lat/long
     */
    private void setLocation(Location location) {
        if (location != null) {
            setLocation(location.getLatitude(), location.getLongitude());
        } else {
            updateLocationText(getString(R.string.post_settings_location_not_found));
            setLocationStatus(LocationStatus.NOT_FOUND);
        }
    }

    private void setLocation(double latitude, double longitude) {
        mPost.setLocation(latitude, longitude);
        dispatchUpdatePostAction();
        new GetAddressTask().execute(mPost.getLatitude(), mPost.getLongitude());
    }

    private void removeLocation() {
        mPost.clearLocation();
        dispatchUpdatePostAction();

        updateLocationText("");
        setLocationStatus(LocationStatus.NONE);
    }

    private void viewLocation() {
        if (mPost.supportsLocation() && mPost.getLocation().isValid()) {
            String locationString = "geo:" + mPost.getLatitude() + "," + mPost.getLongitude();
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
        Toast.makeText(getActivity(), getResources().getText(R.string.post_settings_location_not_found),
                Toast.LENGTH_SHORT).show();
    }
}
