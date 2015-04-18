package org.wordpress.android.ui.main;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.HashSet;

class SitePickerAdapter extends RecyclerView.Adapter<SitePickerAdapter.SiteViewHolder> {

    interface OnSiteSelectedListener {
        void onSiteSelected(SitePickerActivity.SiteRecord site);
    }

    interface OnSelectedItemsChangeListener {
        void onSelectedItemsChanged();
    }

    private final int mColorNormal;
    private final int mColorHidden;

    private final SitePickerActivity.SiteList mSites;
    private final LayoutInflater mInflater;
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();
    private boolean mEnableSelection;

    private OnSiteSelectedListener mSiteSelectedListener;
    private OnSelectedItemsChangeListener mSelectionListener;

    static class SiteViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final TextView txtDomain;
        private final WPNetworkImageView imgBlavatar;
        private Boolean isSiteHidden;

        public SiteViewHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDomain = (TextView) view.findViewById(R.id.text_domain);
            imgBlavatar = (WPNetworkImageView) view.findViewById(R.id.image_blavatar);
            isSiteHidden = null;
        }
    }

    public SitePickerAdapter(Context context, @NonNull SitePickerActivity.SiteList sites) {
        super();
        setHasStableIds(true);
        mInflater = LayoutInflater.from(context);
        mSites = sites;
        mColorNormal = context.getResources().getColor(R.color.grey_dark);
        mColorHidden = context.getResources().getColor(R.color.grey);

    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).localId;
    }

    private SitePickerActivity.SiteRecord getItem(int position) {
        return mSites.get(position);
    }

    void setOnSelectedItemsChangeListener(OnSelectedItemsChangeListener listener) {
        mSelectionListener = listener;
    }

    void setOnSiteSelectedListener(OnSiteSelectedListener listener) {
        mSiteSelectedListener = listener;
    }

    boolean isSameList(SitePickerActivity.SiteList sites) {
        return (mSites != null && mSites.isSameList(sites));
    }

    @Override
    public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.site_picker_listitem, parent, false);
        return new SiteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SiteViewHolder holder, final int position) {
        SitePickerActivity.SiteRecord site = getItem(position);
        holder.txtTitle.setText(site.blogName);
        holder.txtDomain.setText(site.hostName);
        holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSiteSelectedListener != null) {
                    mSiteSelectedListener.onSiteSelected(getItem(position));
                }
            }
        });

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.isHidden) {
            holder.isSiteHidden = site.isHidden;
            int textColor = (site.isHidden ? mColorHidden : mColorNormal);
            holder.txtTitle.setTextColor(textColor);
            holder.txtDomain.setTextColor(textColor);
            holder.txtTitle.setTypeface(holder.txtTitle.getTypeface(), site.isHidden ? Typeface.NORMAL : Typeface.BOLD);
            holder.imgBlavatar.setAlpha(site.isHidden ? 0.5f : 1f);
        }
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mSites.size());
    }


    void setEnableSelection(boolean enable) {
        if (enable == mEnableSelection) return;

        mEnableSelection = enable;
        if (mEnableSelection) {
            notifyDataSetChanged();
        } else {
            clearSelection();
        }
    }

    private void clearSelection() {
        if (mSelectedPositions.size() > 0) {
            mSelectedPositions.clear();
            notifyDataSetChanged();
            if (mSelectionListener != null)
                mSelectionListener.onSelectedItemsChanged();
        }
    }

    int getSelectionCount() {
        return mSelectedPositions.size();
    }

    SitePickerActivity.SiteList getSelectedSites() {
        SitePickerActivity.SiteList sites = new SitePickerActivity.SiteList();
        if (!mEnableSelection)
            return sites;

        for (Integer position : mSelectedPositions) {
            if (isValidPosition(position))
                sites.add(mSites.get(position));
        }

        return sites;
    }
}
