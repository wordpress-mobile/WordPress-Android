package org.wordpress.android.ui.publicize;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnAdapterLoadedListener;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnServiceClickListener;
import org.wordpress.android.util.NetworkUtils;

import javax.inject.Inject;

public class PublicizeListFragment extends PublicizeBaseFragment {
    public interface PublicizeButtonPrefsListener {
        void onButtonPrefsClicked();
    }

    private PublicizeButtonPrefsListener mListener;
    private SiteModel mSite;
    private PublicizeServiceAdapter mAdapter;
    private RecyclerView mRecycler;
    private TextView mEmptyView;

    @Inject AccountStore mAccountStore;

    public static PublicizeListFragment newInstance(@NonNull SiteModel site) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);

        PublicizeListFragment fragment = new PublicizeListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mSite = (SiteModel) args.getSerializable(WordPress.SITE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_list_fragment, container, false);

        mRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);

        View manageContainer = rootView.findViewById(R.id.container_manage);
        manageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onButtonPrefsClicked();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
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
            if (!isAdded()) return;

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
                mAdapter.setOnServiceClickListener((OnServiceClickListener) getActivity());
            }
        }
        return mAdapter;
    }

    void reload() {
        getAdapter().reload();
    }

}
