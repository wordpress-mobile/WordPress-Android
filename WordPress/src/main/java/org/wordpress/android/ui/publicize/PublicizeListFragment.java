package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.os.Bundle;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.QuickStartStore;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnAdapterLoadedListener;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnServiceClickListener;
import org.wordpress.android.ui.quickstart.QuickStartEvent;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.QuickStartUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPDialogSnackbar;

import javax.inject.Inject;

import static org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask.ENABLE_POST_SHARING;

public class PublicizeListFragment extends PublicizeBaseFragment {
    public interface PublicizeButtonPrefsListener {
        void onButtonPrefsClicked();
    }

    private PublicizeButtonPrefsListener mListener;
    private SiteModel mSite;
    private PublicizeServiceAdapter mAdapter;
    private RecyclerView mRecycler;
    private TextView mEmptyView;

    private QuickStartEvent mQuickStartEvent;

    @Inject AccountStore mAccountStore;
    @Inject QuickStartStore mQuickStartStore;
    @Inject Dispatcher mDispatcher;

    public static PublicizeListFragment newInstance(@NonNull SiteModel site) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);

        PublicizeListFragment fragment = new PublicizeListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (getArguments() != null) {
            mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }

        if (savedInstanceState != null) {
            mQuickStartEvent = savedInstanceState.getParcelable(QuickStartEvent.KEY);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && mRecycler.getAdapter() == null) {
            mRecycler.setAdapter(getAdapter());
        }
        getAdapter().refresh();
        setTitle(R.string.sharing);
        setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_list_fragment, container, false);

        mRecycler = rootView.findViewById(R.id.recycler_view);
        mEmptyView = rootView.findViewById(R.id.empty_view);

        boolean isAdminOrSelfHosted = mSite.getHasCapabilityManageOptions() || !SiteUtils.isAccessedViaWPComRest(mSite);
        View manageContainer = rootView.findViewById(R.id.manage_container);
        if (isAdminOrSelfHosted) {
            manageContainer.setVisibility(View.VISIBLE);
            View manageButton = rootView.findViewById(R.id.manage_button);
            manageButton.setOnClickListener(view -> {
                if (mListener != null) {
                    mListener.onButtonPrefsClicked();
                }
            });
        } else {
            manageContainer.setVisibility(View.GONE);
        }

        if (mQuickStartEvent != null) {
            showQuickStartFocusPoint();
        }

        return rootView;
    }

    @SuppressWarnings("unused")
    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEvent(final QuickStartEvent event) {
        if (!isAdded() || getView() == null) {
            return;
        }

        mQuickStartEvent = event;
        EventBus.getDefault().removeStickyEvent(event);

        if (mQuickStartEvent.getTask() == ENABLE_POST_SHARING) {
            showQuickStartFocusPoint();

            Spannable title = QuickStartUtils.stylizeQuickStartPrompt(getActivity(),
                    R.string.quick_start_dialog_enable_sharing_message_short_connections);

            WPDialogSnackbar.make(getView(), title,
                    getResources().getInteger(R.integer.quick_start_snackbar_duration_ms)).show();
        }
    }

    private void showQuickStartFocusPoint() {
        // we are waiting for RecyclerView to populate itself with views and then grab the first one when it's ready
        mRecycler.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override public void onGlobalLayout() {
                RecyclerView.ViewHolder holder = mRecycler.findViewHolderForAdapterPosition(0);
                if (holder != null) {
                    final View quickStartTarget = holder.itemView;

                    quickStartTarget.post(new Runnable() {
                        @Override public void run() {
                            if (getView() == null) {
                                return;
                            }
                            ViewGroup focusPointContainer = getView().findViewById(R.id.publicize_scroll_view_child);
                            int focusPointSize =
                                    getResources().getDimensionPixelOffset(R.dimen.quick_start_focus_point_size);

                            int verticalOffset = (((quickStartTarget.getHeight()) - focusPointSize) / 2);

                            QuickStartUtils.addQuickStartFocusPointAboveTheView(focusPointContainer, quickStartTarget,
                                    0, verticalOffset);
                        }
                    });
                    mRecycler.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(QuickStartEvent.KEY, mQuickStartEvent);
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);

        if (activity instanceof PublicizeButtonPrefsListener) {
            mListener = (PublicizeButtonPrefsListener) activity;
        } else {
            throw new RuntimeException(activity.toString() + " must implement PublicizeButtonPrefsListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final OnAdapterLoadedListener mAdapterLoadedListener = new OnAdapterLoadedListener() {
        @Override
        public void onAdapterLoaded(boolean isEmpty) {
            if (!isAdded()) {
                return;
            }

            if (isEmpty) {
                if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                    mEmptyView.setText(R.string.no_network_title);
                } else {
                    mEmptyView.setText(R.string.loading);
                }
            }
            mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
    };

    private PublicizeServiceAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new PublicizeServiceAdapter(
                    getActivity(),
                    mSite.getSiteId(),
                    mAccountStore.getAccount().getUserId());
            mAdapter.setOnAdapterLoadedListener(mAdapterLoadedListener);
            if (getActivity() instanceof OnServiceClickListener) {
                mAdapter.setOnServiceClickListener(service -> {
                    QuickStartUtils.completeTaskAndRemindNextOne(mQuickStartStore, ENABLE_POST_SHARING,
                            mDispatcher, mSite, mQuickStartEvent, getContext());
                    if (getView() != null) {
                        QuickStartUtils.removeQuickStartFocusPoint((ViewGroup) getView());
                    }
                    mQuickStartEvent = null;
                    ((OnServiceClickListener) getActivity()).onServiceClicked(service);
                });
            }
        }
        return mAdapter;
    }

    void reload() {
        getAdapter().reload();
    }

    @Override public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
