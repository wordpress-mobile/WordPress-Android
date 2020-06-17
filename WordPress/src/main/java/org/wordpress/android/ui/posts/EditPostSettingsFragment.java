package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.TaxonomyAction;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.generated.TaxonomyActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.model.post.PostLocation;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.MediaStore.OnMediaUploaded;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnPostFormatsChanged;
import org.wordpress.android.fluxc.store.TaxonomyStore;
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaBrowserType;
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel.PublishUiModel;
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult;
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageData;
import org.wordpress.android.ui.posts.FeaturedImageHelper.FeaturedImageState;
import org.wordpress.android.ui.posts.FeaturedImageHelper.TrackableEvent;
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment.DialogType;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsInterface.SiteSettingsListener;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.GeocoderUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageManager.RequestListener;
import org.wordpress.android.util.image.ImageType;

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

    private static final int CHOOSE_FEATURED_IMAGE_MENU_ID = 100;
    private static final int REMOVE_FEATURED_IMAGE_MENU_ID = 101;
    private static final int REMOVE_FEATURED_IMAGE_UPLOAD_MENU_ID = 102;
    private static final int RETRY_FEATURED_IMAGE_UPLOAD_MENU_ID = 103;

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
    private TextView mCategoriesTagsHeaderTextView;
    private TextView mFeaturedImageHeaderTextView;
    private TextView mMoreOptionsHeaderTextView;
    private TextView mPublishHeaderTextView;
    private ImageView mFeaturedImageView;
    private ImageView mLocalFeaturedImageView;
    private Button mFeaturedImageButton;
    private ViewGroup mFeaturedImageRetryOverlay;
    private ViewGroup mFeaturedImageProgressOverlay;

    private PostLocation mPostLocation;

    private ArrayList<String> mDefaultPostFormatKeys;
    private ArrayList<String> mDefaultPostFormatNames;
    private ArrayList<String> mPostFormatKeys;
    private ArrayList<String> mPostFormatNames;

    @Inject SiteStore mSiteStore;
    @Inject TaxonomyStore mTaxonomyStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;
    @Inject FeaturedImageHelper mFeaturedImageHelper;
    @Inject UiHelpers mUiHelpers;
    @Inject PostSettingsUtils mPostSettingsUtils;

    @Inject ViewModelProvider.Factory mViewModelFactory;
    private EditPostPublishSettingsViewModel mPublishedViewModel;


    interface EditPostActivityHook {
        EditPostRepository getEditPostRepository();

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

        // Early load the default lists for post format keys and names.
        // Will use it later without needing to have access to the Resources.
        mDefaultPostFormatKeys =
                new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.post_format_keys)));
        mDefaultPostFormatNames = new ArrayList<>(Arrays.asList(getResources()
                .getStringArray(R.array.post_format_display_names)));
        mPublishedViewModel =
                ViewModelProviders.of(getActivity(), mViewModelFactory).get(EditPostPublishSettingsViewModel.class);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updatePostFormatKeysAndNames();
        fetchSiteSettingsAndUpdateDefaultPostFormatIfNecessary();

        // Update post formats and categories, in case anything changed.
        SiteModel siteModel = getSite();
        mDispatcher.dispatch(SiteActionBuilder.newFetchPostFormatsAction(siteModel));
        if (!getEditPostRepository().isPage()) {
            mDispatcher.dispatch(TaxonomyActionBuilder.newFetchCategoriesAction(siteModel));
        }

        refreshViews();
    }

    private void fetchSiteSettingsAndUpdateDefaultPostFormatIfNecessary() {
        // A format is already set for the post, no need to fetch the default post format
        if (!TextUtils.isEmpty(getEditPostRepository().getPostFormat())) {
            return;
        }
        // we need to fetch site settings in order to get the latest default post format
        mSiteSettings = SiteSettingsInterface.getInterface(
                getActivity(), getSite(), new SiteSettingsListener() {
                    @Override
                    public void onSaveError(Exception error) {
                        // no-op
                    }

                    @Override
                    public void onFetchError(Exception error) {
                        // no-op
                    }

                    @Override
                    public void onSettingsUpdated() {
                        // mEditPostActivityHook will be null if the fragment is detached
                        if (getEditPostActivityHook() != null) {
                            updatePostFormat(
                                    mSiteSettings.getDefaultPostFormat());
                        }
                    }

                    @Override
                    public void onSettingsSaved() {
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
    public void onDestroy() {
        mDispatcher.unregister(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.edit_post_settings_fragment, container, false);

        if (rootView == null) {
            return null;
        }

        mExcerptTextView = rootView.findViewById(R.id.post_excerpt);
        mSlugTextView = rootView.findViewById(R.id.post_slug);
        mLocationTextView = rootView.findViewById(R.id.post_location);
        mCategoriesTextView = rootView.findViewById(R.id.post_categories);
        mTagsTextView = rootView.findViewById(R.id.post_tags);
        mStatusTextView = rootView.findViewById(R.id.post_status);
        mPostFormatTextView = rootView.findViewById(R.id.post_format);
        mPasswordTextView = rootView.findViewById(R.id.post_password);
        mPublishDateTextView = rootView.findViewById(R.id.publish_date);
        mCategoriesTagsHeaderTextView = rootView.findViewById(R.id.post_settings_categories_and_tags_header);
        mMoreOptionsHeaderTextView = rootView.findViewById(R.id.post_settings_more_options_header);
        mFeaturedImageHeaderTextView = rootView.findViewById(R.id.post_settings_featured_image_header);
        mPublishHeaderTextView = rootView.findViewById(R.id.post_settings_publish);

        mFeaturedImageView = rootView.findViewById(R.id.post_featured_image);
        mLocalFeaturedImageView = rootView.findViewById(R.id.post_featured_image_local);
        mFeaturedImageButton = rootView.findViewById(R.id.post_add_featured_image_button);
        mFeaturedImageRetryOverlay = rootView.findViewById(R.id.post_featured_image_retry_overlay);
        mFeaturedImageProgressOverlay = rootView.findViewById(R.id.post_featured_image_progress_overlay);

        OnClickListener showContextMenuListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.showContextMenu();
            }
        };

        mFeaturedImageView.setOnClickListener(showContextMenuListener);
        mLocalFeaturedImageView.setOnClickListener(showContextMenuListener);
        mFeaturedImageRetryOverlay.setOnClickListener(showContextMenuListener);
        mFeaturedImageProgressOverlay.setOnClickListener(showContextMenuListener);

        registerForContextMenu(mFeaturedImageView);
        registerForContextMenu(mLocalFeaturedImageView);
        registerForContextMenu(mFeaturedImageRetryOverlay);
        registerForContextMenu(mFeaturedImageProgressOverlay);

        mFeaturedImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFeaturedMediaPicker();
            }
        });

        mExcerptContainer = rootView.findViewById(R.id.post_excerpt_container);
        mExcerptContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostExcerptDialog();
            }
        });

        final LinearLayout slugContainer = rootView.findViewById(R.id.post_slug_container);
        slugContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSlugDialog();
            }
        });

        final LinearLayout locationContainer = rootView.findViewById(R.id.post_location_container);
        locationContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLocationPickerOrPopupMenu(view);
            }
        });

        mCategoriesContainer = rootView.findViewById(R.id.post_categories_container);
        mCategoriesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showCategoriesActivity();
            }
        });

        mTagsContainer = rootView.findViewById(R.id.post_tags_container);
        mTagsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTagsActivity();
            }
        });

        final LinearLayout statusContainer = rootView.findViewById(R.id.post_status_container);
        statusContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showStatusDialog();
            }
        });

        mFormatContainer = rootView.findViewById(R.id.post_format_container);
        mFormatContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostFormatDialog();
            }
        });

        final LinearLayout passwordContainer = rootView.findViewById(R.id.post_password_container);
        passwordContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPostPasswordDialog();
            }
        });

        final LinearLayout publishDateContainer = rootView.findViewById(R.id.publish_date_container);
        publishDateContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentActivity activity = getActivity();
                if (activity instanceof EditPostSettingsCallback) {
                    ((EditPostSettingsCallback) activity).onEditPostPublishedSettingsClick();
                }
            }
        });


        if (getEditPostRepository() != null && getEditPostRepository().isPage()) { // remove post specific views
            final View categoriesTagsContainer = rootView.findViewById(R.id.post_categories_and_tags_card);
            final View formatBottomSeparator = rootView.findViewById(R.id.post_format_bottom_separator);
            categoriesTagsContainer.setVisibility(View.GONE);
            formatBottomSeparator.setVisibility(View.GONE);
            mFormatContainer.setVisibility(View.GONE);
        }

        mPublishedViewModel.getOnUiModel().observe(getViewLifecycleOwner(), new Observer<PublishUiModel>() {
            @Override public void onChanged(PublishUiModel uiModel) {
                updatePublishDateTextView(uiModel.getPublishDateLabel());
            }
        });
        mPublishedViewModel.getOnPostStatusChanged().observe(getViewLifecycleOwner(), new Observer<PostStatus>() {
            @Override public void onChanged(PostStatus postStatus) {
                updatePostStatus(postStatus);
            }
        });

        setupSettingHintsForAccessibility();
        applyAccessibilityHeadingToSettings();

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (mFeaturedImageRetryOverlay.getVisibility() == View.VISIBLE) {
            menu.add(0, RETRY_FEATURED_IMAGE_UPLOAD_MENU_ID, 0,
                    getString(R.string.post_settings_retry_featured_image));
            menu.add(0, REMOVE_FEATURED_IMAGE_UPLOAD_MENU_ID, 0,
                    getString(R.string.post_settings_remove_featured_image));
        } else {
            menu.add(0, CHOOSE_FEATURED_IMAGE_MENU_ID, 0, getString(R.string.post_settings_choose_featured_image));
            menu.add(0, REMOVE_FEATURED_IMAGE_MENU_ID, 0, getString(R.string.post_settings_remove_featured_image));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        SiteModel site = getSite();
        PostImmutableModel post = getEditPostRepository().getPost();
        if (site == null || post == null) {
            AppLog.w(T.POSTS, "Unexpected state: Post or Site is null.");
            return false;
        }
        switch (item.getItemId()) {
            case CHOOSE_FEATURED_IMAGE_MENU_ID:
                mFeaturedImageHelper.cancelFeaturedImageUpload(site, post, false);
                launchFeaturedMediaPicker();
                return true;
            case REMOVE_FEATURED_IMAGE_UPLOAD_MENU_ID:
            case REMOVE_FEATURED_IMAGE_MENU_ID:
                mFeaturedImageHelper.cancelFeaturedImageUpload(site, post, false);
                clearFeaturedImage();

                mFeaturedImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_REMOVE_CLICKED, post.getId());

                return true;
            case RETRY_FEATURED_IMAGE_UPLOAD_MENU_ID:
                retryFeaturedImageUpload(site, post);
                return true;
            default:
                return false;
        }
    }

    private void setupSettingHintsForAccessibility() {
        AccessibilityUtils.disableHintAnnouncement(mPublishDateTextView);
        AccessibilityUtils.disableHintAnnouncement(mCategoriesTextView);
        AccessibilityUtils.disableHintAnnouncement(mTagsTextView);
        AccessibilityUtils.disableHintAnnouncement(mPasswordTextView);
        AccessibilityUtils.disableHintAnnouncement(mSlugTextView);
        AccessibilityUtils.disableHintAnnouncement(mExcerptTextView);
        AccessibilityUtils.disableHintAnnouncement(mLocationTextView);
    }

    private void applyAccessibilityHeadingToSettings() {
        AccessibilityUtils.enableAccessibilityHeading(mCategoriesTagsHeaderTextView);
        AccessibilityUtils.enableAccessibilityHeading(mFeaturedImageHeaderTextView);
        AccessibilityUtils.enableAccessibilityHeading(mMoreOptionsHeaderTextView);
        AccessibilityUtils.enableAccessibilityHeading(mPublishHeaderTextView);
    }

    private void retryFeaturedImageUpload(@NonNull SiteModel site, @NonNull PostImmutableModel post) {
        MediaModel mediaModel = mFeaturedImageHelper.retryFeaturedImageUpload(site, post);
        if (mediaModel == null) {
            clearFeaturedImage();
        }
    }

    public void refreshViews() {
        if (!isAdded()) {
            return;
        }

        if (getEditPostRepository().isPage()) {
            // remove post specific views
            mCategoriesContainer.setVisibility(View.GONE);
            mExcerptContainer.setVisibility(View.GONE);
            mFormatContainer.setVisibility(View.GONE);
            mTagsContainer.setVisibility(View.GONE);
        }
        mExcerptTextView.setText(getEditPostRepository().getExcerpt());
        mSlugTextView.setText(getEditPostRepository().getSlug());
        mPasswordTextView.setText(getEditPostRepository().getPassword());
        PostImmutableModel postModel = getEditPostRepository().getPost();
        updatePostFormatTextView(postModel);
        updateTagsTextView(postModel);
        updateStatusTextView();
        updatePublishDateTextView(postModel);
        mPublishedViewModel.start(getEditPostRepository());
        updateCategoriesTextView(postModel);
        initLocation();
        updateFeaturedImageView(postModel);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || ((requestCode == RequestCodes.TAKE_PHOTO
                              || requestCode == RequestCodes.TAKE_VIDEO))) {
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
            }
        }
    }

    private void showPostExcerptDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                getEditPostRepository().getExcerpt(), getString(R.string.post_settings_excerpt),
                getString(R.string.post_settings_excerpt_dialog_hint), false);
        dialog.setPostSettingsInputDialogListener(
                new PostSettingsInputDialogFragment.PostSettingsInputDialogListener() {
                    @Override
                    public void onInputUpdated(String input) {
                        updateExcerpt(input);
                    }
                });
        dialog.show(getChildFragmentManager(), null);
    }

    private void showSlugDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                getEditPostRepository().getSlug(), getString(R.string.post_settings_slug),
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
        categoriesIntent.putExtra(EXTRA_POST_LOCAL_ID, getEditPostRepository().getId());
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
        String tags = TextUtils.join(",", getEditPostRepository().getTagNameList());
        tagsIntent.putExtra(PostSettingsTagsActivity.KEY_TAGS, tags);
        startActivityForResult(tagsIntent, ACTIVITY_REQUEST_CODE_SELECT_TAGS);
    }

    /*
     * called by the activity when the user taps OK on a PostSettingsDialogFragment
     */
    public void onPostSettingsFragmentPositiveButtonClicked(@NonNull PostSettingsListDialogFragment fragment) {
        switch (fragment.getDialogType()) {
            case POST_STATUS:
                int index = fragment.getCheckedIndex();
                PostStatus status = getPostStatusAtIndex(index);
                updatePostStatus(status);
                break;
            case POST_FORMAT:
                String formatName = fragment.getSelectedItem();
                updatePostFormat(getPostFormatKeyFromName(formatName));
                break;
        }
    }

    private void showStatusDialog() {
        if (!isAdded()) {
            return;
        }

        int index = getCurrentPostStatusIndex();
        FragmentManager fm = getActivity().getSupportFragmentManager();
        PostSettingsListDialogFragment fragment =
                PostSettingsListDialogFragment.newInstance(DialogType.POST_STATUS, index);
        fragment.show(fm, PostSettingsListDialogFragment.TAG);
    }

    private void showPostFormatDialog() {
        if (!isAdded()) {
            return;
        }

        int checkedIndex = 0;
        String postFormat = getEditPostRepository().getPostFormat();
        if (!TextUtils.isEmpty(postFormat)) {
            for (int i = 0; i < mPostFormatKeys.size(); i++) {
                if (postFormat.equals(mPostFormatKeys.get(i))) {
                    checkedIndex = i;
                    break;
                }
            }
        }

        FragmentManager fm = getActivity().getSupportFragmentManager();
        PostSettingsListDialogFragment fragment =
                PostSettingsListDialogFragment.newInstance(DialogType.POST_FORMAT, checkedIndex);
        fragment.show(fm, PostSettingsListDialogFragment.TAG);
    }

    private void showPostPasswordDialog() {
        if (!isAdded()) {
            return;
        }
        PostSettingsInputDialogFragment dialog = PostSettingsInputDialogFragment.newInstance(
                getEditPostRepository().getPassword(), getString(R.string.password),
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

    // Helpers

    private EditPostRepository getEditPostRepository() {
        if (getEditPostActivityHook() == null) {
            // This can only happen during a callback while activity is re-created for some reason (config changes etc)
            return null;
        }
        return getEditPostActivityHook().getEditPostRepository();
    }

    private SiteModel getSite() {
        if (getEditPostActivityHook() == null) {
            // This can only happen during a callback while activity is re-created for some reason (config changes etc)
            return null;
        }
        return getEditPostActivityHook().getSite();
    }

    private EditPostActivityHook getEditPostActivityHook() {
        Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        if (activity instanceof EditPostActivityHook) {
            return (EditPostActivityHook) activity;
        } else {
            throw new RuntimeException(activity.toString() + " must implement EditPostActivityHook");
        }
    }

    private void updateSaveButton() {
        if (isAdded()) {
            getActivity().invalidateOptionsMenu();
        }
    }

    private void updateExcerpt(String excerpt) {
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setExcerpt(excerpt);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    mExcerptTextView.setText(excerpt);
                }
                return null;
            });
        }
    }

    private void updateSlug(String slug) {
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setSlug(slug);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    mSlugTextView.setText(slug);
                }
                return null;
            });
        }
    }

    private void updatePassword(String password) {
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setPassword(password);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    mPasswordTextView.setText(password);
                }
                return null;
            });
        }
    }

    private void updateCategories(List<Long> categoryList) {
        if (categoryList == null) {
            return;
        }
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setCategoryIdList(categoryList);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    updateCategoriesTextView(postModel);
                }
                return null;
            });
        }
    }

    public void updatePostStatus(PostStatus postStatus) {
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setStatus(postStatus.toString());
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    updatePostStatusRelatedViews(postModel);
                    updateSaveButton();
                }
                return null;
            });
        }
    }

    private void updatePostFormat(String postFormat) {
        EditPostRepository editPostRepository = getEditPostRepository();
        if (editPostRepository != null) {
            editPostRepository.updateAsync(postModel -> {
                postModel.setPostFormat(postFormat);
                return true;
            }, (postModel, result) -> {
                if (result == UpdatePostResult.Updated.INSTANCE) {
                    updatePostFormatTextView(postModel);
                }
                return null;
            });
        }
    }

    public void updatePostStatusRelatedViews(PostImmutableModel postModel) {
        updateStatusTextView();
        updatePublishDateTextView(postModel);
        mPublishedViewModel.onPostStatusChanged(postModel);
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
        EditPostRepository postRepository = getEditPostRepository();
        if (postRepository == null) {
            return;
        }
        postRepository.updateAsync(postModel -> {
            if (!TextUtils.isEmpty(selectedTags)) {
                String tags = selectedTags.replace("\n", " ");
                postModel.setTagNameList(Arrays.asList(TextUtils.split(tags, ",")));
            } else {
                postModel.setTagNameList(new ArrayList<>());
            }
            return true;
        }, (postModel, result) -> {
            if (result == UpdatePostResult.Updated.INSTANCE) {
                updateTagsTextView(postModel);
            }
            return null;
        });
    }

    private void updateTagsTextView(PostImmutableModel postModel) {
        String tags = TextUtils.join(",", postModel.getTagNameList());
        // If `tags` is empty, the hint "Not Set" will be shown instead
        tags = StringEscapeUtils.unescapeHtml4(tags);
        mTagsTextView.setText(tags);
    }

    private void updatePostFormatTextView(PostImmutableModel postModel) {
        // Post format can be updated due to a site settings fetch and the textView might not have been initialized yet
        if (mPostFormatTextView == null) {
            return;
        }
        String postFormat = getPostFormatNameFromKey(postModel.getPostFormat());
        mPostFormatTextView.setText(postFormat);
    }

    private void updatePublishDateTextView(PostImmutableModel postModel) {
        if (!isAdded()) {
            return;
        }
        if (postModel != null) {
            String labelToUse = mPostSettingsUtils.getPublishDateLabel(postModel);
            mPublishDateTextView.setText(labelToUse);
        }
    }

    private void updatePublishDateTextView(String label) {
        mPublishDateTextView.setText(label);
    }

    private void updateCategoriesTextView(PostImmutableModel post) {
        if (post == null || getSite() == null) {
            // Since this method can get called after a callback, we have to make sure we have the post and site
            return;
        }
        List<TermModel> categories = mTaxonomyStore.getCategoriesForPost(post, getSite());
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
        switch (getEditPostRepository().getStatus()) {
            case DRAFT:
                return 1;
            case PENDING:
                return 2;
            case PRIVATE:
                return 3;
            case TRASHED:
            case UNKNOWN:
            case PUBLISHED:
            case SCHEDULED:
                return 0;
        }
        return 0;
    }

    // Post Format Helpers

    private void updatePostFormatKeysAndNames() {
        final SiteModel site = getSite();
        if (site == null) {
            // Since this method can get called after a callback, we have to make sure we have the site
            return;
        }

        // Initialize the lists from the defaults
        mPostFormatKeys = new ArrayList<>(mDefaultPostFormatKeys);
        mPostFormatNames = new ArrayList<>(mDefaultPostFormatNames);

        // If we have specific values for this site, use them
        List<PostFormatModel> postFormatModels = mSiteStore.getPostFormats(site);
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
        EditPostRepository postRepository = getEditPostRepository();
        if (postRepository == null) {
            return;
        }
        postRepository.updateAsync(postModel -> {
            postModel.setFeaturedImageId(featuredImageId);
            return true;
        }, (postModel, result) -> {
            if (result == UpdatePostResult.Updated.INSTANCE) {
                updateFeaturedImageView(postModel);
            }
            return null;
        });
    }

    private void clearFeaturedImage() {
        updateFeaturedImage(0);
    }

    private void updateFeaturedImageView(PostImmutableModel postModel) {
        Context context = getContext();
        SiteModel site = getSite();
        if (!isAdded() || postModel == null || site == null || context == null) {
            return;
        }
        final FeaturedImageData currentFeaturedImageState =
                mFeaturedImageHelper.createCurrentFeaturedImageState(site, postModel);

        FeaturedImageState uiState = currentFeaturedImageState.getUiState();
        updateFeaturedImageViews(currentFeaturedImageState.getUiState());
        if (currentFeaturedImageState.getMediaUri() != null) {
            if (uiState == FeaturedImageState.REMOTE_IMAGE_LOADING) {
                /*
                 *  Fetch the remote image, but keep showing the local image (when present) until "onResourceReady"
                 *  is invoked.  We use this hack to prevent showing an empty view when the local image is replaced
                 *  with a remote image.
                 */
                mImageManager.loadWithResultListener(mFeaturedImageView, ImageType.IMAGE,
                        currentFeaturedImageState.getMediaUri(), ScaleType.FIT_CENTER,
                        null, new RequestListener<Drawable>() {
                            @Override public void onLoadFailed(@Nullable Exception e, @Nullable Object model) {
                            }

                            @Override public void onResourceReady(@NonNull Drawable resource, @Nullable Object model) {
                                if (currentFeaturedImageState.getUiState() == FeaturedImageState.REMOTE_IMAGE_LOADING) {
                                    updateFeaturedImageViews(FeaturedImageState.REMOTE_IMAGE_SET);
                                }
                            }
                        });
            } else {
                mImageManager.load(mLocalFeaturedImageView, ImageType.IMAGE, currentFeaturedImageState.getMediaUri(),
                        ScaleType.FIT_CENTER);
            }
        }
    }

    private void launchFeaturedMediaPicker() {
        if (isAdded()) {
            int postId = getEditPostRepository().getId();
            mFeaturedImageHelper.trackFeaturedImageEvent(TrackableEvent.IMAGE_SET_CLICKED, postId);

            ActivityLauncher.showPhotoPickerForResult(getActivity(), MediaBrowserType.FEATURED_IMAGE_PICKER, getSite(),
                    postId);
        }
    }

    // Publish Date Helpers

    private Calendar getCurrentPublishDateAsCalendar() {
        Calendar calendar = Calendar.getInstance();
        String dateCreated = getEditPostRepository().getDateCreated();
        // Set the currently selected time if available
        if (!TextUtils.isEmpty(dateCreated)) {
            calendar.setTime(DateTimeUtils.dateFromIso8601(dateCreated));
        }
        return calendar;
    }

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTaxonomyChanged(OnTaxonomyChanged event) {
        if (event.isError()) {
            AppLog.e(T.POSTS, "An error occurred while updating taxonomy with type: " + event.error.type);
            return;
        }
        if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES) {
            updateCategoriesTextView(getEditPostRepository().getPost());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onPostFormatsChanged(OnPostFormatsChanged event) {
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
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        // Pre-pick the previous selected location if any
        LatLng latLng = null;
        if (mPostLocation != null) {
            latLng = new LatLng(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        } else if (getEditPostRepository().hasLocation()) {
            PostLocation location = getEditPostRepository().getLocation();
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
        EditPostRepository postRepository = getEditPostRepository();
        if (postRepository == null) {
            return;
        }
        postRepository.updateAsync(postModel -> {
            if (place == null) {
                postModel.clearLocation();
                mPostLocation = null;
                return false;
            }
            mPostLocation = new PostLocation(place.getLatLng().latitude, place.getLatLng().longitude);
            postModel.setLocation(mPostLocation);
            return true;
        }, (postModel, result) -> {
            if (result == UpdatePostResult.Updated.INSTANCE) {
                if (place == null) {
                    mLocationTextView.setText("");
                } else {
                    mLocationTextView.setText(place.getAddress());
                }
            }
            return null;
        });
    }

    private void initLocation() {
        if (!getEditPostRepository().hasLocation()) {
            mPostLocation = null;
        } else {
            mPostLocation = getEditPostRepository().getLocation();
            mLocationTextView.setText(getString(
                    R.string.latitude_longitude, mPostLocation.getLatitude(), mPostLocation.getLongitude()));
            // Asynchronously get the address from the location coordinates
            new FetchAndSetAddressAsyncTask().execute(mPostLocation.getLatitude(), mPostLocation.getLongitude());
        }
    }

    private void showLocationPickerOrPopupMenu(@NonNull final View view) {
        if (!isAdded()) {
            return;
        }

        // If the post doesn't have location set, show the picker directly
        if (!getEditPostRepository().hasLocation()) {
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMediaUploaded(OnMediaUploaded event) {
        if (event.media.getMarkedLocallyAsFeatured()) {
            refreshViews();
        }
    }

    private void updateFeaturedImageViews(FeaturedImageState state) {
        mUiHelpers.updateVisibility(mFeaturedImageView, state.getImageViewVisible());
        mUiHelpers.updateVisibility(mLocalFeaturedImageView, state.getLocalImageViewVisible());
        mUiHelpers.updateVisibility(mFeaturedImageButton, state.getButtonVisible());
        mUiHelpers.updateVisibility(mFeaturedImageRetryOverlay, state.getRetryOverlayVisible());
        mUiHelpers.updateVisibility(mFeaturedImageProgressOverlay, state.getProgressOverlayVisible());
        if (!state.getLocalImageViewVisible()) {
            mImageManager.cancelRequestAndClearImageView(mLocalFeaturedImageView);
        }
    }

    interface EditPostSettingsCallback {
        void onEditPostPublishedSettingsClick();
    }
}
