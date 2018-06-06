package org.wordpress.android.ui.reader.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.ui.reader.views.ReaderSiteSearchResultView;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter which shows the results of a reader site search
 */
public class ReaderSiteSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface SiteSearchAdapterListener {
        void onSiteClicked(ReaderSiteModel site);
        void onLoadMore(int offset);
    }

    private final SiteSearchAdapterListener mListener;
    private final List<ReaderSiteModel> mSites = new ArrayList<>();
    private boolean mCanLoadMore = true;

    public ReaderSiteSearchAdapter(@NonNull SiteSearchAdapterListener listener) {
        super();
        mListener = listener;
        setHasStableIds(true);
    }

    public void setSiteList(@NonNull List<ReaderSiteModel> sites) {
        mSites.clear();
        mSites.addAll(sites);
        mCanLoadMore = true;
        notifyDataSetChanged();
    }

    public void addSiteList(@NonNull List<ReaderSiteModel> sites) {
        mSites.addAll(sites);
        notifyDataSetChanged();
    }

    public void clear() {
        if (mSites.size() > 0) {
            mSites.clear();
            notifyDataSetChanged();
        }
    }

    public void setCanLoadMore(boolean canLoadMore) {
        mCanLoadMore = canLoadMore;
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < getItemCount();
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        // note that we use the feedId because the siteId may be 0 for feeds
        return mSites.get(position).getFeedId();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ReaderSiteSearchResultView view = new ReaderSiteSearchResultView(parent.getContext());
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (!isValidPosition(position)) {
            return;
        }

        SiteViewHolder siteHolder = (SiteViewHolder) holder;
        siteHolder.mSearchResultView.setSite(mSites.get(position));

        if (mCanLoadMore && position >= getItemCount() - 1) {
            mListener.onLoadMore(getItemCount() - 1);
        }
    }

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ReaderSiteSearchResultView mSearchResultView;

        SiteViewHolder(View view) {
            super(view);
            mSearchResultView = (ReaderSiteSearchResultView) view;
            view.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position) && mListener != null) {
                        ReaderSiteModel site = mSites.get(position);
                        mListener.onSiteClicked(site);
                    }
                }
            });
        }
    }
}
