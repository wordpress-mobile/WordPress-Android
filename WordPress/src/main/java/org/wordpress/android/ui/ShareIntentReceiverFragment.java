package org.wordpress.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import javax.inject.Inject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.SitePickerAdapter.HeaderHandler;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

public class ShareIntentReceiverFragment extends Fragment {

    public static final String TAG = "share_intent_fragment_tag";

    private static final String ARG_SHARING_MEDIA = "ARG_SHARING_MEDIA";
    private static final String ARG_LAST_USED_BLOG_LOCAL_ID = "ARG_LAST_USED_BLOG_LOCAL_ID";
    private static final String ARG_AFTER_LOGIN = "ARG_AFTER_LOGIN";

    @Inject AccountStore mAccountStore;
    private ShareIntentFragmentListener mShareIntentFragmentListener;
    private SitePickerAdapter mAdapter;

    private Button mSharePostBtn;
    private Button mShareMediaBtn;

    private boolean mSharingMediaFile;
    private int mLastUsedBlogLocalId;
    private boolean mAfterLogin;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.share_intent_fragment, container, false);
        initShareActionPostButton(layout);
        if (mSharingMediaFile) {
            initShareActionMediaButton(layout);
        }
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
        outState.putInt(ARG_LAST_USED_BLOG_LOCAL_ID, mAdapter.getSelectedItemLocalId());
    }

    private void initShareActionPostButton(final ViewGroup layout) {
        mSharePostBtn = (Button) layout.findViewById(R.id.primary_button);
        addShareActionListener(mSharePostBtn, ShareAction.SHARE_TO_POST);
        mSharePostBtn.setVisibility(View.VISIBLE);
    }

    private void initShareActionMediaButton(final ViewGroup layout) {
        mShareMediaBtn = (Button) layout.findViewById(R.id.secondary_button);
        addShareActionListener(mShareMediaBtn, ShareAction.SHARE_TO_MEDIA_LIBRARY);
        mShareMediaBtn.setVisibility(View.VISIBLE);
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
        RecyclerView recyclerView = (RecyclerView) layout.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        recyclerView.setItemAnimator(null);
        recyclerView.setAdapter(createSiteAdapter());
    }

    private Adapter createSiteAdapter() {
        mAdapter = new SitePickerAdapter(getActivity(), R.layout.share_intent_sites_listitem, 0, "", false,
            new SitePickerAdapter.OnDataLoadedListener() {
                @Override
                public void onBeforeLoad(boolean isEmpty) {
                }

                @Override
                public void onAfterLoad() {
                    if (mAdapter.getItemCount() == 0) {
                        ToastUtils
                            .showToast(getContext(), R.string.cant_share_no_visible_blog, ToastUtils.Duration.LONG);
                        getActivity().finish();
                    } else {
                        mAdapter.findAndSelect(mLastUsedBlogLocalId);
                    }
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
                return new HeaderViewHolder(layoutInflater.inflate(R.layout.share_intent_header, parent, false));
            }

            @Override
            public void onBindViewHolder(ViewHolder holder, int numberOfSites) {
                refreshAccountDetails((HeaderViewHolder) holder);
            }
        };
    }

    private void refreshAccountDetails(HeaderViewHolder holder) {
        if (!isAdded()) {
            return;
        }
        // we only want to show user details for WordPress.com users
        if (mAfterLogin && mAccountStore.hasAccessToken()) {
            holder.mLoggedInAsHeading.setVisibility(View.VISIBLE);
            holder.mUserDetailsCard.setVisibility(View.VISIBLE);

            AccountModel defaultAccount = mAccountStore.getAccount();
            final String avatarUrl = constructGravatarUrl(defaultAccount);
            holder.mAvatarImageView.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR, null);
            holder.mUsernameTextView.setText(getString(R.string.login_username_at, defaultAccount.getUserName()));

            String displayName = defaultAccount.getDisplayName();
            if (!TextUtils.isEmpty(displayName)) {
                holder.mDisplayNameTextView.setText(displayName);
            } else {
                holder.mDisplayNameTextView.setText(defaultAccount.getUserName());
            }
        } else {
            holder.mLoggedInAsHeading.setVisibility(View.GONE);
            holder.mUserDetailsCard.setVisibility(View.GONE);
        }

        holder.mPickSiteHeadingTextView.setVisibility(View.VISIBLE);
    }

    private String constructGravatarUrl(AccountModel account) {
        int avatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_large);
        return GravatarUtils.fixGravatarUrl(account.getAvatarUrl(), avatarSz);
    }

    enum ShareAction {
        SHARE_TO_POST(1, EditPostActivity.class), SHARE_TO_MEDIA_LIBRARY(2, MediaBrowserActivity.class);

        public final Class targetClass;
        public final int id;


        ShareAction(int id, Class targetClass) {
            this.targetClass = targetClass;
            this.id = id;
        }

        public static ShareAction fromId(int id) {
            for (ShareAction item : ShareAction.values()) {
                if (item.id == id) {
                    return item;
                }
            }
            AppLog.w(T.SHARING, "Unknown ShareAction type.");
            return SHARE_TO_POST;
        }
    }

    interface ShareIntentFragmentListener {

        void share(ShareAction shareAction, int selectedSiteLocalId);
    }

    private class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final View mLoggedInAsHeading;
        private final View mUserDetailsCard;
        private final WPNetworkImageView mAvatarImageView;
        private final TextView mDisplayNameTextView;
        private final TextView mUsernameTextView;
        private final TextView mPickSiteHeadingTextView;

        HeaderViewHolder(View view) {
            super(view);
            mLoggedInAsHeading = view.findViewById(R.id.logged_in_as_heading);
            mUserDetailsCard = view.findViewById(R.id.user_details_card);
            mAvatarImageView = (WPNetworkImageView) view.findViewById(R.id.avatar);
            mDisplayNameTextView = (TextView) view.findViewById(R.id.display_name);
            mUsernameTextView = (TextView) view.findViewById(R.id.username);
            mPickSiteHeadingTextView = (TextView) view.findViewById(R.id.pick_site_heading);
        }
    }
}
