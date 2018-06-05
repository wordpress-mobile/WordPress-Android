package org.wordpress.android.ui.reader.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ReaderFeedModel;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter which shows the results of a reader site search
 */
public class ReaderSiteSearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface SiteClickListener {
        void onSiteClicked(ReaderFeedModel site);
    }

    private SiteClickListener mClickListener;
    private final List<ReaderFeedModel> mSites = new ArrayList<>();

    public ReaderSiteSearchAdapter() {
        super();
        setHasStableIds(true);
    }

    public void setSiteClickListener(SiteClickListener listener) {
        mClickListener = listener;
    }

    public void setFeedList(@NonNull List<ReaderFeedModel> feeds) {
        mSites.clear();
        mSites.addAll(feeds);
        notifyDataSetChanged();
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
        return mSites.get(position).getFeedId();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                  .inflate(R.layout.reader_site_search_result, parent, false);
        return new SiteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ReaderFeedModel feed = mSites.get(position);
        SiteViewHolder siteHolder = (SiteViewHolder) holder;
        siteHolder.mTxtTitle.setText(feed.getTitle());
        siteHolder.mTxtUrl.setText(UrlUtils.getHost(feed.getUrl()));
        siteHolder.mImgBlavatar.setImageUrl(feed.getIconUrl(), ImageType.BLAVATAR);
        siteHolder.mFollowButton.setIsFollowed(feed.isFollowing());
    }

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTxtTitle;
        private final TextView mTxtUrl;
        private ReaderFollowButton mFollowButton;
        private final WPNetworkImageView mImgBlavatar;

        SiteViewHolder(View view) {
            super(view);
            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtUrl = view.findViewById(R.id.text_url);
            mFollowButton = view.findViewById(R.id.follow_button);
            mImgBlavatar = view.findViewById(R.id.image_blavatar);

            view.setOnClickListener(new OnClickListener() {
                @Override public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position) && mClickListener != null) {
                        ReaderFeedModel site = mSites.get(position);
                        mClickListener.onSiteClicked(site);
                    }
                }
            });
        }
    }
}
