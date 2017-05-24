package org.wordpress.android.ui.publicize;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnAdapterLoadedListener;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter.OnServiceClickListener;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import javax.inject.Inject;

public class PublicizeListFragment extends PublicizeBaseFragment {
    public interface PublicizeManageConnectionsListener {
        void onManageConnectionsClicked();
    }

    private PublicizeManageConnectionsListener mListener;
    private int mSiteId;
    private PublicizeServiceAdapter mAdapter;
    private RecyclerView mRecycler;
    private TextView mEmptyView;
    private Button mManageButton;

    @Inject AccountStore mAccountStore;

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
        ((WordPress) getActivity().getApplication()).component().inject(this);

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
        setNavigationIcon(R.drawable.ic_arrow_left_white_24dp);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PublicizeConstants.ARG_SITE_ID, mSiteId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.publicize_list_fragment, container, false);

        mRecycler = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mEmptyView = (TextView) rootView.findViewById(R.id.empty_view);
        mManageButton = (Button) rootView.findViewById(R.id.manage_button);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getActivity(), 1);
        mRecycler.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mManageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onManageConnectionsClicked();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof PublicizeManageConnectionsListener) {
            mListener = (PublicizeManageConnectionsListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement PublicizeManageConnectionsListener");
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
                    mSiteId,
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
