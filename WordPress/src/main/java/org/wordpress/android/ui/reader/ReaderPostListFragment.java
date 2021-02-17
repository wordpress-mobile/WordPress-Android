package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.elevation.ElevationOverlayProvider;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.google.android.material.tabs.TabLayout.Tab;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.datasets.ReaderDatabase;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.datasets.ReaderSearchTable;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.ReaderActionBuilder;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload;
import org.wordpress.android.fluxc.store.AccountStore.AddOrDeleteSubscriptionPayload.SubscriptionAction;
import org.wordpress.android.fluxc.store.AccountStore.OnSubscriptionUpdated;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask;
import org.wordpress.android.fluxc.store.ReaderStore;
import org.wordpress.android.fluxc.store.ReaderStore.OnReaderSitesSearched;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostDiscoverData;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.ViewPagerFragment;
import org.wordpress.android.ui.main.BottomNavController;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.pages.SnackbarMessageHolder;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.quickstart.QuickStartEvent;
import org.wordpress.android.ui.reader.ReaderEvents.TagAdded;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSearchSuggestionRecyclerAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderSiteSearchAdapter.SiteSearchAdapterListener;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.OpenEditorForReblog;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedSavedOnlyLocallyDialog;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowBookmarkedTab;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowNoSitesToReblog;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowReportPost;
import org.wordpress.android.ui.reader.discover.ReaderNavigationEvents.ShowSitePickerForResult;
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter;
import org.wordpress.android.ui.reader.services.post.ReaderPostServiceStarter.UpdateAction;
import org.wordpress.android.ui.reader.services.search.ReaderSearchServiceStarter;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.reader.services.update.TagUpdateClientUtilsProvider;
import org.wordpress.android.ui.reader.subfilter.ActionType.OpenSubsAtPage;
import org.wordpress.android.ui.reader.subfilter.BottomSheetUiState.BottomSheetVisible;
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel;
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site;
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll;
import org.wordpress.android.ui.reader.usecases.ReaderSiteFollowUseCase.FollowSiteState.FollowStatusChanged;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.ui.reader.viewmodels.ReaderModeInfo;
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel;
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.QuickStartUtilsWrapper;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.config.SeenUnseenWithCounterFeatureConfig;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel;
import org.wordpress.android.widgets.AppRatingDialog;
import org.wordpress.android.widgets.RecyclerItemDecoration;
import org.wordpress.android.widgets.WPDialogSnackbar;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.inject.Inject;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST;
import static org.wordpress.android.fluxc.generated.AccountActionBuilder.newUpdateSubscriptionNotificationPostAction;
import static org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType.INTERNAL;

import kotlin.Unit;

