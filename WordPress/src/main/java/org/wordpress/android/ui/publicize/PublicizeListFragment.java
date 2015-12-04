package org.wordpress.android.ui.publicize;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnAdapterLoadedListener;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnServiceConnectionClickListener;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;

public class PublicizeListFragment extends PublicizeBaseFragment {
    private int mSiteId;
    private PublicizeServiceAdapter mAdapter;
    private RecyclerView mRecycler;
    private TextView mEmptyView;

    public static PublicizeListFragment newInstance(int siteId) {
        Bundle args = new Bundle();
        args.putInt(PublicizeConstants.ARG_SITE_ID, siteId);

        PublicizeListFragment fragment = new PublicizeListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);

        if (args != null) {
            mSiteId = args.getInt(PublicizeConstants.ARG_SITE_ID);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSiteId = savedInstanceState.getInt(PublicizeConstants.ARG_SITE_ID);
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
        setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_list_fragment, container, false);

        mRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_connections);
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getActivity(), 1);
        mRecycler.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        return rootView;
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
            mAdapter = new PublicizeServiceAdapter(getActivity(), mSiteId);
            mAdapter.setOnAdapterLoadedListener(mAdapterLoadedListener);
            if (getActivity() instanceof OnServiceConnectionClickListener) {
                mAdapter.setOnServiceClickListener((OnServiceConnectionClickListener) getActivity());
            }
        }
        return mAdapter;
    }

    void reload() {
        getAdapter().reload();
    }

}
