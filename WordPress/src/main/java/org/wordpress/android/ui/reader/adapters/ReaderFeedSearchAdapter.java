package org.wordpress.android.ui.reader.adapters;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ReaderFeedModel;
import org.wordpress.android.ui.reader.adapters.ReaderFeedSearchAdapter.FeedViewHolder;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter which shows the results of a reader site search
 */
public class ReaderFeedSearchAdapter extends RecyclerView.Adapter<FeedViewHolder> {
    public interface FeedClickListener {
        void onFeedClicked(ReaderFeedModel feed);
    }

    private FeedClickListener mClickListener;

    private final List<ReaderFeedModel> mFeeds = new ArrayList<>();

    private String mSearchFilter;

    public ReaderFeedSearchAdapter() {
        super();
        setHasStableIds(true);
    }

    public void setSiteClickListener(FeedClickListener listener) {
        mClickListener = listener;
    }

    public void setFeedList(@NonNull List<ReaderFeedModel> feeds) {
        mFeeds.clear();
        mFeeds.addAll(feeds);
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return (getItemCount() == 0);
    }

    @Override
    public int getItemCount() {
        return mFeeds.size();
    }

    @Override
    public long getItemId(int position) {
        return mFeeds.get(position).getFeedId();
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                                         .inflate(R.layout.reader_listitem_site_search_result, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {
        ReaderFeedModel feed = mFeeds.get(position);
        holder.mTxtTitle.setText(feed.getTitle());
        holder.mTxtUrl.setText(UrlUtils.getHost(feed.getUrl()));
        holder.mImgBlavatar.setImageUrl(feed.getIconUrl(), ImageType.BLAVATAR);
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTxtTitle;
        private final TextView mTxtUrl;
        private final WPNetworkImageView mImgBlavatar;

        FeedViewHolder(View view) {
            super(view);

            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtUrl = view.findViewById(R.id.text_url);
            mImgBlavatar = view.findViewById(R.id.image_blavatar);
        }
    }
}
