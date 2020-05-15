package org.wordpress.android.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteList;
import org.wordpress.android.ui.main.SitePickerAdapter.ViewHolderHandler;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.util.image.ImageManager;

import javax.inject.Inject;

public class ShareIntentReceiverFragment extends Fragment {
    public static final String TAG = "share_intent_fragment_tag";

    private static final String ARG_SHARING_MEDIA = "ARG_SHARING_MEDIA";
    private static final String ARG_LAST_USED_BLOG_LOCAL_ID = "ARG_LAST_USED_BLOG_LOCAL_ID";

    @Inject AccountStore mAccountStore;
    @Inject ImageManager mImageManager;
    private ShareIntentFragmentListener mShareIntentFragmentListener;
    private SitePickerAdapter mAdapter;

    private Button mSharePostBtn;
    private Button mShareMediaBtn;

    private boolean mSharingMediaFile;
    private int mLastUsedBlogLocalId;
    private RecyclerView mRecyclerView;
    private View mBottomButtonsContainer;
    private View mBottomButtonsShadow;

    public static ShareIntentReceiverFragment newInstance(boolean sharingMediaFile, int lastUsedBlogLocalId) {
        ShareIntentReceiverFragment fragment = new ShareIntentReceiverFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SHARING_MEDIA, sharingMediaFile);
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
        ViewGroup layout = (ViewGroup) inflater.inflate(R.layout.share_intent_receiver_fragment, container, false);
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
    }

    private void initShareActionMediaButton(final ViewGroup layout, boolean sharingMediaFile) {
        mShareMediaBtn = layout.findViewById(R.id.secondary_button);
        addShareActionListener(mShareMediaBtn, ShareAction.SHARE_TO_MEDIA_LIBRARY);
        mShareMediaBtn.setVisibility(sharingMediaFile ? View.VISIBLE : View.GONE);
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
                                } else {
                                    mBottomButtonsShadow.setVisibility(View.GONE);
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

    private ViewHolderHandler<HeaderViewHolder> createHeaderHandler() {
        return new ViewHolderHandler<HeaderViewHolder>() {
            @Override
            public HeaderViewHolder onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent,
                                                       boolean attachToRoot) {
                return new HeaderViewHolder(
                        layoutInflater.inflate(R.layout.share_intent_receiver_header, parent, false));
            }

            @Override
            public void onBindViewHolder(HeaderViewHolder holder, SiteList sites) {
                if (!isAdded()) {
                    return;
                }
                holder.bindText(getString(
                        sites.size() == 1 ? R.string.share_intent_adding_to : R.string.share_intent_pick_site));
            }
        };
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView mHeaderTextView;

        HeaderViewHolder(View view) {
            super(view);
            mHeaderTextView = view.findViewById(R.id.header_text_view);
        }

        void bindText(String text) {
            mHeaderTextView.setText(text);
        }
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
