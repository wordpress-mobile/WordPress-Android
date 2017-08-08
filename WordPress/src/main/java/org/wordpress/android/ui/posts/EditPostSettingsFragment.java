package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
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
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.commons.lang3.StringEscapeUtils;
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
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity.MediaBrowserType;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.ListUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID;
import static org.wordpress.android.ui.posts.SelectCategoriesActivity.KEY_SELECTED_CATEGORY_IDS;

public class EditPostSettingsFragment extends Fragment {
    private static final String POST_FORMAT_STANDARD_KEY = "standard";

    private static final int ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES = 5;
    private static final int ACTIVITY_REQUEST_CODE_SELECT_TAGS = 6;
    private static final int ACTIVITY_REQUEST_CODE_PICK_LOCATION = 7;
    private static final int ACTIVITY_REQUEST_PLAY_SERVICES_RESOLUTION = 8;

    private static final int SELECT_LIBRARY_MENU_POSITION = 100;
    private static final int CLEAR_FEATURED_IMAGE_MENU_POSITION = 101;

    private EditPostActivityHook mEditPostActivityHook;
    private SiteSettingsInterface mSiteSettings;

    private LinearLayout mCategoriesContainer;
    private LinearLayout mExcerptContainer;
    private LinearLayout mFormatContainer;
    private LinearLayout mTagsContainer;
    private TextView mExcerptTextView;
    private TextView mSlugTextView;
    private TextView mLocationTextView;
    private TextView mCategoriesTextView;
    private TextView mTagsTextView;
    private TextView mStatusTextView;
    private TextView mPostFormatTextView;
    private TextView mPasswordTextView;
    private TextView mPublishDateTextView;
    private WPNetworkImageView mFeaturedImageView;
    private Button mFeaturedImageButton;

    private PostLocation mPostLocation;

    private ArrayList<String> mPostFormatKeys;
    private ArrayList<String> mPostFormatNames;

    @Inject SiteStore mSiteStore;
    @Inject MediaStore mMediaStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;

    interface EditPostActivityHook {
        PostModel getPost();
        SiteModel getSite();
    }