public class ReaderPostListFragment extends ViewPagerFragment
        implements ReaderInterfaces.OnPostSelectedListener,
        ReaderInterfaces.OnFollowListener,
        ReaderInterfaces.OnPostListItemButtonListener,
        WPMainActivity.OnActivityBackPressedListener,
        WPMainActivity.OnScrollToTopListener {
    private static final int TAB_POSTS = 0;
    private static final int TAB_SITES = 1;
    private static final int NO_POSITION = -1;

    private ReaderPostAdapter mPostAdapter;
    private ReaderSiteSearchAdapter mSiteSearchAdapter;
    private ReaderSearchSuggestionAdapter mSearchSuggestionAdapter;
    private ReaderSearchSuggestionRecyclerAdapter mSearchSuggestionRecyclerAdapter;

    private FilteredRecyclerView mRecyclerView;
    private boolean mFirstLoad = true;

    private View mNewPostsBar;
    private ActionableEmptyView mActionableEmptyView;
    private ProgressBar mProgress;
    private TabLayout mSearchTabs;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    private View mSubFilterComponent;
    private View mSubFiltersListButton;
    private TextView mSubFilterTitle;
    private View mRemoveFilterButton;

    private boolean mIsTopLevel = false;
    private static final String SUBFILTER_BOTTOM_SHEET_TAG = "SUBFILTER_BOTTOM_SHEET_TAG";

    private BottomNavController mBottomNavController;

    private ReaderTag mCurrentTag;
    private ReaderTag mTagFragmentStartedWith = null;
    private long mCurrentBlogId;
    private long mCurrentFeedId;
    private String mCurrentSearchQuery;
    private ReaderPostListType mPostListType;
    private ReaderSiteModel mLastTappedSiteSearchResult;

    private int mRestorePosition;
    private int mSiteSearchRestorePosition;
    private int mPostSearchAdapterPos;
    private int mSiteSearchAdapterPos;
    private int mSearchTabsPos = NO_POSITION;

    private boolean mIsUpdating;
    private boolean mWasPaused;
    private boolean mHasUpdatedPosts;
    private boolean mIsAnimatingOutNewPostsBar;

    private static boolean mHasPurgedReaderDb;

    private final HistoryStack mTagPreviewHistory = new HistoryStack("tag_preview_history");

    private AlertDialog mBookmarksSavedLocallyDialog;
    private QuickStartEvent mQuickStartEvent;

    private ReaderPostListViewModel mViewModel;
    // This VM is initialized only on the Following tab
    private SubFilterViewModel mSubFilterViewModel;
    private ReaderViewModel mReaderViewModel = null;

    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject AccountStore mAccountStore;
    @Inject ReaderStore mReaderStore;
    @Inject Dispatcher mDispatcher;
    @Inject ImageManager mImageManager;
    @Inject QuickStartStore mQuickStartStore;
    @Inject UiHelpers mUiHelpers;
    @Inject TagUpdateClientUtilsProvider mTagUpdateClientUtilsProvider;
    @Inject QuickStartUtilsWrapper mQuickStartUtilsWrapper;
    @Inject SeenUnseenWithCounterFeatureConfig mSeenUnseenWithCounterFeatureConfig;

    private enum ActionableEmptyViewButtonType {
        DISCOVER,
        FOLLOWED
    }

    private static class HistoryStack extends Stack<String> {
        private final String mKeyName;

        HistoryStack(@SuppressWarnings("SameParameterValue") String keyName) {
            mKeyName = keyName;
        }

        void restoreInstance(Bundle bundle) {
            clear();
            if (bundle.containsKey(mKeyName)) {
                ArrayList<String> history = bundle.getStringArrayList(mKeyName);
                if (history != null) {
                    this.addAll(history);
                }
            }
        }

        void saveInstance(Bundle bundle) {
            if (!isEmpty()) {
                ArrayList<String> history = new ArrayList<>();
                history.addAll(this);
                bundle.putStringArrayList(mKeyName, history);
            }
        }
    }

    /*
     * show posts with a specific tag (either TAG_FOLLOWED or TAG_PREVIEW)
     */
    static ReaderPostListFragment newInstanceForTag(ReaderTag tag, ReaderPostListType listType) {
        return newInstanceForTag(tag, listType, false);
    }

    static ReaderPostListFragment newInstanceForTag(
            ReaderTag tag,
            ReaderPostListType listType,
            boolean isTopLevel
    ) {
        AppLog.d(T.READER, "reader post list > newInstance (tag)");

        Bundle args = new Bundle();
        // Tag this fragment is started with
        args.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, tag);
        // Tag this fragment is started with but also used for savedState
        args.putSerializable(ReaderConstants.ARG_TAG, tag);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, listType);
        args.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, isTopLevel);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);
        fragment.trackTagLoaded(tag);

        return fragment;
    }

    static ReaderPostListFragment newInstanceForSearch() {
        AppLog.d(T.READER, "reader post list > newInstance (search)");

        Bundle args = new Bundle();
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.SEARCH_RESULTS);
        args.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, false);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /*
     * show posts in a specific blog
     */
    public static ReaderPostListFragment newInstanceForBlog(long blogId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_BLOG_ID, blogId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static ReaderPostListFragment newInstanceForFeed(long feedId) {
        AppLog.d(T.READER, "reader post list > newInstance (blog)");

        Bundle args = new Bundle();
        args.putLong(ReaderConstants.ARG_FEED_ID, feedId);
        args.putLong(ReaderConstants.ARG_BLOG_ID, feedId);
        args.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);

        ReaderPostListFragment fragment = new ReaderPostListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public @Nullable SiteModel getSelectedSite() {
        if (getActivity() instanceof WPMainActivity) {
            WPMainActivity mainActivity = (WPMainActivity) getActivity();
            return mainActivity.getSelectedSite();
        }
        return null;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            if (args.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) args.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (args.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
                mTagFragmentStartedWith = (ReaderTag) args.getSerializable(ReaderConstants.ARG_ORIGINAL_TAG);
            }
            if (args.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderPostListType) args.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }

            if (args.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
                mIsTopLevel = args.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL);
            }

            mCurrentBlogId = args.getLong(ReaderConstants.ARG_BLOG_ID);
            mCurrentFeedId = args.getLong(ReaderConstants.ARG_FEED_ID);
            mCurrentSearchQuery = args.getString(ReaderConstants.ARG_SEARCH_QUERY);

            if (getPostListType() == ReaderPostListType.TAG_PREVIEW && hasCurrentTag()) {
                mTagPreviewHistory.push(getCurrentTagName());
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            AppLog.d(T.READER, "reader post list > restoring instance state");
            if (savedInstanceState.containsKey(ReaderConstants.ARG_TAG)) {
                mCurrentTag = (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_TAG);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_BLOG_ID)) {
                mCurrentBlogId = savedInstanceState.getLong(ReaderConstants.ARG_BLOG_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_FEED_ID)) {
                mCurrentFeedId = savedInstanceState.getLong(ReaderConstants.ARG_FEED_ID);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_SEARCH_QUERY)) {
                mCurrentSearchQuery = savedInstanceState.getString(ReaderConstants.ARG_SEARCH_QUERY);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType =
                        (ReaderPostListType) savedInstanceState.getSerializable(ReaderConstants.ARG_POST_LIST_TYPE);
            }
            if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                mTagPreviewHistory.restoreInstance(savedInstanceState);
            }
            if (savedInstanceState.containsKey(ReaderConstants.ARG_IS_TOP_LEVEL)) {
                mIsTopLevel = savedInstanceState.getBoolean(ReaderConstants.ARG_IS_TOP_LEVEL);
            }

            if (savedInstanceState.containsKey(ReaderConstants.ARG_ORIGINAL_TAG)) {
                mTagFragmentStartedWith =
                        (ReaderTag) savedInstanceState.getSerializable(ReaderConstants.ARG_ORIGINAL_TAG);
            }

            mRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_RESTORE_POSITION);
            mSiteSearchRestorePosition = savedInstanceState.getInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION);
            mWasPaused = savedInstanceState.getBoolean(ReaderConstants.KEY_WAS_PAUSED);
            mHasUpdatedPosts = savedInstanceState.getBoolean(ReaderConstants.KEY_ALREADY_UPDATED);
            mFirstLoad = savedInstanceState.getBoolean(ReaderConstants.KEY_FIRST_LOAD);
            mSearchTabsPos = savedInstanceState.getInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, NO_POSITION);
            mQuickStartEvent = savedInstanceState.getParcelable(QuickStartEvent.KEY);
        }
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                                       .get(ReaderPostListViewModel.class);
        if (mIsTopLevel) {
            mReaderViewModel = new ViewModelProvider(getParentFragment(), mViewModelFactory)
                                                 .get(ReaderViewModel.class);
        }

        if (isFilterableScreen()) {
            initSubFilterViewModel(savedInstanceState);
        }

        mViewModel.getNavigationEvents().observe(getViewLifecycleOwner(),
                event -> event.applyIfNotHandled(navTarget -> {
                    if (navTarget instanceof ShowSitePickerForResult) {
                        ShowSitePickerForResult data = (ShowSitePickerForResult) navTarget;
                        ActivityLauncher.showSitePickerForResult(
                                ReaderPostListFragment.this,
                                data.getPreselectedSite(),
                                data.getMode()
                        );
                    } else if (navTarget instanceof OpenEditorForReblog) {
                        OpenEditorForReblog data = (OpenEditorForReblog) navTarget;
                        ActivityLauncher.openEditorForReblog(
                                getActivity(),
                                data.getSite(),
                                data.getPost(),
                                data.getSource()
                        );
                    } else if (navTarget instanceof ShowNoSitesToReblog) {
                        ReaderActivityLauncher.showNoSiteToReblog(getActivity());
                    } else if (navTarget instanceof ShowBookmarkedTab) {
                        ActivityLauncher.viewSavedPostsListInReader(getActivity());
                    } else if (navTarget instanceof ShowBookmarkedSavedOnlyLocallyDialog) {
                        showBookmarksSavedLocallyDialog((ShowBookmarkedSavedOnlyLocallyDialog) navTarget);
                    } else if (navTarget instanceof ShowReportPost) {
                        ShowReportPost data = (ShowReportPost) navTarget;
                        ReaderActivityLauncher.openUrl(
                            getContext(),
                            ReaderUtils.getReportPostUrl(data.getUrl()),
                            INTERNAL);
                    } else {
                        throw new IllegalStateException("Action not supported in ReaderPostListFragment " + navTarget);
                    }
                    return Unit.INSTANCE;
                }));

        mViewModel.getSnackbarEvents().observe(getViewLifecycleOwner(), event ->
                event.applyIfNotHandled(holder -> {
                    showSnackbar(holder);
                    return Unit.INSTANCE;
                })
        );

        mViewModel.getPreloadPostEvents().observe(getViewLifecycleOwner(), event ->
                event.applyIfNotHandled(holder -> {
                    addWebViewCachingFragment(holder.getBlogId(), holder.getPostId());
                    return Unit.INSTANCE;
                })
        );

        mViewModel.getRefreshPosts().observe(getViewLifecycleOwner(), event ->
                event.applyIfNotHandled(holder -> {
                    refreshPosts();
                    return Unit.INSTANCE;
                })
        );

        mViewModel.getUpdateFollowStatus().observe(getViewLifecycleOwner(), this::setFollowStatusForBlog);

        mViewModel.start(mReaderViewModel);

        if (isFollowingScreen()) {
            mSubFilterViewModel.onUserComesToReader();
        }

        if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
            mRecyclerView.showAppBarLayout();
            mSearchMenuItem.expandActionView();
            mRecyclerView.setToolbarScrollFlags(0);
        }
    }

    private void setFollowStatusForBlog(FollowStatusChanged readerData) {
        if (!hasPostAdapter()) {
            return;
        }
        getPostAdapter().setFollowStatusForBlog(readerData.getBlogId(), readerData.getFollowing());
    }

    private void showSnackbar(SnackbarMessageHolder holder) {
        Snackbar snackbar = WPSnackbar.make(
                getSnackbarParent(),
                mUiHelpers.getTextOfUiString(requireContext(), holder.getMessage()),
                Snackbar.LENGTH_LONG
        );
        if (holder.getButtonTitle() != null) {
            snackbar.setAction(
                    mUiHelpers.getTextOfUiString(requireContext(), holder.getButtonTitle()),
                    v -> holder.getButtonAction().invoke()
            );
        }
        snackbar.show();
    }

    private void addWebViewCachingFragment(Long blogId, Long postId) {
        String tag = blogId + "" + postId;

        if (getParentFragmentManager().findFragmentByTag(tag) == null) {
            getParentFragmentManager().beginTransaction()
                                 .add(ReaderPostWebViewCachingFragment.newInstance(blogId, postId), tag)
                                 .commit();
        }
    }

    private void initSubFilterViewModel(@Nullable Bundle savedInstanceState) {
        WPMainActivityViewModel wpMainActivityViewModel = new ViewModelProvider(requireActivity(), mViewModelFactory)
                                                     .get(WPMainActivityViewModel.class);
        mSubFilterViewModel = new ViewModelProvider(this, mViewModelFactory).get(
                SubFilterViewModel.SUBFILTER_VM_BASE_KEY + mTagFragmentStartedWith.getKeyString(),
                SubFilterViewModel.class
        );

        mSubFilterViewModel.getCurrentSubFilter().observe(getViewLifecycleOwner(), subfilterListItem -> {
            if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                mSubFilterViewModel.onSubfilterSelected(subfilterListItem);
                if (shouldShowEmptyViewForSelfHostedCta()) {
                    setEmptyTitleDescriptionAndButton(false);
                    showEmptyView();
                }
            }
        });

        mSubFilterViewModel.getReaderModeInfo().observe(getViewLifecycleOwner(), readerModeInfo -> {
            if (readerModeInfo != null) {
                changeReaderMode(readerModeInfo, true);

                if (readerModeInfo.getLabel() != null) {
                    mSubFilterTitle.setText(
                            mUiHelpers.getTextOfUiString(
                                    requireActivity(),
                                    readerModeInfo.getLabel()
                            )
                    );
                }

                if (readerModeInfo.isFiltered()) {
                    mRemoveFilterButton.setVisibility(View.VISIBLE);
                } else {
                    mRemoveFilterButton.setVisibility(View.GONE);
                }
            }
        });

        mSubFilterViewModel.getBottomSheetUiState().observe(getViewLifecycleOwner(), event -> {
            event.applyIfNotHandled(uiState -> {
                FragmentManager fm = getChildFragmentManager();
                if (fm != null) {
                    SubfilterBottomSheetFragment bottomSheet =
                            (SubfilterBottomSheetFragment) fm.findFragmentByTag(SUBFILTER_BOTTOM_SHEET_TAG);
                    if (uiState.isVisible() && bottomSheet == null) {
                        mSubFilterViewModel.loadSubFilters();
                        BottomSheetVisible visibleState = (BottomSheetVisible) uiState;
                        bottomSheet = SubfilterBottomSheetFragment.newInstance(
                                SubFilterViewModel.SUBFILTER_VM_BASE_KEY + mTagFragmentStartedWith.getKeyString(),
                                visibleState.getCategories(),
                                mUiHelpers.getTextOfUiString(requireContext(), visibleState.getTitle())
                        );
                        bottomSheet.show(getChildFragmentManager(), SUBFILTER_BOTTOM_SHEET_TAG);
                    } else if (!uiState.isVisible() && bottomSheet != null) {
                        bottomSheet.dismiss();
                    }
                }
                return null;
            });
        });

        mSubFilterViewModel.getBottomSheetEmptyViewAction().observe(getViewLifecycleOwner(), event -> {
            event.applyIfNotHandled(action -> {
                if (action instanceof OpenSubsAtPage) {
                    ReaderActivityLauncher.showReaderSubs(
                            requireActivity(),
                            ((OpenSubsAtPage) action).getTabIndex()
                    );
                } else {
                    wpMainActivityViewModel.onOpenLoginPage();
                }

                return null;
            });
        });

        mSubFilterViewModel.getUpdateTagsAndSites().observe(getViewLifecycleOwner(), event -> {
            event.applyIfNotHandled(tasks -> {
                if (NetworkUtils.isNetworkAvailable(getActivity())) {
                    ReaderUpdateServiceStarter.startService(getActivity(), tasks);
                }
                return null;
            });
        });

        mSubFilterViewModel.start(mTagFragmentStartedWith, mCurrentTag, savedInstanceState);
    }

    private void initSubFilterViews(ViewGroup rootView, LayoutInflater inflater) {
        mSubFilterComponent = inflater.inflate(R.layout.subfilter_component, rootView, false);
        ((ViewGroup) rootView.findViewById(R.id.sub_filter_component_container)).addView(mSubFilterComponent);

        mSubFiltersListButton = mSubFilterComponent.findViewById(R.id.filter_selection);
        mSubFiltersListButton.setOnClickListener(v -> {
            mSubFilterViewModel.onSubFiltersListButtonClicked();
        });

        mSubFilterTitle = mSubFilterComponent.findViewById(R.id.selected_filter_name);

        mRemoveFilterButton = mSubFilterComponent.findViewById(R.id.remove_filter_button);
        mRemoveFilterButton.setOnClickListener(v -> {
            mSubFilterViewModel.setDefaultSubfilter();
        });
        mSubFilterComponent.setVisibility(isFilterableScreen() ? View.VISIBLE : View.GONE);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(mRecyclerView.getContext());
        float cardElevation = getResources().getDimension(R.dimen.card_elevation);
        int elevatedCardColor = elevationOverlayProvider.compositeOverlayWithThemeSurfaceColorIfNeeded(cardElevation);
        mSubFilterComponent.setBackgroundColor(elevatedCardColor);
    }

    private void changeReaderMode(ReaderModeInfo readerModeInfo, boolean onlyOnChanges) {
        boolean changesDetected = false;

        if (onlyOnChanges) {
            changesDetected = (readerModeInfo.getTag() != null
                               && mCurrentTag != null
                               && !readerModeInfo.getTag().equals(mCurrentTag))
                              || (mPostListType != readerModeInfo.getListType())
                              || (mCurrentBlogId != readerModeInfo.getBlogId())
                              || (mCurrentFeedId != readerModeInfo.getFeedId())
                              || (readerModeInfo.isFirstLoad());

            if (changesDetected && !readerModeInfo.isFirstLoad()) {
                trackTagLoaded(readerModeInfo.getTag());
            }
        }

        if (onlyOnChanges && !changesDetected) return;

        if (readerModeInfo.getTag() != null) {
            mCurrentTag = readerModeInfo.getTag();
        }

        mPostListType = readerModeInfo.getListType();
        mCurrentBlogId = readerModeInfo.getBlogId();
        mCurrentFeedId = readerModeInfo.getFeedId();

        resetPostAdapter(mPostListType);
        if (readerModeInfo.getRequestNewerPosts()) {
            updatePosts(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mBookmarksSavedLocallyDialog != null) {
            mBookmarksSavedLocallyDialog.dismiss();
        }
        mWasPaused = true;

        mViewModel.onFragmentPause(mIsTopLevel, getPostListType() == ReaderPostListType.SEARCH_RESULTS,
                isFilterableScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
         * This is a workaround for https://github.com/wordpress-mobile/WordPress-Android/issues/11985.
         * The RecyclerView doesn't get redrawn correctly when the adapter finishes its initialization in onStart.
         */
        getPostAdapter().notifyDataSetChanged();
        if (mWasPaused) {
            AppLog.d(T.READER, "reader post list > resumed from paused state");
            mWasPaused = false;

            Site currentSite;

            if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
                resumeFollowedTag();
            } else if ((currentSite = getSiteIfBlogPreview()) != null) {
                resumeFollowedSite(currentSite);
            } else {
                refreshPosts();
            }

            if (mIsTopLevel) {
                // Remove sticky event if not consumed
                EventBus.getDefault().removeStickyEvent(ReaderEvents.TagAdded.class);
            }

            // if the user tapped a site to show site preview, it's possible they also changed the follow
            // status so tell the search adapter to check whether it has the correct follow status
            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS && mLastTappedSiteSearchResult != null) {
                getSiteSearchAdapter().checkFollowStatusForSite(mLastTappedSiteSearchResult);
                mLastTappedSiteSearchResult = null;
            }

            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                return;
            }
        }

        if (shouldShowEmptyViewForSelfHostedCta()) {
            setEmptyTitleDescriptionAndButton(false);
            showEmptyView();
        }

        mViewModel.onFragmentResume(mIsTopLevel, getPostListType() == ReaderPostListType.SEARCH_RESULTS,
                isFilterableScreen(), isFilterableScreen() ? mSubFilterViewModel.getCurrentSubfilterValue() : null);
    }

    /*
     * called when fragment is resumed and we're looking at posts in a followed tag
     */
    private void resumeFollowedTag() {
        TagAdded addedTag = EventBus.getDefault().getStickyEvent(ReaderEvents.TagAdded.class);
        if (isFollowingScreen() && addedTag != null) {
            EventBus.getDefault().removeStickyEvent(addedTag);
            // user just added a tag so switch to it.
            ReaderTag newTag = ReaderUtils.getTagFromTagName(addedTag.getTagName(), ReaderTagType.FOLLOWED);
            mSubFilterViewModel.setSubfilterFromTag(newTag);
        } else if (isFollowingScreen() && !ReaderTagTable.tagExists(getCurrentTag())) {
            // user just removed a tag which was selected in the subfilter
            mSubFilterViewModel.setDefaultSubfilter();
        } else {
            // otherwise, refresh posts to make sure any changes are reflected and auto-update
            // posts in the current tag if it's time
            refreshPosts();
            updateCurrentTagIfTime();
        }
    }

    @Nullable
    private Site getSiteIfBlogPreview() {
        Site currentSite = null;

        if (isFilterableScreen() && getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            currentSite = mSubFilterViewModel.getCurrentSubfilterValue() instanceof Site ? (Site) (mSubFilterViewModel
                    .getCurrentSubfilterValue()) : null;
        }

        return currentSite;
    }

    private void resumeFollowedSite(Site currentSite) {
        ReaderBlog blog = currentSite.getBlog();
        boolean isSiteStillAvailable = false;

        if (blog != null) {
            if ((blog.hasFeedUrl() && ReaderBlogTable.isFollowedFeed(blog.feedId))
                || ReaderBlogTable.isFollowedBlog(blog.blogId)) {
                isSiteStillAvailable = true;
            }
        }

        if (isSiteStillAvailable) {
            refreshPosts();
        } else {
            if (isFilterableScreen()) {
                mSubFilterViewModel.setDefaultSubfilter();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // detect the bottom nav controller when this fragment is hosted in the main activity - this is used to
        // hide the bottom nav when the user searches from the reader
        if (context instanceof BottomNavController) {
            mBottomNavController = (BottomNavController) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBottomNavController = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        mDispatcher.register(this);
        EventBus.getDefault().register(this);

        reloadTags();

        // purge database and update followed tags/blog if necessary - note that we don't purge unless
        // there's a connection to avoid removing posts the user would expect to see offline
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && NetworkUtils.isNetworkAvailable(getActivity())) {
            purgeDatabaseIfNeeded();
        }

        checkPostAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
        mNewPostsBar.clearAnimation();
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
    }

    /*
     * ensures the adapter is created and posts are updated if they haven't already been
     */
    private void checkPostAdapter() {
        if (isAdded() && mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(getPostAdapter());
            refreshPosts();
            if (!mHasUpdatedPosts && NetworkUtils.isNetworkAvailable(getActivity())) {
                mHasUpdatedPosts = true;
                if (getPostListType().isTagType()) {
                    updateCurrentTagIfTime();
                } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                    updatePostsInCurrentBlogOrFeed(ReaderPostServiceStarter.UpdateAction.REQUEST_NEWER);
                }
            }
        }
    }

    /*
     * reset the post adapter to initial state and create it again using the passed list type
     */
    private void resetPostAdapter(ReaderPostListType postListType) {
        mPostListType = postListType;
        mPostAdapter = null;
        mRecyclerView.setAdapter(null);
        mRecyclerView.setAdapter(getPostAdapter());
        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.FollowedTagsChanged event) {
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            // reload the tag filter since tags have changed
            reloadTags();

            // update the current tag if the list fragment is empty - this will happen if
            // the tag table was previously empty (ie: first run)
            if (isPostAdapterEmpty()) {
                updateCurrentTag();
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.FollowedBlogsChanged event) {
        // refresh posts if user is viewing "Followed Sites"
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED
            && hasCurrentTag()
            && (getCurrentTag().isFollowedSites() || getCurrentTag().isDefaultInMemoryTag())) {
            refreshPosts();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(final QuickStartEvent event) {
        if (!isAdded() || getView() == null) {
            return;
        }

        mQuickStartEvent = event;
        EventBus.getDefault().removeStickyEvent(event);

        if (mQuickStartEvent.getTask() == QuickStartTask.FOLLOW_SITE
            && isAdded() && getActivity() instanceof WPMainActivity) {
            Spannable title = mQuickStartUtilsWrapper.stylizeQuickStartPrompt(
                    requireContext(),
                    R.string.quick_start_dialog_follow_sites_message_short_search,
                    R.drawable.ic_search_white_24dp);

            WPDialogSnackbar snackbar = WPDialogSnackbar.make(getSnackbarParent(), title,
                    getResources().getInteger(R.integer.quick_start_snackbar_duration_ms));

            ((WPMainActivity) getActivity()).showQuickStartSnackBar(snackbar);

            if (getSelectedSite() != null) {
                QuickStartUtils.completeTaskAndRemindNextOne(mQuickStartStore, QuickStartTask.FOLLOW_SITE,
                        mDispatcher, getSelectedSite(), mQuickStartEvent, getContext());
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        AppLog.d(T.READER, "reader post list > saving instance state");

        if (mCurrentTag != null) {
            outState.putSerializable(ReaderConstants.ARG_TAG, mCurrentTag);
        }

        if (mTagFragmentStartedWith != null) {
            outState.putSerializable(ReaderConstants.ARG_ORIGINAL_TAG, mTagFragmentStartedWith);
        }

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            mTagPreviewHistory.saveInstance(outState);
        } else if (getPostListType() == ReaderPostListType.SEARCH_RESULTS
                   && mSearchView != null
                   && mSearchView.getQuery() != null) {
            String query = mSearchView.getQuery().toString();
            outState.putString(ReaderConstants.ARG_SEARCH_QUERY, query);
        }

        outState.putLong(ReaderConstants.ARG_BLOG_ID, mCurrentBlogId);
        outState.putLong(ReaderConstants.ARG_FEED_ID, mCurrentFeedId);
        outState.putBoolean(ReaderConstants.KEY_WAS_PAUSED, mWasPaused);
        outState.putBoolean(ReaderConstants.KEY_ALREADY_UPDATED, mHasUpdatedPosts);
        outState.putBoolean(ReaderConstants.KEY_FIRST_LOAD, mFirstLoad);
        if (mRecyclerView != null) {
            outState.putBoolean(ReaderConstants.KEY_IS_REFRESHING, mRecyclerView.isRefreshing());
            outState.putInt(ReaderConstants.KEY_RESTORE_POSITION, getCurrentPosition());
        }
        outState.putSerializable(ReaderConstants.ARG_POST_LIST_TYPE, getPostListType());
        outState.putBoolean(ReaderConstants.ARG_IS_TOP_LEVEL, mIsTopLevel);
        outState.putParcelable(QuickStartEvent.KEY, mQuickStartEvent);

        if (isSearchTabsShowing()) {
            int tabPosition = getSearchTabsPosition();
            outState.putInt(ReaderConstants.KEY_ACTIVE_SEARCH_TAB, tabPosition);
            int siteSearchPosition = tabPosition == TAB_SITES ? getCurrentPosition() : mSiteSearchAdapterPos;
            outState.putInt(ReaderConstants.KEY_SITE_SEARCH_RESTORE_POSITION, siteSearchPosition);
        }

        if (isFilterableTag(mTagFragmentStartedWith) && mSubFilterViewModel != null) {
            mSubFilterViewModel.onSaveInstanceState(outState);
        }

        super.onSaveInstanceState(outState);
    }

    private int getCurrentPosition() {
        if (mRecyclerView != null && hasPostAdapter()) {
            return mRecyclerView.getCurrentPosition();
        } else {
            return -1;
        }
    }

    private void updatePosts(boolean forced) {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mRecyclerView.setRefreshing(false);
            return;
        }

        if (forced) {
            // Update the tags on post refresh since following some sites (like P2) will change followed tags and blogs
            ReaderUpdateServiceStarter.startService(getContext(),
                    EnumSet.of(UpdateTask.TAGS, UpdateTask.FOLLOWED_BLOGS));
        }

        if (mFirstLoad) {
            // let onResume() take care of this logic, as the FilteredRecyclerView.FilterListener onLoadData
            // method is called on two moments: once for first time load, and then each time the swipe to
            // refresh gesture triggers a refresh.
            mRecyclerView.setRefreshing(false);
            mFirstLoad = false;
        } else {
            UpdateAction updateAction = forced ? UpdateAction.REQUEST_REFRESH
                    : UpdateAction.REQUEST_NEWER;
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                    // fall through to TAG_PREVIEW
                case TAG_PREVIEW:
                    updatePostsWithTag(getCurrentTag(), updateAction);
                    break;
                case BLOG_PREVIEW:
                    updatePostsInCurrentBlogOrFeed(updateAction);
                    break;
                case SEARCH_RESULTS:
                    // no-op
                    break;
            }
            // make sure swipe-to-refresh progress shows since this is a manual refresh
            mRecyclerView.setRefreshing(true);
        }
        if (getCurrentTag() != null && getCurrentTag().isBookmarked()) {
            ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();
            refreshPosts();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.reader_fragment_post_cards, container, false);
        mRecyclerView = rootView.findViewById(R.id.reader_recycler_view);

        mActionableEmptyView = rootView.findViewById(R.id.empty_custom_view);

        mRecyclerView.setLogT(AppLog.T.READER);
        mRecyclerView.setCustomEmptyView();
        mRecyclerView.setFilterListener(new FilteredRecyclerView.FilterListener() {
            @Override
            public List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh) {
                return null;
            }

            @Override
            public void onLoadFilterCriteriaOptionsAsync(
                    FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener, boolean refresh) {
            }

            @Override
            public void onLoadData(boolean forced) {
                if (forced) {
                    AnalyticsTracker.track(Stat.READER_PULL_TO_REFRESH);
                }
                updatePosts(forced);
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                onTagChanged((ReaderTag) criteria);
            }

            @Override
            public FilterCriteria onRecallSelection() {
                if (hasCurrentTag()) {
                    ReaderTag defaultTag;

                    defaultTag = ReaderUtils.getDefaultTagFromDbOrCreateInMemory(
                            requireActivity(),
                            mTagUpdateClientUtilsProvider
                    );

                    ReaderTag tag = ReaderUtils.getValidTagForSharedPrefs(
                            getCurrentTag(),
                            mIsTopLevel,
                            mRecyclerView,
                            defaultTag);

                    return tag;
                } else {
                    AppLog.w(T.READER, "reader post list > no current tag in onRecallSelection");
                    return ReaderUtils.getDefaultTag();
                }
            }

            @Override
            public String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType) {
                return null;
            }

            @Override
            public void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType) {
                setEmptyTitleDescriptionAndButton(
                        EmptyViewMessageType.NETWORK_ERROR.equals(emptyViewMsgType)
                        || EmptyViewMessageType.PERMISSION_ERROR.equals(emptyViewMsgType)
                        || EmptyViewMessageType.GENERIC_ERROR.equals(emptyViewMsgType));
            }
        });

        // add the item decoration (dividers) to the recycler, skipping the first item if the first
        // item is the tag toolbar (shown when viewing posts in followed tags) - this is to avoid
        // having the tag toolbar take up more vertical space than necessary
        int spacingHorizontal = getResources().getDimensionPixelSize(R.dimen.reader_card_margin);
        int spacingVertical = getResources().getDimensionPixelSize(R.dimen.reader_card_gutters);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical, false));

        mRecyclerView.setToolbarBackgroundColor(0);
        mRecyclerView.setToolbarSpinnerDrawable(R.drawable.ic_dropdown_primary_30_24dp);

        if (mIsTopLevel) {
            mRecyclerView.setToolbarTitle(
                    R.string.reader_screen_title,
                    getResources().getDimensionPixelSize(R.dimen.margin_extra_large)
            );
        } else {
            mRecyclerView.setToolbarLeftAndRightPadding(
                    getResources().getDimensionPixelSize(R.dimen.margin_medium),
                    getResources().getDimensionPixelSize(R.dimen.margin_extra_large));
        }

        // add a menu to the filtered recycler's toolbar
        if (mAccountStore.hasAccessToken() && getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
            setupRecyclerToolbar();
        }

        mRecyclerView.setSwipeToRefreshEnabled(isSwipeToRefreshSupported());

        // bar that appears at top after new posts are loaded
        mNewPostsBar = rootView.findViewById(R.id.layout_new_posts);
        mNewPostsBar.setVisibility(View.GONE);
        mNewPostsBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.scrollRecycleViewToPosition(0);
                refreshPosts();
            }
        });

        // progress bar that appears when loading more posts
        mProgress = rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        if (savedInstanceState != null && savedInstanceState.getBoolean(ReaderConstants.KEY_IS_REFRESHING)) {
            mIsUpdating = true;
            mRecyclerView.setRefreshing(true);
        }



        if (isFilterableScreen()) {
            initSubFilterViews(rootView, inflater);
        }

        return rootView;
    }

    /*
     * adds a menu to the recycler's toolbar containing search items - only called
     * for followed tags
     */
    private void setupRecyclerToolbar() {
        Menu menu = mRecyclerView.addToolbarMenu(R.menu.reader_list);
        mSearchMenuItem = menu.findItem(R.id.menu_reader_search);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setQueryHint(getString(R.string.reader_hint_post_search));
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setIconified(true);

        // force the search view to take up as much horizontal space as possible (without this
        // it looks truncated on landscape)
        int maxWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
        mSearchView.setMaxWidth(maxWidth);

        // this is hacky, but we want to change the SearchView's autocomplete to show suggestions
        // after a single character is typed, and there's no less hacky way to do this...
        View view = mSearchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (view instanceof AutoCompleteTextView) {
            ((AutoCompleteTextView) view).setThreshold(1);
        }

        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SEARCH_LOADED);
                }
                resetPostAdapter(ReaderPostListType.SEARCH_RESULTS);
                populateSearchSuggestions(null);
                showSearchMessageOrSuggestions();
                // hide the bottom navigation when search is active
                if (mBottomNavController != null) {
                    mBottomNavController.onRequestHideBottomNavigation();
                }

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                requireActivity().finish();
                return false;
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                                               @Override
                                               public boolean onQueryTextSubmit(String query) {
                                                   submitSearchQuery(query);
                                                   return true;
                                               }

                                               @Override
                                               public boolean onQueryTextChange(String newText) {
                                                   populateSearchSuggestions(newText);
                                                   showSearchMessageOrSuggestions();
                                                   return true;
                                               }
                                           }
        );
    }

    private void showSearchMessageOrSuggestions() {
        boolean hasQuery = !isSearchViewEmpty();
        boolean hasPerformedSearch = !TextUtils.isEmpty(mCurrentSearchQuery);
        boolean isSearching = getPostListType() == ReaderPostListType.SEARCH_RESULTS;

        // prevents suggestions from being shown after the search view has been collapsed
        if (!isSearching) {
            return;
        }

        // prevents suggestions from being shown above search results after configuration changes
        if (mWasPaused && hasPerformedSearch) {
            return;
        }

        if (!hasQuery || !hasPerformedSearch) {
            // clear posts and sites so only the suggestions or the empty view are visible
            getPostAdapter().clear();
            getSiteSearchAdapter().clear();

            hideSearchTabs();

            // clears the last performed query
            mCurrentSearchQuery = null;

            final boolean hasSuggestions =
                    mSearchSuggestionRecyclerAdapter != null && mSearchSuggestionRecyclerAdapter.getItemCount() > 0;

            if (hasSuggestions) {
                hideSearchMessage();
                showSearchSuggestions();
            } else {
                showSearchMessage();
                hideSearchSuggestions();
            }
        }
    }

    /*
     * start the search service to search for posts matching the current query - the passed
     * offset is used during infinite scroll, pass zero for initial search
     */
    private void updatePostsInCurrentSearch(int offset) {
        ReaderSearchServiceStarter.startService(getActivity(), mCurrentSearchQuery, offset);
    }

    /*
     * start a search for reader sites matching the current search query
     */
    private void updateSitesInCurrentSearch(int offset) {
        if (getSearchTabsPosition() == TAB_SITES) {
            if (offset == 0) {
                mRecyclerView.setRefreshing(true);
            } else {
                showLoadingProgress(true);
            }
        }
        ReaderSearchSitesPayload payload = new ReaderSearchSitesPayload(
                mCurrentSearchQuery,
                ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST,
                offset,
                false);
        mDispatcher.dispatch(ReaderActionBuilder.newReaderSearchSitesAction(payload));
    }

    private void submitSearchQuery(@NonNull String query) {
        if (!isAdded()) {
            return;
        }

        mSearchView.clearFocus(); // this will hide suggestions and the virtual keyboard
        hideSearchMessage();
        hideSearchSuggestions();

        // remember this query for future suggestions
        String trimQuery = query.trim();
        ReaderSearchTable.addOrUpdateQueryString(trimQuery);

        // remove cached results for this search - search results are ephemeral so each search
        // should be treated as a "fresh" one
        ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(trimQuery);
        ReaderPostTable.deletePostsWithTag(searchTag);

        mPostAdapter.setCurrentTag(searchTag);
        mCurrentSearchQuery = trimQuery;
        updatePostsInCurrentSearch(0);
        updateSitesInCurrentSearch(0);

        // track that the user performed a search
        if (!trimQuery.equals("")) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("query", trimQuery);
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SEARCH_PERFORMED, properties);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReaderSitesSearched(OnReaderSitesSearched event) {
        if (!isAdded() || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return;
        }

        if (!isUpdating()) {
            mRecyclerView.setRefreshing(false);
        }
        showLoadingProgress(false);

        ReaderSiteSearchAdapter adapter = getSiteSearchAdapter();
        if (event.isError()) {
            adapter.clear();
        } else if (StringUtils.equals(event.searchTerm, mCurrentSearchQuery)) {
            adapter.setCanLoadMore(event.canLoadMore);
            if (event.offset == 0) {
                adapter.setSiteList(event.sites);
            } else {
                adapter.addSiteList(event.sites);
            }
            if (mSiteSearchRestorePosition > 0) {
                mRecyclerView.scrollRecycleViewToPosition(mSiteSearchRestorePosition);
            }
        }

        if (getSearchTabsPosition() == TAB_SITES && adapter.isEmpty()) {
            setEmptyTitleDescriptionAndButton(event.isError());
            showEmptyView();
        }

        mSiteSearchRestorePosition = 0;
    }

    /*
     * reuse "empty" view to let user know what they're querying
     */
    private void showSearchMessage() {
        if (!isAdded()) {
            return;
        }

        setEmptyTitleDescriptionAndButton(false);
        showEmptyView();
    }

    private void hideSearchMessage() {
        hideEmptyView();
    }

    private void showSearchSuggestions() {
        mRecyclerView.showSearchSuggestions();
    }

    private void hideSearchSuggestions() {
        mRecyclerView.hideSearchSuggestions();
    }

    /*
     * create the TabLayout that separates search results between POSTS and SITES and places it below
     * the FilteredRecyclerView's toolbar
     */
    private void createSearchTabs() {
        if (mSearchTabs == null) {
            ViewGroup rootView = getView().findViewById(android.R.id.content);
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            mSearchTabs = (TabLayout) inflater.inflate(R.layout.reader_search_tabs, rootView);
            mSearchTabs.setVisibility(View.GONE);
            mRecyclerView.getAppBarLayout().addView(mSearchTabs);
        }
    }

    private boolean isSearchTabsShowing() {
        return mSearchTabs != null && mSearchTabs.getVisibility() == View.VISIBLE;
    }

    private void showSearchTabs() {
        if (!isAdded()) {
            return;
        }
        if (mSearchTabs == null) {
            createSearchTabs();
        }
        if (mSearchTabs.getVisibility() != View.VISIBLE) {
            mSearchTabs.setVisibility(View.VISIBLE);

            mPostSearchAdapterPos = 0;
            mSiteSearchAdapterPos = 0;

            mSearchTabs.addOnTabSelectedListener(new OnTabSelectedListener() {
                @Override public void onTabSelected(Tab tab) {
                    if (tab.getPosition() == TAB_POSTS) {
                        mRecyclerView.setAdapter(getPostAdapter());
                        if (mPostSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mPostSearchAdapterPos);
                        }
                        if (getPostAdapter().isEmpty()) {
                            setEmptyTitleDescriptionAndButton(false);
                            showEmptyView();
                        } else {
                            hideEmptyView();
                        }
                    } else if (tab.getPosition() == TAB_SITES) {
                        mRecyclerView.setAdapter(getSiteSearchAdapter());
                        if (mSiteSearchAdapterPos > 0) {
                            mRecyclerView.scrollRecycleViewToPosition(mSiteSearchAdapterPos);
                        }
                        if (getSiteSearchAdapter().isEmpty()) {
                            setEmptyTitleDescriptionAndButton(false);
                            showEmptyView();
                        } else {
                            hideEmptyView();
                        }
                    }
                }

                @Override public void onTabUnselected(Tab tab) {
                    if (tab.getPosition() == TAB_POSTS) {
                        mPostSearchAdapterPos = mRecyclerView.getCurrentPosition();
                    } else if (tab.getPosition() == TAB_SITES) {
                        mSiteSearchAdapterPos = mRecyclerView.getCurrentPosition();
                    }
                }

                @Override public void onTabReselected(Tab tab) {
                    mRecyclerView.smoothScrollToPosition(0);
                }
            });

            if (mSearchTabsPos != NO_POSITION && mSearchTabsPos != mSearchTabs.getSelectedTabPosition()) {
                Tab tab = mSearchTabs.getTabAt(mSearchTabsPos);
                if (tab != null) {
                    tab.select();
                }
                mSearchTabsPos = NO_POSITION;
            }
        }
    }

    private void hideSearchTabs() {
        if (isAdded() && mSearchTabs != null && mSearchTabs.getVisibility() == View.VISIBLE) {
            mSearchTabs.setVisibility(View.GONE);
            mSearchTabs.clearOnTabSelectedListeners();
            if (mSearchTabs.getSelectedTabPosition() != TAB_POSTS) {
                mSearchTabs.getTabAt(TAB_POSTS).select();
            }
            mRecyclerView.setAdapter(getPostAdapter());
            mLastTappedSiteSearchResult = null;
            showLoadingProgress(false);
        }
    }

    private int getSearchTabsPosition() {
        return isSearchTabsShowing() ? mSearchTabs.getSelectedTabPosition() : -1;
    }

    private void populateSearchSuggestions(String query) {
        populateSearchSuggestionAdapter(query);
        populateSearchSuggestionRecyclerAdapter(null); // always passing null as there's no need to filter
    }

    /*
     * create and assign the suggestion adapter for the search view
     */
    private void createSearchSuggestionAdapter() {
        mSearchSuggestionAdapter = new ReaderSearchSuggestionAdapter(getActivity());
        mSearchView.setSuggestionsAdapter(mSearchSuggestionAdapter);

        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                return false;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String query = mSearchSuggestionAdapter.getSuggestion(position);
                onSearchSuggestionClicked(query);
                return true;
            }
        });

        mSearchSuggestionAdapter.setOnSuggestionDeleteClickListener(this::onSearchSuggestionDeleteClicked);
        mSearchSuggestionAdapter.setOnSuggestionClearClickListener(this::onSearchSuggestionClearClicked);
    }

    private void populateSearchSuggestionAdapter(String query) {
        if (mSearchSuggestionAdapter == null) {
            createSearchSuggestionAdapter();
        }
        mSearchSuggestionAdapter.setFilter(query);
    }

    private void resetSearchSuggestionAdapter() {
        mSearchView.setSuggestionsAdapter(null);
        mSearchSuggestionAdapter = null;
    }

    private void createSearchSuggestionRecyclerAdapter() {
        mSearchSuggestionRecyclerAdapter = new ReaderSearchSuggestionRecyclerAdapter();
        mRecyclerView.setSearchSuggestionAdapter(mSearchSuggestionRecyclerAdapter);

        mSearchSuggestionRecyclerAdapter.setOnSuggestionClickListener(this::onSearchSuggestionClicked);
        mSearchSuggestionRecyclerAdapter.setOnSuggestionDeleteClickListener(this::onSearchSuggestionDeleteClicked);
        mSearchSuggestionRecyclerAdapter.setOnSuggestionClearClickListener(this::onSearchSuggestionClearClicked);
    }

    private void populateSearchSuggestionRecyclerAdapter(String query) {
        if (mSearchSuggestionRecyclerAdapter == null) {
            createSearchSuggestionRecyclerAdapter();
        }
        mSearchSuggestionRecyclerAdapter.setQuery(query);
    }

    private void resetSearchSuggestionRecyclerAdapter() {
        mRecyclerView.setSearchSuggestionAdapter(null);
        mSearchSuggestionRecyclerAdapter = null;
    }

    private void onSearchSuggestionClicked(String query) {
        if (!TextUtils.isEmpty(query)) {
            mSearchView.setQuery(query, true);
        }
    }

    private void onSearchSuggestionDeleteClicked(String query) {
        ReaderSearchTable.deleteQueryString(query);

        mSearchSuggestionAdapter.reload();
        mSearchSuggestionRecyclerAdapter.reload();

        showSearchMessageOrSuggestions();
    }

    private void onSearchSuggestionClearClicked() {
        showClearSearchSuggestionsConfirmationDialog(getContext());
    }

    private void showClearSearchSuggestionsConfirmationDialog(final Context context) {
        new MaterialAlertDialogBuilder(context)
                .setMessage(R.string.dlg_confirm_clear_search_history)
                .setCancelable(true)
                .setNegativeButton(R.string.no, null)
                .setPositiveButton(R.string.yes, (dialog, id) -> clearSearchSuggestions())
                .create()
                .show();
    }

    private void clearSearchSuggestions() {
        ReaderSearchTable.deleteAllQueries();

        mSearchSuggestionAdapter.swapCursor(null);
        mSearchSuggestionRecyclerAdapter.swapCursor(null);

        showSearchMessageOrSuggestions();
    }

    /*
     * is the search input showing?
     */
    private boolean isSearchViewExpanded() {
        return mSearchView != null && !mSearchView.isIconified();
    }

    private boolean isSearchViewEmpty() {
        return mSearchView != null && mSearchView.getQuery().length() == 0;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.SearchPostsStarted event) {
        if (!isAdded() || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return;
        }

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(true, updateAction);
        setEmptyTitleDescriptionAndButton(false);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.SearchPostsEnded event) {
        if (!isAdded() || getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            return;
        }

        UpdateAction updateAction = event.getOffset() == 0 ? UpdateAction.REQUEST_NEWER : UpdateAction.REQUEST_OLDER;
        setIsUpdating(false, updateAction);

        // load the results if the search succeeded and it's the current search - note that success
        // means the search didn't fail, not necessarily that is has results - which is fine because
        // if there aren't results then refreshing will show the empty message
        if (event.didSucceed()
            && getPostListType() == ReaderPostListType.SEARCH_RESULTS
            && event.getQuery().equals(mCurrentSearchQuery)) {
            refreshPosts();
            showSearchTabs();
        } else {
            hideSearchTabs();
        }
    }

    /*
     * returns the parent view for snackbars - if this fragment is hosted in the main activity we want the
     * parent to be the main activity's CoordinatorLayout
     */
    private View getSnackbarParent() {
        View coordinator = getActivity().findViewById(R.id.coordinator_layout);
        if (coordinator != null) {
            return coordinator;
        }
        return getView();
    }

    private void setEmptyTitleDescriptionAndButton(boolean requestFailed) {
        if (!isAdded()) {
            return;
        }

        int heightToolbar = getActivity().getResources().getDimensionPixelSize(R.dimen.toolbar_height);
        int heightTabs = getActivity().getResources().getDimensionPixelSize(R.dimen.tab_height);
        mActionableEmptyView.updateLayoutForSearch(false, 0);
        mActionableEmptyView.subtitle.setContentDescription(null);
        boolean isSearching = false;
        String title;
        String description = null;
        ActionableEmptyViewButtonType button = null;

        // Ensure the default image is reset for empty views before applying logic
        mActionableEmptyView.image.setImageResource(R.drawable.img_illustration_empty_results_216dp);

        if (shouldShowEmptyViewForSelfHostedCta()) {
            setEmptyTitleAndDescriptionForSelfHostedCta();
            return;
        } else if (getPostListType() == ReaderPostListType.TAG_FOLLOWED && getCurrentTag().isBookmarked()) {
            setEmptyTitleAndDescriptionForBookmarksList();
            return;
        } else if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            title = getString(R.string.reader_empty_posts_no_connection);
        } else if (requestFailed) {
            if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                title = getString(R.string.reader_empty_search_request_failed);
            } else {
                title = getString(R.string.reader_empty_posts_request_failed);
            }
        } else if (isUpdating() && getPostListType() != ReaderPostListType.SEARCH_RESULTS) {
            title = getString(R.string.reader_empty_posts_in_tag_updating);
        } else {
            switch (getPostListType()) {
                case TAG_FOLLOWED:
                    if (getCurrentTag().isFollowedSites() || getCurrentTag().isDefaultInMemoryTag()) {
                        if (ReaderBlogTable.hasFollowedBlogs()) {
                            title = getString(R.string.reader_empty_followed_blogs_no_recent_posts_title);
                            description = getString(R.string.reader_empty_followed_blogs_no_recent_posts_description);
                        } else {
                            title = getString(R.string.reader_empty_followed_blogs_title);
                            description = getString(R.string.reader_empty_followed_blogs_description);
                        }
                        mActionableEmptyView.image.setImageResource(
                                R.drawable.img_illustration_following_empty_results_196dp);
                        button = ActionableEmptyViewButtonType.DISCOVER;
                    } else if (getCurrentTag().isPostsILike()) {
                        title = getString(R.string.reader_empty_posts_liked_title);
                        description = getString(R.string.reader_empty_posts_liked_description);
                        button = ActionableEmptyViewButtonType.FOLLOWED;
                    } else if (getCurrentTag().isListTopic()) {
                        title = getString(R.string.reader_empty_posts_in_custom_list);
                    } else {
                        title = getString(R.string.reader_empty_posts_in_tag);
                    }
                    break;
                case BLOG_PREVIEW:
                    title = getString(R.string.reader_empty_posts_in_blog);
                    break;
                case SEARCH_RESULTS:
                    isSearching = true;

                    if (isSearchViewEmpty() || TextUtils.isEmpty(mCurrentSearchQuery)) {
                        title = getString(R.string.reader_label_post_search_explainer);
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar);
                    } else if (isUpdating()) {
                        title = "";
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar);
                    } else {
                        title = getString(R.string.reader_empty_search_title);
                        String formattedQuery = "<em>" + mCurrentSearchQuery + "</em>";
                        description = String.format(getString(R.string.reader_empty_search_description),
                                formattedQuery);
                        mActionableEmptyView.updateLayoutForSearch(true, heightToolbar + heightTabs);
                    }
                    break;
                case TAG_PREVIEW:
                    // fall through to the default case
                default:
                    title = getString(R.string.reader_empty_posts_in_tag);
                    break;
            }
        }

        setEmptyTitleDescriptionAndButton(title, description, button, isSearching);
    }

    /*
     * Currently, only local bookmarks are supported.  Show an empty view if the local database has no data.
     */
    private void setEmptyTitleAndDescriptionForBookmarksList() {
        // replace %s placeholder with bookmark outline icon
        String description = getString(R.string.reader_empty_saved_posts_description);
        SpannableStringBuilder ssb = new SpannableStringBuilder(description);
        int imagePlaceholderPosition = description.indexOf("%s");
        addBookmarkImageSpan(ssb, imagePlaceholderPosition);
        mActionableEmptyView.image.setVisibility(View.VISIBLE);
        mActionableEmptyView.title.setText(R.string.reader_empty_saved_posts_title);
        mActionableEmptyView.subtitle.setText(ssb);
        mActionableEmptyView.subtitle
                .setContentDescription(getString(R.string.reader_empty_saved_posts_content_description));
        mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);
        mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_followed);
        mActionableEmptyView.button.setVisibility(View.VISIBLE);
        mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                setCurrentTagFromEmptyViewButton(ActionableEmptyViewButtonType.FOLLOWED);
            }
        });
    }

    private boolean shouldShowEmptyViewForSelfHostedCta() {
        return isFilterableScreen() && !mAccountStore.hasAccessToken() && mSubFilterViewModel
                .getCurrentSubfilterValue() instanceof SiteAll;
    }

    private void setEmptyTitleAndDescriptionForSelfHostedCta() {
        if (!isAdded()) {
            return;
        }

        mActionableEmptyView.image.setVisibility(View.VISIBLE);
        mActionableEmptyView.title.setText(getString(R.string.reader_self_hosted_select_filter));
        mActionableEmptyView.subtitle.setVisibility(View.GONE);
        mActionableEmptyView.button.setVisibility(View.GONE);
    }

    private void addBookmarkImageSpan(SpannableStringBuilder ssb, int imagePlaceholderPosition) {
        Drawable d = ContextCompat.getDrawable(getActivity(), R.drawable.ic_bookmark_grey_dark_18dp);
        d.setBounds(0, 0, (int) (d.getIntrinsicWidth() * 1.2), (int) (d.getIntrinsicHeight() * 1.2));
        ssb.setSpan(new ImageSpan(d), imagePlaceholderPosition, imagePlaceholderPosition + 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void setEmptyTitleDescriptionAndButton(@NonNull String title, String description,
                                                   final ActionableEmptyViewButtonType button, boolean isSearching) {
        if (!isAdded()) {
            return;
        }

        mActionableEmptyView.image.setVisibility(!isUpdating() && !isSearching ? View.VISIBLE : View.GONE);
        mActionableEmptyView.title.setText(title);

        if (description == null) {
            mActionableEmptyView.subtitle.setVisibility(View.GONE);
        } else {
            mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);

            if (description.contains("<") && description.contains(">")) {
                mActionableEmptyView.subtitle.setText(Html.fromHtml(description));
            } else {
                mActionableEmptyView.subtitle.setText(description);
            }
        }

        if (button == null) {
            mActionableEmptyView.button.setVisibility(View.GONE);
        } else {
            mActionableEmptyView.button.setVisibility(View.VISIBLE);

            switch (button) {
                case DISCOVER:
                    mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_discover);
                    break;
                case FOLLOWED:
                    mActionableEmptyView.button.setText(R.string.reader_empty_followed_blogs_button_followed);
                    break;
            }

            mActionableEmptyView.button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    setCurrentTagFromEmptyViewButton(button);
                }
            });
        }
    }

    private void showEmptyView() {
        if (isAdded()) {
            mActionableEmptyView.setVisibility(View.VISIBLE);
            mActionableEmptyView.announceEmptyStateForAccessibility();
        }
    }

    private void hideEmptyView() {
        if (isAdded()) {
            mActionableEmptyView.setVisibility(View.GONE);
        }
    }

    private void setCurrentTagFromEmptyViewButton(ActionableEmptyViewButtonType button) {
            ReaderTag tag = null;

            switch (button) {
                case DISCOVER:
                    tag = ReaderUtils.getTagFromEndpoint(ReaderTag.DISCOVER_PATH);
                    break;
                case FOLLOWED:
                    tag = ReaderUtils.getTagFromEndpoint(ReaderTag.FOLLOWING_PATH);
                    break;
            }
            if (tag == null) {
                tag = ReaderUtils.getDefaultTag();
            }

            mViewModel.onEmptyStateButtonTapped(tag);
    }

    /*
     * called by post adapter when data has been loaded
     */
    private final ReaderInterfaces.DataLoadedListener mDataLoadedListener = new ReaderInterfaces.DataLoadedListener() {
        @Override
        public void onDataLoaded(boolean isEmpty) {
            if (!isAdded()) {
                return;
            }
            if (isEmpty) {
                if (getPostListType() != ReaderPostListType.SEARCH_RESULTS
                    || getSearchTabsPosition() == TAB_SITES && getSiteSearchAdapter().isEmpty()
                    || getSearchTabsPosition() == TAB_POSTS && getPostAdapter().isEmpty()) {
                    setEmptyTitleDescriptionAndButton(false);
                    showEmptyView();
                }
            } else {
                hideEmptyView();
                announceListStateForAccessibility();
                if (mRestorePosition > 0) {
                    AppLog.d(T.READER, "reader post list > restoring position");
                    mRecyclerView.scrollRecycleViewToPosition(mRestorePosition);
                }
                if (getPostListType() == ReaderPostListType.SEARCH_RESULTS && !isSearchTabsShowing()) {
                    showSearchTabs();
                }
            }
            mRestorePosition = 0;
        }
    };

    private void announceListStateForAccessibility() {
        if (getView() != null) {
            getView().announceForAccessibility(getString(R.string.reader_acessibility_list_loaded,
                    getPostAdapter().getItemCount()));
        }
    }

    private void showBookmarksSavedLocallyDialog(ShowBookmarkedSavedOnlyLocallyDialog holder) {
        mBookmarksSavedLocallyDialog = new MaterialAlertDialogBuilder(requireActivity())
                .setTitle(getString(holder.getTitle()))
                .setMessage(getString(holder.getMessage()))
                .setPositiveButton(holder.getButtonLabel(), (dialog, which) -> holder.getOkButtonAction().invoke())
                .setCancelable(false)
                .create();
        mBookmarksSavedLocallyDialog.show();
    }

    private boolean isBookmarksList() {
        return getPostListType() == ReaderPostListType.TAG_FOLLOWED
               && (mCurrentTag != null && mCurrentTag.isBookmarked());
    }

    /*
     * called by post adapter to load older posts when user scrolls to the last post
     */
    private final ReaderActions.DataRequestedListener mDataRequestedListener =
            new ReaderActions.DataRequestedListener() {
                @Override
                public void onRequestData() {
                    // skip if update is already in progress
                    if (isUpdating()) {
                        return;
                    }

                    // request older posts unless we already have the max # to show
                    switch (getPostListType()) {
                        case TAG_FOLLOWED:
                            // fall through to TAG_PREVIEW
                        case TAG_PREVIEW:
                            if (ReaderPostTable.getNumPostsWithTag(mCurrentTag)
                                < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                // request older posts
                                updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_OLDER);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;

                        case BLOG_PREVIEW:
                            int numPosts;
                            if (mCurrentFeedId != 0) {
                                numPosts = ReaderPostTable.getNumPostsInFeed(mCurrentFeedId);
                            } else {
                                numPosts = ReaderPostTable.getNumPostsInBlog(mCurrentBlogId);
                            }
                            if (numPosts < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                updatePostsInCurrentBlogOrFeed(UpdateAction.REQUEST_OLDER);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;

                        case SEARCH_RESULTS:
                            ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(mCurrentSearchQuery);
                            int offset = ReaderPostTable.getNumPostsWithTag(searchTag);
                            if (offset < ReaderConstants.READER_MAX_POSTS_TO_DISPLAY) {
                                updatePostsInCurrentSearch(offset);
                                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_INFINITE_SCROLL);
                            }
                            break;
                    }
                }
            };

    private ReaderPostAdapter getPostAdapter() {
        if (mPostAdapter == null) {
            AppLog.d(T.READER, "reader post list > creating post adapter");
            Context context = WPActivityUtils.getThemedContext(getActivity());
            mPostAdapter = new ReaderPostAdapter(
                    context,
                    getPostListType(),
                    mImageManager,
                    mUiHelpers,
                    mIsTopLevel
            );
            mPostAdapter.setOnFollowListener(this);
            mPostAdapter.setOnPostSelectedListener(this);
            mPostAdapter.setOnPostListItemButtonListener(this);
            mPostAdapter.setOnDataLoadedListener(mDataLoadedListener);
            mPostAdapter.setOnDataRequestedListener(mDataRequestedListener);
            if (getActivity() instanceof ReaderSiteHeaderView.OnBlogInfoLoadedListener) {
                mPostAdapter.setOnBlogInfoLoadedListener((ReaderSiteHeaderView.OnBlogInfoLoadedListener) getActivity());
            }
            if (getPostListType().isTagType()) {
                mPostAdapter.setCurrentTag(getCurrentTag());
            } else if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                mPostAdapter.setCurrentBlogAndFeed(mCurrentBlogId, mCurrentFeedId);
            } else if (getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
                ReaderTag searchTag = ReaderUtils.getTagForSearchQuery(mCurrentSearchQuery);
                mPostAdapter.setCurrentTag(searchTag);
            }
        }
        return mPostAdapter;
    }

    private ReaderSiteSearchAdapter getSiteSearchAdapter() {
        if (mSiteSearchAdapter == null) {
            mSiteSearchAdapter = new ReaderSiteSearchAdapter(new SiteSearchAdapterListener() {
                @Override
                public void onSiteClicked(@NonNull ReaderSiteModel site) {
                    mLastTappedSiteSearchResult = site;
                    ReaderActivityLauncher.showReaderBlogOrFeedPreview(
                            getActivity(), site.getSiteId(), site.getFeedId());
                }

                @Override
                public void onLoadMore(int offset) {
                    showLoadingProgress(true);
                    updateSitesInCurrentSearch(offset);
                }
            });
        }
        return mSiteSearchAdapter;
    }

    private boolean hasPostAdapter() {
        return (mPostAdapter != null);
    }

    private boolean isPostAdapterEmpty() {
        return (mPostAdapter == null || mPostAdapter.isEmpty());
    }

    private boolean isCurrentTag(final ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    private boolean isCurrentTagName(String tagName) {
        return (tagName != null && tagName.equalsIgnoreCase(getCurrentTagName()));
    }

    private ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    private String getCurrentTagName() {
        return (mCurrentTag != null ? mCurrentTag.getTagSlug() : "");
    }

    private boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    private void setCurrentTag(final ReaderTag tag) {
        if (tag == null) {
            return;
        }

        // skip if this is already the current tag and the post adapter is already showing it
        if (isCurrentTag(tag)
            && hasPostAdapter()
            && getPostAdapter().isCurrentTag(tag)) {
            return;
        }

        mCurrentTag = tag;

        if (isFilterableScreen()) {
            if (isFilterableTag(mCurrentTag) || mCurrentTag.isDefaultInMemoryTag()) {
                mSubFilterViewModel.onSubfilterReselected();
            } else {
                changeReaderMode(new ReaderModeInfo(
                                tag,
                                ReaderPostListType.TAG_FOLLOWED,
                                0,
                                0,
                                false,
                                null,
                                false,
                                mRemoveFilterButton.getVisibility() == View.VISIBLE),
                        false
                );
            }
        }

        ReaderTag validTag;

        validTag = ReaderUtils.getValidTagForSharedPrefs(
                tag,
                mIsTopLevel,
                mRecyclerView,
                ReaderUtils.getDefaultTagFromDbOrCreateInMemory(
                        requireActivity(),
                        mTagUpdateClientUtilsProvider
                )
        );

        switch (getPostListType()) {
            case TAG_FOLLOWED:
                // remember this as the current tag if viewing followed tag
                AppPrefs.setReaderTag(validTag);

                break;
            case TAG_PREVIEW:
                mTagPreviewHistory.push(tag.getTagSlug());
                break;
            case BLOG_PREVIEW:
                if (mIsTopLevel) {
                    AppPrefs.setReaderTag(validTag);
                }
                // noop
                break;
            case SEARCH_RESULTS:
                // noop
                break;
        }

        getPostAdapter().setCurrentTag(mCurrentTag);
        hideNewPostsBar();
        showLoadingProgress(false);
        updateCurrentTagIfTime();
    }

    @Override
    public View getScrollableViewForUniqueIdProvision() {
        return mRecyclerView.getInternalRecyclerView();
    }

    /*
     * called by the activity when user hits the back button - returns true if the back button
     * is handled here and should be ignored by the activity
     */
    @Override
    public boolean onActivityBackPressed() {
        if (isSearchViewExpanded()) {
            mSearchMenuItem.collapseActionView();
            return true;
        } else {
            return goBackInTagHistory();
        }
    }

    /*
     * when previewing posts with a specific tag, a history of previewed tags is retained so
     * the user can navigate back through them - this is faster and requires less memory
     * than creating a new fragment for each previewed tag
     */
    private boolean goBackInTagHistory() {
        if (mTagPreviewHistory.empty()) {
            return false;
        }

        String tagName = mTagPreviewHistory.pop();
        if (isCurrentTagName(tagName)) {
            if (mTagPreviewHistory.empty()) {
                return false;
            }
            tagName = mTagPreviewHistory.pop();
        }

        ReaderTag newTag = ReaderUtils.getTagFromTagName(tagName, ReaderTagType.FOLLOWED);
        setCurrentTag(newTag);

        return true;
    }

    /*
     * refresh adapter so latest posts appear
     */
    private void refreshPosts() {
        hideNewPostsBar();
        if (hasPostAdapter()) {
            getPostAdapter().refresh();
        }
    }

    /*
     * same as above but clears posts before refreshing
     */
    private void reloadPosts() {
        hideNewPostsBar();
        if (hasPostAdapter()) {
            getPostAdapter().reload();
        }
    }

    /*
     * reload the list of tags for the dropdown filter
     */
    private void reloadTags() {
        if (isAdded() && mRecyclerView != null) {
            mRecyclerView.refreshFilterCriteriaOptions();
        }
    }

    /*
     * get posts for the current blog from the server
     */
    private void updatePostsInCurrentBlogOrFeed(final UpdateAction updateAction) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled blog update");
            return;
        }
        if (mCurrentFeedId != 0) {
            ReaderPostServiceStarter.startServiceForFeed(getActivity(), mCurrentFeedId, updateAction);
        } else {
            ReaderPostServiceStarter.startServiceForBlog(getActivity(), mCurrentBlogId, updateAction);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdatePostsStarted event) {
        if (!isAdded()) {
            return;
        }
        // check if the event is related to this instance of the ReaderPostListFragment
        if (event.getReaderTag() != null && !isCurrentTag(event.getReaderTag())) {
            return;
        }
        setIsUpdating(true, event.getAction());
        setEmptyTitleDescriptionAndButton(false);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ReaderEvents.UpdatePostsEnded event) {
        if (!isAdded()) {
            return;
        }
        // check if the event is related to this instance of the ReaderPostListFragment
        if (event.getReaderTag() != null && !isCurrentTag(event.getReaderTag())) {
            return;
        }
        setIsUpdating(false, event.getAction());

        // don't show new posts if user is searching - posts will automatically
        // appear when search is exited
        if (isSearchViewExpanded()
            || getPostListType() == ReaderPostListType.SEARCH_RESULTS) {
            return;
        }

        // determine whether to show the "new posts" bar - when this is shown, the newly
        // downloaded posts aren't displayed until the user taps the bar - only appears
        // when there are new posts in a followed tag and the user has scrolled the list
        // beyond the first post
        if (event.getResult() == ReaderActions.UpdateResult.HAS_NEW
            && event.getAction() == UpdateAction.REQUEST_NEWER
            && getPostListType() == ReaderPostListType.TAG_FOLLOWED
            && !isPostAdapterEmpty()
            && (!isAdded() || !mRecyclerView.isFirstItemVisible())) {
            showNewPostsBar();
        } else if (event.getResult().isNewOrChanged()
                   || event.getAction() == UpdateAction.REQUEST_REFRESH) {
            refreshPosts();
        } else {
            boolean requestFailed = (event.getResult() == ReaderActions.UpdateResult.FAILED);
            setEmptyTitleDescriptionAndButton(requestFailed);
            // if we requested posts in order to fill a gap but the request failed or didn't
            // return any posts, reload the adapter so the gap marker is reset (hiding its
            // progress bar)
            if (event.getAction() == UpdateAction.REQUEST_OLDER_THAN_GAP) {
                reloadPosts();
            }
        }
    }

    /*
     * get latest posts for this tag from the server
     */
    private void updatePostsWithTag(ReaderTag tag, UpdateAction updateAction) {
        if (!isAdded()) {
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            AppLog.i(T.READER, "reader post list > network unavailable, canceled tag update");
            return;
        }
        if (tag == null) {
            AppLog.w(T.READER, "null tag passed to updatePostsWithTag");
            return;
        }
        AppLog.d(T.READER,
                "reader post list > updating tag " + tag.getTagNameForLog() + ", updateAction=" + updateAction.name());
        ReaderPostServiceStarter.startServiceForTag(getActivity(), tag, updateAction);
    }

    private void updateCurrentTag() {
        updatePostsWithTag(getCurrentTag(), UpdateAction.REQUEST_NEWER);
    }

    /*
     * update the current tag if it's time to do so - note that the check is done in the
     * background since it can be expensive and this is called when the fragment is
     * resumed, which on slower devices can result in a janky experience
     */
    private void updateCurrentTagIfTime() {
        if (!isAdded() || !hasCurrentTag()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                if (ReaderTagTable.shouldAutoUpdateTag(getCurrentTag()) && isAdded()) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCurrentTag();
                        }
                    });
                }
            }
        }.start();
    }

    private boolean isUpdating() {
        return mIsUpdating;
    }

    /*
     * show/hide progress bar which appears at the bottom of the activity when loading more posts
     */
    private void showLoadingProgress(boolean showProgress) {
        if (isAdded() && mProgress != null) {
            if (showProgress) {
                mProgress.bringToFront();
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    private void setIsUpdating(boolean isUpdating, UpdateAction updateAction) {
        if (!isAdded() || mIsUpdating == isUpdating) {
            return;
        }

        if (updateAction == UpdateAction.REQUEST_OLDER) {
            // show/hide progress bar at bottom if these are older posts
            showLoadingProgress(isUpdating);
        } else if (isUpdating && isPostAdapterEmpty()) {
            // show swipe-to-refresh if update started and no posts are showing
            mRecyclerView.setRefreshing(true);
        } else if (!isUpdating) {
            // hide swipe-to-refresh progress if update is complete
            mRecyclerView.setRefreshing(false);
        }
        mIsUpdating = isUpdating;

        // if swipe-to-refresh isn't active, keep it disabled during an update - this prevents
        // doing a refresh while another update is already in progress
        if (mRecyclerView != null && !mRecyclerView.isRefreshing()) {
            mRecyclerView.setSwipeToRefreshEnabled(!isUpdating && isSwipeToRefreshSupported());
        }
    }

    /*
     * swipe-to-refresh isn't supported for search results since they're really brief snapshots
     * and are unlikely to show new posts due to the way they're sorted
     */
    private boolean isSwipeToRefreshSupported() {
        return getPostListType() != ReaderPostListType.SEARCH_RESULTS;
    }

    /*
     * bar that appears at the top when new posts have been retrieved
     */
    private boolean isNewPostsBarShowing() {
        return (mNewPostsBar != null && mNewPostsBar.getVisibility() == View.VISIBLE);
    }

    /*
     * scroll listener assigned to the recycler when the "new posts" bar is shown to hide
     * it upon scrolling
     */
    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            hideNewPostsBar();
        }
    };

    private void showNewPostsBar() {
        if (!isAdded() || isNewPostsBarShowing()) {
            return;
        }

        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_in);
        mNewPostsBar.setVisibility(View.VISIBLE);

        // assign the scroll listener to hide the bar when the recycler is scrolled, but don't assign
        // it right away since the user may be scrolling when the bar appears (which would cause it
        // to disappear as soon as it's displayed)
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && isNewPostsBarShowing()) {
                    mRecyclerView.addOnScrollListener(mOnScrollListener);
                }
            }
        }, 1000L);

        // remove the gap marker if it's showing, since it's no longer valid
        getPostAdapter().removeGapMarker();
    }

    private void hideNewPostsBar() {
        if (!isAdded() || !isNewPostsBarShowing() || mIsAnimatingOutNewPostsBar) {
            return;
        }

        mIsAnimatingOutNewPostsBar = true;

        // remove the onScrollListener assigned in showNewPostsBar()
        mRecyclerView.removeOnScrollListener(mOnScrollListener);

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isAdded()) {
                    mNewPostsBar.setVisibility(View.GONE);
                    mIsAnimatingOutNewPostsBar = false;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        AniUtils.startAnimation(mNewPostsBar, R.anim.reader_top_bar_out, listener);
    }

    /*
     * are we showing all posts with a specific tag (followed or previewed), or all
     * posts in a specific blog?
     */
    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    /*
     * called from adapter when user taps a post
     */
    @Override
    public void onPostSelected(ReaderPost post) {
        if (!isAdded() || post == null) {
            return;
        }

        AppRatingDialog.INSTANCE.incrementInteractions(APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST);

        if (post.isBookmarked) {
            if (isBookmarksList()) {
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST);
            } else {
                AnalyticsTracker.track(AnalyticsTracker.Stat.READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST);
            }
        }

        // "discover" posts that highlight another post should open the original (source) post when tapped
        if (post.isDiscoverPost()) {
            ReaderPostDiscoverData discoverData = post.getDiscoverData();
            if (discoverData != null
                && discoverData.getDiscoverType() == ReaderPostDiscoverData.DiscoverType.EDITOR_PICK) {
                if (discoverData.getBlogId() != 0 && discoverData.getPostId() != 0) {
                    ReaderActivityLauncher.showReaderPostDetail(
                            getActivity(),
                            discoverData.getBlogId(),
                            discoverData.getPostId());
                    return;
                } else if (discoverData.hasPermalink()) {
                    if (mSeenUnseenWithCounterFeatureConfig.isEnabled()) {
                        mViewModel.onExternalPostOpened(post);
                    }
                    // if we don't have a blogId/postId, we sadly resort to showing the post
                    // in a WebView activity - this will happen for non-JP self-hosted
                    ReaderActivityLauncher.openUrl(getActivity(), discoverData.getPermaLink());
                    return;
                }
            }
        }

        // if this is a cross-post, we want to show the original post
        if (post.isXpost()) {
            ReaderActivityLauncher.showReaderPostDetail(getActivity(), post.xpostBlogId, post.xpostPostId);
            return;
        }

        ReaderPostListType type = getPostListType();

        switch (type) {
            case TAG_FOLLOWED:
                // fall through to the TAG_PREVIEW
            case TAG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForTag(
                        getActivity(),
                        getCurrentTag(),
                        getPostListType(),
                        post.blogId,
                        post.postId);
                break;
            case BLOG_PREVIEW:
                ReaderActivityLauncher.showReaderPostPagerForBlog(
                        getActivity(),
                        post.blogId,
                        post.postId);
                break;
            case SEARCH_RESULTS:
                AnalyticsUtils.trackWithReaderPostDetails(AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED, post);
                ReaderActivityLauncher.showReaderPostDetail(getActivity(), post.blogId, post.postId);
                break;
        }
    }

    /*
     * called when user selects a tag from the tag toolbar
     */
    private void onTagChanged(ReaderTag tag) {
        if (!isAdded() || isCurrentTag(tag)) {
            return;
        }
        // clear 'post removed from saved posts' undo items
        if (getPostListType() == ReaderPostListType.TAG_FOLLOWED) {
            ReaderPostTable.purgeUnbookmarkedPostsWithBookmarkTag();
        }

        trackTagLoaded(tag);
        AppLog.d(T.READER, String.format("reader post list > tag %s displayed", tag.getTagNameForLog()));
        setCurrentTag(tag);
    }

    private void trackTagLoaded(ReaderTag tag) {
        if (tag == null) {
            return;
        }

        AnalyticsTracker.Stat stat;
        if (tag.isTagTopic()) {
            stat = Stat.READER_TAG_LOADED;
        } else if (tag.isListTopic()) {
            stat = Stat.READER_LIST_LOADED;
        } else {
            return;
        }

        Map<String, String> properties = new HashMap<>();
        properties.put("tag", tag.getTagSlug());

        AnalyticsTracker.track(stat, properties);
    }

    @Override
    public void onButtonClicked(ReaderPost post, ReaderPostCardActionType actionType) {
        switch (actionType) {
            case FOLLOW:
                mViewModel.onFollowSiteClicked(post, isBookmarksList());
                break;
            case SITE_NOTIFICATIONS:
                mViewModel.onSiteNotificationMenuClicked(post.blogId, post.postId, isBookmarksList());
                break;
            case SHARE:
                AnalyticsUtils.trackWithSiteId(Stat.SHARED_ITEM_READER, post.blogId);
                sharePost(post);
                break;
            case VISIT_SITE:
                AnalyticsTracker.track(Stat.READER_ARTICLE_VISITED);
                ReaderActivityLauncher.openPost(getContext(), post);
                break;
            case LIKE:
                mViewModel.onLikeButtonClicked(post, isBookmarksList());
                break;
            case REBLOG:
                mViewModel.onReblogButtonClicked(post, isBookmarksList());
                break;
            case REPORT_POST:
                mViewModel.onReportPostButtonClicked(post, isBookmarksList());
                break;
            case BLOCK_SITE:
                mViewModel.onBlockSiteButtonClicked(post, isBookmarksList());
                break;
            case BOOKMARK:
                mViewModel.onBookmarkButtonClicked(post.blogId, post.postId, isBookmarksList());
                break;
            case COMMENTS:
                ReaderActivityLauncher.showReaderComments(requireContext(), post.blogId, post.postId);
                break;
            case TOGGLE_SEEN_STATUS:
                if (mSeenUnseenWithCounterFeatureConfig.isEnabled()) {
                    mViewModel.onToggleSeenStatusClicked(post, isBookmarksList());
                }
                break;
        }
    }

    @Override
    public void onFollowTapped(View view, String blogName, final long blogId) {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());

        String blog = TextUtils.isEmpty(blogName)
                ? getString(R.string.reader_followed_blog_notifications_this)
                : blogName;

        if (blogId > 0) {
            WPSnackbar.make(getSnackbarParent(), Html.fromHtml(getString(R.string.reader_followed_blog_notifications,
                    "<b>", blog, "</b>")), Snackbar.LENGTH_LONG)
                      .setAction(getString(R.string.reader_followed_blog_notifications_action),
                              new View.OnClickListener() {
                                  @Override public void onClick(View view) {
                                      AnalyticsUtils
                                              .trackWithSiteId(Stat.FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED, blogId);
                                      AddOrDeleteSubscriptionPayload payload = new AddOrDeleteSubscriptionPayload(
                                              String.valueOf(blogId), SubscriptionAction.NEW);
                                      mDispatcher.dispatch(newUpdateSubscriptionNotificationPostAction(payload));
                                      ReaderBlogTable.setNotificationsEnabledByBlogId(blogId, true);
                                  }
                              })
                      .show();
        }
    }

    @Override
    public void onFollowingTapped() {
        mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSubscriptionUpdated(OnSubscriptionUpdated event) {
        if (event.isError()) {
            AppLog.e(T.API, ReaderPostListFragment.class.getSimpleName() + ".onSubscriptionUpdated: "
                            + event.error.type + " - " + event.error.message);
        } else {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSubscriptionsAction());
        }
    }

    private void sharePost(ReaderPost post) {
        String url = (post.hasShortUrl() ? post.getShortUrl() : post.getUrl());

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, url);
        intent.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
        } catch (android.content.ActivityNotFoundException ex) {
            ToastUtils.showToast(getActivity(), R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * purge reader db if it hasn't been done yet
     */
    private void purgeDatabaseIfNeeded() {
        if (!mHasPurgedReaderDb) {
            AppLog.d(T.READER, "reader post list > purging database");
            mHasPurgedReaderDb = true;
            ReaderDatabase.purgeAsync();
        }
    }

    @Override
    public void onScrollToTop() {
        if (isAdded() && getCurrentPosition() > 0) {
            mRecyclerView.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.SITE_PICKER && resultCode == Activity.RESULT_OK) {
            int siteLocalId = data.getIntExtra(SitePickerActivity.KEY_LOCAL_ID, -1);
            mViewModel.onReblogSiteSelected(siteLocalId);
        }
    }

    private boolean isFollowingScreen() {
        return mTagFragmentStartedWith != null && mTagFragmentStartedWith.isFollowedSites();
    }

    private boolean isFilterableScreen() {
        return isFilterableTag(mTagFragmentStartedWith);
    }

    private boolean isFilterableTag(ReaderTag tag) {
        return tag != null && tag.isFilterable();
    }
}
