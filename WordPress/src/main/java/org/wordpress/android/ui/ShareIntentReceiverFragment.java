package org.wordpress.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.accounts.login.LoginHeaderViewHolder;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.SitePickerAdapter.HeaderHandler;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.ViewUtils;
import org.wordpress.android.util.image.ImageManager;

import javax.inject.Inject;

public class ShareIntentReceiverFragment extends Fragment {
    public static final String TAG = "share_intent_fragment_tag";

    private static final String ARG_SHARING_MEDIA = "ARG_SHARING_MEDIA";
    private static final String ARG_LAST_USED_BLOG_LOCAL_ID = "ARG_LAST_USED_BLOG_LOCAL_ID";
    private static final String ARG_AFTER_LOGIN = "ARG_AFTER_LOGIN";

    @Inject AccountStore mAccountStore;
    @Inject ImageManager mImageManager;
    private ShareIntentFragmentListener mShareIntentFragmentListener;
    private SitePickerAdapter mAdapter;

    private Button mSharePostBtn;
    private Button mShareMediaBtn;

    private boolean mSharingMediaFile;
    private int mLastUsedBlogLocalId;
    private boolean mAfterLogin;
    private RecyclerView mRecyclerView;
    private View mBottomButtonsContainer;
    private View mBottomButtonsShadow;

    public static ShareIntentReceiverFragment newInstance(boolean sharingMediaFile, int lastUsedBlogLocalId,
                                                          boolean afterLogin) {
        ShareIntentReceiverFragment fragment = new ShareIntentReceiverFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHARING_MEDIA, sharingMediaFile);
        args.putBoolean(ARG_AFTER_LOGIN, afterLogin);
        args.putInt(ARG_LAST_USED_BLOG_LOCAL_ID, lastUsedBlogLocalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ShareIntentFragmentListener) {
            mShareIntentFragmentListener = (ShareIntentFragmentListener) context;
        } else {
            throw new RuntimeException("The parent activity doesn't implement ShareIntentFragmentListener.");
        }
    }

    @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.share_intent_screen_title);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.login_epilogue_screen, container, false);
        initButtonsContainer(layout);
        initShareActionPostButton(layout);
        initShareActionMediaButton(layout, mSharingMediaFile);
        initRecyclerView(layout);
        return layout;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mSharingMediaFile = getArguments().getBoolean(ARG_SHARING_MEDIA);
        mAfterLogin = getArguments().getBoolean(ARG_AFTER_LOGIN);
        mLastUsedBlogLocalId = getArguments().getInt(ARG_LAST_USED_BLOG_LOCAL_ID);
        loadSavedState(savedInstanceState);
    }

    private void loadSavedState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mLastUsedBlogLocalId = savedInstanceState.getInt(ARG_LAST_USED_BLOG_LOCAL_ID);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int selectedItemLocalId = mAdapter.getSelectedItemLocalId();
        if (selectedItemLocalId != -1) {
            outState.putInt(ARG_LAST_USED_BLOG_LOCAL_ID, mAdapter.getSelectedItemLocalId());
        }
    }

    private void initButtonsContainer(ViewGroup layout) {
        mBottomButtonsContainer = layout.findViewById(R.id.bottom_buttons);
        mBottomButtonsShadow = layout.findViewById(R.id.bottom_shadow);
    }

    private void initShareActionPostButton(final ViewGroup layout) {
        mSharePostBtn = layout.findViewById(R.id.primary_button);
        addShareActionListener(mSharePostBtn, ShareAction.SHARE_TO_POST);
        mSharePostBtn.setVisibility(View.VISIBLE);
        mSharePostBtn.setText(R.string.share_action_post);
    }

    private void initShareActionMediaButton(final ViewGroup layout, boolean sharingMediaFile) {
        mShareMediaBtn = layout.findViewById(R.id.secondary_button);
        addShareActionListener(mShareMediaBtn, ShareAction.SHARE_TO_MEDIA_LIBRARY);
        mShareMediaBtn.setVisibility(sharingMediaFile ? View.VISIBLE : View.GONE);
        mShareMediaBtn.setText(R.string.share_action_media);
    }

    private void addShareActionListener(final Button button, final ShareAction shareAction) {
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareIntentFragmentListener.share(shareAction, mAdapter.getSelectedItemLocalId());
            }
        });
    }

    private void initRecyclerView(ViewGroup layout) {
        mRecyclerView = layout.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(createSiteAdapter());
    }

    private Adapter createSiteAdapter() {
        mAdapter = new SitePickerAdapter(
                getActivity(), R.layout.share_intent_sites_listitem, 0, "", false,
                new SitePickerAdapter.OnDataLoadedListener() {
                    @Override
                    public void onBeforeLoad(boolean isEmpty) {
                    }

                    @Override
                    public void onAfterLoad() {
                        mRecyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!isAdded()) {
                                    return;
                                }

                                if (mRecyclerView.computeVerticalScrollRange() > mRecyclerView.getHeight()) {
                                    mBottomButtonsShadow.setVisibility(View.VISIBLE);
                                    mBottomButtonsContainer.setBackgroundResource(android.R.color.white);
                                    mShareMediaBtn.setTextColor(getResources().getColor(R.color.primary_50));
                                    ViewUtils.setButtonBackgroundColor(getContext(), mShareMediaBtn,
                                            R.style.WordPress_Button_Grey,
                                            R.attr.colorButtonNormal);
                                } else {
                                    mBottomButtonsShadow.setVisibility(View.GONE);
                                    mBottomButtonsContainer.setBackground(null);
                                    mShareMediaBtn.setTextColor(
                                            ContextExtensionsKt
                                                    .getColorFromAttribute(getContext(), R.attr.wpColorText));
                                    ViewUtils.setButtonBackgroundColor(getContext(), mShareMediaBtn,
                                            R.style.WordPress_Button,
                                            R.attr.colorButtonNormal);
                                }
                            }
                        });
                        mAdapter.findAndSelect(mLastUsedBlogLocalId);
                    }
                },
                createHeaderHandler(),
                null
        );
        mAdapter.setSingleItemSelectionEnabled(true);
        return mAdapter;
    }

    private HeaderHandler createHeaderHandler() {
        return new HeaderHandler() {
            @Override
            public ViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent,
                                                 boolean attachToRoot) {
                return new LoginHeaderViewHolder(layoutInflater.inflate(R.layout.login_epilogue_header, parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, SiteList sites) {
                refreshAccountDetails((LoginHeaderViewHolder) holder, sites);
            }
        };
    }

    private void refreshAccountDetails(LoginHeaderViewHolder holder, SiteList sites) {
        if (!isAdded()) {
            return;
        }
        holder.updateLoggedInAsHeading(getContext(), mImageManager, mAfterLogin, mAccountStore.getAccount());
        holder.showSitesHeading(
                getString(sites.size() == 1 ? R.string.share_intent_adding_to : R.string.share_intent_pick_site));
    }

    enum ShareAction {
        SHARE_TO_POST("new_post", EditPostActivity.class),
        SHARE_TO_MEDIA_LIBRARY("media_library", MediaBrowserActivity.class);

        public final Class targetClass;
        public final String analyticsName;


        ShareAction(String analyticsName, Class targetClass) {
            this.targetClass = targetClass;
            this.analyticsName = analyticsName;
        }
    }

    interface ShareIntentFragmentListener {
        void share(ShareAction shareAction, int selectedSiteLocalId);
    }
}
