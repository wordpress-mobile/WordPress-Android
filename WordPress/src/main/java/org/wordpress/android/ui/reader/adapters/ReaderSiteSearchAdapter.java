package org.wordpress.android.ui.reader.adapters;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.ui.reader.views.ReaderSiteSearchResultView;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter which shows the results of a reader site search
 */
public class ReaderSiteSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ReaderSiteSearchResultView.OnSiteFollowedListener {
    public interface SiteSearchAdapterListener {
        void onSiteClicked(@NonNull ReaderSiteModel site);
        void onLoadMore(int offset);
    }

    private final SiteSearchAdapterListener mListener;
    private final List<ReaderSiteModel> mSites = new ArrayList<>();
    private boolean mCanLoadMore = true;
    private boolean mIsLoadingMore;

    public ReaderSiteSearchAdapter(@NonNull SiteSearchAdapterListener listener) {
        super();
        mListener = listener;
        setHasStableIds(true);
    }

    public void setSiteList(@NonNull List<ReaderSiteModel> sites) {
        mSites.clear();
        mSites.addAll(sites);
        mCanLoadMore = true;
        mIsLoadingMore = false;
        notifyDataSetChanged();
    }

    public void addSiteList(@NonNull List<ReaderSiteModel> sites) {
        mSites.addAll(sites);
        mIsLoadingMore = false;
        notifyDataSetChanged();
    }

    public void clear() {
        mIsLoadingMore = false;
        if (mSites.size() > 0) {
            mSites.clear();
            notifyDataSetChanged();
        }
    }

    private void checkLoadMore(int position) {
        if (mCanLoadMore
            && !mIsLoadingMore
            && position >= getItemCount() - 1
            && getItemCount() >= ReaderConstants.READER_MAX_SEARCH_RESULTS_TO_REQUEST) {
            mIsLoadingMore = true;
            mListener.onLoadMore(getItemCount());
        }
    }

    public void setCanLoadMore(boolean canLoadMore) {
        mCanLoadMore = canLoadMore;
    }

    private boolean isValidPosition(int position) {
        return position >= 0 && position < getItemCount();
    }

    public boolean isEmpty() {
        return mSites.size() == 0;
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        if (!isValidPosition(position)) {
            return -1;
        }
        ReaderSiteModel site = mSites.get(position);
        return site.getFeedId() != 0 ? site.getFeedId() : site.getSiteId();
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
        siteHolder.mSearchResultView.setSite(mSites.get(position), this);

        checkLoadMore(position);
    }

    @Override
    public void onSiteFollowed(@NonNull ReaderSiteModel site) {
        setSiteFollowed(site, true);
    }

    @Override
    public void onSiteUnFollowed(@NonNull ReaderSiteModel site) {
        setSiteFollowed(site, false);
    }

    private void setSiteFollowed(@NonNull ReaderSiteModel site, boolean isFollowed) {
        for (int position = 0; position < mSites.size(); position++) {
            if (mSites.get(position).getFeedId() == site.getFeedId()) {
                mSites.get(position).setFollowing(isFollowed);
                notifyItemChanged(position);
                break;
            }
        }
    }

    public void checkFollowStatusForSite(@NonNull ReaderSiteModel site) {
        boolean isFollowed;
        if (site.getSiteId() != 0) {
            isFollowed = ReaderBlogTable.isFollowedBlog(site.getSiteId());
        } else {
            isFollowed = ReaderBlogTable.isFollowedFeed(site.getFeedId());
        }
        setSiteFollowed(site, isFollowed);
    }

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ReaderSiteSearchResultView mSearchResultView;

        SiteViewHolder(View view) {
            super(view);
            mSearchResultView = (ReaderSiteSearchResultView) view;
            view.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    int position = getBindingAdapterPosition();
                    if (isValidPosition(position) && mListener != null) {
                        ReaderSiteModel site = mSites.get(position);
                        mListener.onSiteClicked(site);
                    }
                }
            });
        }
    }
}