    public static EditPostSettingsFragment newInstance() {
        return new EditPostSettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        mDispatcher.register(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updatePostFormatKeysAndNames();
        fetchSiteSettingsAndUpdateDefaultPostFormatIfNecessary();

        // Update post formats and categories, in case anything changed.
        SiteModel siteModel = getSite();
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(siteModel));
        if (!getPost().isPage()) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(siteModel));
        }

        refreshViews();
    }

    private void fetchSiteSettingsAndUpdateDefaultPostFormatIfNecessary() {
        // A format is already set for the post, no need to fetch the default post format
        if (!TextUtils.isEmpty(getPost().getPostFormat())) {
            return;
        }
        // we need to fetch site settings in order to get the latest default post format
        mSiteSettings = SiteSettingsInterface.getInterface(getActivity(), getSite(),
                new SiteSettingsListener() {
                    @Override
                    public void onSettingsUpdated(Exception error) {
                        // mEditPostActivityHook will be null if the fragment is detached
                        if (error == null && mEditPostActivityHook != null) {
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

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof EditPostActivityHook) {
            mEditPostActivityHook = (EditPostActivityHook) activity;
        } else {
            throw new RuntimeException(activity.toString() + " must implement PostSettingsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEditPostActivityHook = null;
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.edit_post_settings_fragment, container, false);

        if (rootView == null) {
            return null;
        }

        mExcerptTextView = (TextView) rootView.findViewById(R.id.post_excerpt);
        mSlugTextView = (TextView) rootView.findViewById(R.id.post_slug);
        mLocationTextView = (TextView) rootView.findViewById(R.id.post_location);
        mCategoriesTextView = (TextView) rootView.findViewById(R.id.post_categories);
        mTagsTextView = (TextView) rootView.findViewById(R.id.post_tags);
        mStatusTextView = (TextView) rootView.findViewById(R.id.post_status);
        mPostFormatTextView = (TextView) rootView.findViewById(R.id.post_format);
        mPasswordTextView = (TextView) rootView.findViewById(R.id.post_password);
        mPublishDateTextView = (TextView) rootView.findViewById(R.id.publish_date);

        mFeaturedImageView = (WPNetworkImageView) rootView.findViewById(R.id.post_featured_image);
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

        mExcerptContainer = (LinearLayout) rootView.findViewById(R.id.post_excerpt_container);
        mExcerptContainer.setOnClickListener(new View.OnClickListener() {
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

        mCategoriesContainer = (LinearLayout) rootView.findViewById(R.id.post_categories_container);
        mCategoriesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCategoriesActivity();
            }
        });

        mTagsContainer = (LinearLayout) rootView.findViewById(R.id.post_tags_container);
        mTagsContainer.setOnClickListener(new View.OnClickListener() {
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

        mFormatContainer = (LinearLayout) rootView.findViewById(R.id.post_format_container);
        mFormatContainer.setOnClickListener(new View.OnClickListener() {
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


        if (getPost().isPage()) { // remove post specific views
            final View categoriesTagsContainer = rootView.findViewById(R.id.post_categories_and_tags_card);
            final View formatBottomSeparator = rootView.findViewById(R.id.post_format_bottom_separator);
            categoriesTagsContainer.setVisibility(View.GONE);
            formatBottomSeparator.setVisibility(View.GONE);
            mFormatContainer.setVisibility(View.GONE);
        }

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

    public void refreshViews() {
        if (!isAdded()) {
            return;
        }

        PostModel postModel = getPost();
        if (postModel.isPage()) {
            // remove post specific views
            mCategoriesContainer.setVisibility(View.GONE);
            mExcerptContainer.setVisibility(View.GONE);
            mFormatContainer.setVisibility(View.GONE);
            mTagsContainer.setVisibility(View.GONE);
        }
        mExcerptTextView.setText(postModel.getExcerpt());
        mSlugTextView.setText(postModel.getSlug());
        mPasswordTextView.setText(postModel.getPassword());
        updatePostFormatTextView();
        updateTagsTextView();
        updateStatusTextView();
        updatePublishDateTextView();
        updateCategoriesTextView();
        initLocation();
        if (AppPrefs.isVisualEditorEnabled() || AppPrefs.isAztecEditorEnabled()) {
            updateFeaturedImageView();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO))) {
            Bundle extras;

            switch (requestCode) {
                case ACTIVITY_REQUEST_CODE_PICK_LOCATION:
                    if (isAdded() && resultCode == RESULT_OK) {
                        Place place = PlacePicker.getPlace(getActivity(), data);
                        setLocation(place);
                    }
                    break;
                case ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES:
                    extras = data.getExtras();
                    if (extras != null && extras.containsKey(KEY_SELECTED_CATEGORY_IDS)) {
                        @SuppressWarnings("unchecked")
                        List<Long> categoryList = (ArrayList<Long>) extras.getSerializable(KEY_SELECTED_CATEGORY_IDS);
                        updateCategories(categoryList);
                    }
                    break;
                case ACTIVITY_REQUEST_CODE_SELECT_TAGS:
                    extras = data.getExtras();
                    if (resultCode == RESULT_OK && extras != null) {
                        String selectedTags = extras.getString(PostSettingsTagsActivity.KEY_SELECTED_TAGS);
                        updateTags(selectedTags);
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

    private void showPostExcerptDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                getPost().getExcerpt(), getString(R.string.post_settings_excerpt),
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
                getPost().getSlug(), getString(R.string.post_settings_slug),
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
        if (!isAdded()) {
            return;
        }
        Intent categoriesIntent = new Intent(getActivity(), SelectCategoriesActivity.class);
        categoriesIntent.putExtra(WordPress.SITE, getSite());
        categoriesIntent.putExtra(EXTRA_POST_LOCAL_ID, getPost().getId());
        startActivityForResult(categoriesIntent, ACTIVITY_REQUEST_CODE_SELECT_CATEGORIES);
    }

    private void showTagsActivity() {
        if (!isAdded()) {
            return;
        }
        // Fetch/refresh the tags in preparation for the PostSettingsTagsActivity
        SiteModel siteModel = getSite();
        mDispatcher.dispatch(TaxonomyActionBuilder.newFetchTagsAction(siteModel));

        Intent tagsIntent = new Intent(getActivity(), PostSettingsTagsActivity.class);
        tagsIntent.putExtra(WordPress.SITE, siteModel);
        String tags = TextUtils.join(",", getPost().getTagNameList());
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
        String postFormat = getPost().getPostFormat();
        if (!TextUtils.isEmpty(postFormat)) {
            for (int i = 0; i < mPostFormatKeys.size(); i++) {
                if (postFormat.equals(mPostFormatKeys.get(i))) {
                    checkedItem = i;
                    break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Calypso_AlertDialog);
        builder.setTitle(R.string.post_settings_post_format);
        builder.setSingleChoiceItems(mPostFormatNames.toArray(new CharSequence[0]), checkedItem, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
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
                getPost().getPassword(), getString(R.string.password),
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

        Calendar calendar = getCurrentPublishDateAsCalendar();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        Resources resources = getResources();
        boolean isPublishImmediatelyAvailable = PostUtils.shouldPublishImmediatelyOptionBeAvailable(getPost());

        final DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), null, year, month, day);
        datePickerDialog.setTitle(R.string.select_date);
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, resources.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        DatePicker datePicker = datePickerDialog.getDatePicker();
                        int selectedYear = datePicker.getYear();
                        int selectedMonth = datePicker.getMonth();
                        int selectedDay = datePicker.getDayOfMonth();
                        showPostTimeSelectionDialog(selectedYear, selectedMonth, selectedDay);
                    }
                });
        String neutralButtonTitle = isPublishImmediatelyAvailable ? resources.getString(R.string.immediately)
                : resources.getString(R.string.now);
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, neutralButtonTitle,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Calendar now = Calendar.getInstance();
                        updatePublishDate(now);
                    }
                });
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, resources.getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        if (isPublishImmediatelyAvailable) {
            // We shouldn't let the user pick a past date since we'll just override it to Immediately if they do
            // We can't set the min date to now, so we need to subtract some amount of time
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        }
        datePickerDialog.show();
    }

    private void showPostTimeSelectionDialog(final int selectedYear, final int selectedMonth, final int selectedDay) {
        if (!isAdded()) {
            return;
        }
        final Calendar calendar = getCurrentPublishDateAsCalendar();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        final TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
                        updatePublishDate(selectedCalendar);
                    }
                }, hour, minute, DateFormat.is24HourFormat(getActivity()));
        timePickerDialog.setTitle(R.string.select_time);
        timePickerDialog.show();
    }

    // Helpers

    private PostModel getPost() {
        if (mEditPostActivityHook == null) {
            // This can only happen during a callback while activity is re-created for some reason (config changes etc)
            return null;
        }
        return mEditPostActivityHook.getPost();
    }

    private SiteModel getSite() {
        if (mEditPostActivityHook == null) {
            // This can only happen during a callback while activity is re-created for some reason (config changes etc)
            return null;
        }
        return mEditPostActivityHook.getSite();
    }

    private void updateSaveButton() {
        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void updateExcerpt(String excerpt) {
        getPost().setExcerpt(excerpt);
        mExcerptTextView.setText(excerpt);
    }

    private void updateSlug(String slug) {
        getPost().setSlug(slug);
        mSlugTextView.setText(slug);
    }

    private void updatePassword(String password) {
        getPost().setPassword(password);
        mPasswordTextView.setText(password);
    }

    private void updateCategories(List<Long> categoryList) {
        if (categoryList == null) {
            return;
        }
        getPost().setCategoryIdList(categoryList);
        updateCategoriesTextView();
    }

    private void updatePostStatus(String postStatus) {
        getPost().setStatus(postStatus);
        updatePostStatusRelatedViews();
        updateSaveButton();
    }

    private void updatePostFormat(String postFormat) {
        getPost().setPostFormat(postFormat);
        updatePostFormatTextView();
    }

    public void updatePostStatusRelatedViews() {
        updateStatusTextView();
        updatePublishDateTextView();
    }

    private void updateStatusTextView() {
        if (!isAdded()) {
            return;
        }
        String[] statuses = getResources().getStringArray(R.array.post_settings_statuses);
        int index = getCurrentPostStatusIndex();
        // We should never get an OutOfBoundsException here, but if we do,
        // we should let it crash so we can fix the underlying issue
        mStatusTextView.setText(statuses[index]);
    }

    private void updateTags(String selectedTags) {
        PostModel postModel = getPost();
        if (!TextUtils.isEmpty(selectedTags)) {
            String tags = selectedTags.replace("\n", " ");
            postModel.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
        } else {
            postModel.setTagNameList(null);
        }
        updateTagsTextView();
    }

    private void updateTagsTextView() {
        String tags = TextUtils.join(",", getPost().getTagNameList());
        // If `tags` is empty, the hint "Not Set" will be shown instead
        tags = StringEscapeUtils.unescapeHtml4(tags);
        mTagsTextView.setText(tags);
    }

    private void updatePostFormatTextView() {
        // Post format can be updated due to a site settings fetch and the textView might not have been initialized yet
        if (mPostFormatTextView == null) {
            return;
        }
        String postFormat = getPostFormatNameFromKey(getPost().getPostFormat());
        mPostFormatTextView.setText(postFormat);
    }

    private void updatePublishDate(Calendar calendar) {
        getPost().setDateCreated(DateTimeUtils.iso8601FromDate(calendar.getTime()));
        updatePublishDateTextView();
        updateSaveButton();
    }

    private void updatePublishDateTextView() {
        if (!isAdded()) {
            return;
        }
        PostModel postModel = getPost();
        if (PostUtils.shouldPublishImmediately(postModel)) {
            mPublishDateTextView.setText(R.string.immediately);
        } else {
            String dateCreated = postModel.getDateCreated();
            if (!TextUtils.isEmpty(dateCreated)){
                String formattedDate = DateUtils.formatDateTime(getActivity(),
                        DateTimeUtils.timestampFromIso8601Millis(dateCreated), getDateTimeFlags());
                mPublishDateTextView.setText(formattedDate);
            }
        }
    }

    private void updateCategoriesTextView() {
        if (getPost() == null || getSite() == null) {
            // Since this method can get called after a callback, we have to make sure we have the post and site
            return;
        }
        List<TermModel> categories = mTaxonomyStore.getCategoriesForPost(getPost(), getSite());
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
        mCategoriesTextView.setText(StringEscapeUtils.unescapeHtml4(sb.toString()));
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
        switch (PostStatus.fromPost(getPost())) {
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
        if (getActivity() == null || getSite() == null) {
            // Since this method can get called after a callback, we have to make sure we have the site
            return;
        }
        // Default values
        mPostFormatKeys = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.post_format_keys)));
        mPostFormatNames = new ArrayList<>(Arrays.asList(getResources()
                .getStringArray(R.array.post_format_display_names)));

        // If we have specific values for this site, use them
        List<PostFormatModel> postFormatModels = mSiteStore.getPostFormats(getSite());
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
        PostModel postModel = getPost();
        if (postModel.getFeaturedImageId() == featuredImageId) {
            return;
        }

        postModel.setFeaturedImageId(featuredImageId);
        updateFeaturedImageView();
    }

    private void clearFeaturedImage() {
        updateFeaturedImage(0);
    }

    private void updateFeaturedImageView() {
        if (!isAdded()) {
            return;
        }
        PostModel postModel = getPost();
        if (!postModel.hasFeaturedImage()) {
            mFeaturedImageView.setVisibility(View.GONE);
            mFeaturedImageButton.setVisibility(View.VISIBLE);
            return;
        }

        SiteModel siteModel = getSite();
        MediaModel media = mMediaStore.getSiteMediaWithId(siteModel, postModel.getFeaturedImageId());
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
        if (SiteUtils.isPhotonCapable(siteModel)) {
            mediaUri = PhotonUtils.getPhotonImageUrl(mediaUri, size, 0);
        }

        WPMediaUtils.loadNetworkImage(mediaUri, mFeaturedImageView);
    }

    private void launchFeaturedMediaPicker() {
        if (!isAdded()) {
            return;
        }
        Intent intent = new Intent(getActivity(), MediaBrowserActivity.class);
        intent.putExtra(WordPress.SITE, getSite());
        intent.putExtra(MediaBrowserActivity.ARG_BROWSER_TYPE, MediaBrowserType.SINGLE_SELECT_IMAGE_PICKER);
        startActivityForResult(intent, RequestCodes.SINGLE_SELECT_MEDIA_PICKER);
    }

    // Publish Date Helpers

    private Calendar getCurrentPublishDateAsCalendar() {
        PostModel postModel = getPost();
        if (PostUtils.shouldPublishImmediately(postModel)) {
            return Calendar.getInstance();
        }
        Calendar calendar = Calendar.getInstance();
        String dateCreated = postModel.getDateCreated();
        // Set the currently selected time if available
        if (!TextUtils.isEmpty(dateCreated)) {
            calendar.setTime(DateTimeUtils.dateFromIso8601(dateCreated));
        }
        return calendar;
    }

    private int getDateTimeFlags() {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        return flags;
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
        AppLog.v(T.POSTS, "Post formats successfully fetched!");
        updatePostFormatKeysAndNames();
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
            if (getActivity() == null) {
                return null;
            }
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
            if (address == null || address.getMaxAddressLineIndex() == 0) {
                // Do nothing (keep the "lat, long" format).
                return;
            }
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
    }

    private void showLocationPicker() {
        if (!isAdded()) {
            return;
        }
        PostModel postModel = getPost();
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        // Pre-pick the previous selected location if any
        LatLng latLng = null;
        if (mPostLocation != null) {
            latLng = new LatLng(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        } else if (postModel.hasLocation()) {
            PostLocation location = postModel.getLocation();
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
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), re.getConnectionStatusCode(),
                    ACTIVITY_REQUEST_PLAY_SERVICES_RESOLUTION);
        }
    }

    private void setLocation(@Nullable Place place) {
        PostModel postModel = getPost();
        if (place == null) {
            postModel.clearLocation();
            mLocationTextView.setText("");
            mPostLocation = null;
            return;
        }
        if (mPostLocation == null) {
            mPostLocation = new PostLocation();
        }
        mPostLocation.setLatitude(place.getLatLng().latitude);
        mPostLocation.setLongitude(place.getLatLng().longitude);
        postModel.setLocation(mPostLocation);
        mLocationTextView.setText(place.getAddress());
    }

    private void initLocation() {
        PostModel postModel = getPost();
        if (!postModel.hasLocation()) {
            mPostLocation = null;
        } else {
            mPostLocation = postModel.getLocation();
            mLocationTextView.setText(mPostLocation.getLatitude() + ", " + mPostLocation.getLongitude());
            // Asynchronously get the address from the location coordinates
            new FetchAndSetAddressAsyncTask().execute(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        }
    }

    private void showLocationPickerOrPopupMenu(@NonNull final View view) {
        if (!isAdded()) {
            return;
        }

        // If the post doesn't have location set, show the picker directly
        if (!getPost().hasLocation()) {
            showLocationPicker();
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
}
