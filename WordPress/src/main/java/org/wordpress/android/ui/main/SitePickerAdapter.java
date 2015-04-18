package org.wordpress.android.ui.main;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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

    interface OnSiteClickListener {
        void onSiteClick(SitePickerActivity.SiteRecord site);
    }
    interface OnSiteLongClickListener {
        void onSiteLongClick(SitePickerActivity.SiteRecord site);
    }
    interface OnSelectionCountChangeListener {
        void onSelectedCountChanged(int numSelected);
    }

    private final int mTextColorNormal;
    private final int mTextColorHidden;
    private final Drawable mSelectedItemBackground;

    private final SitePickerActivity.SiteList mSites;
    private final LayoutInflater mInflater;
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();
    private boolean mEnableSelection;

    private OnSiteClickListener mSiteSelectedListener;
    private OnSiteLongClickListener mSiteLongPressListener;
    private OnSelectionCountChangeListener mSelectionListener;

    static class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup layoutContainer;
        private final TextView txtTitle;
        private final TextView txtDomain;
        private final WPNetworkImageView imgBlavatar;
        private Boolean isSiteHidden;

        public SiteViewHolder(View view) {
            super(view);
            layoutContainer = (ViewGroup) view.findViewById(R.id.layout_container);
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
        mTextColorNormal = context.getResources().getColor(R.color.grey_dark);
        mTextColorHidden = context.getResources().getColor(R.color.grey);
        mSelectedItemBackground = new ColorDrawable(context.getResources().getColor(R.color.translucent_grey_lighten_10));
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

    void setOnSelectionCountChangeListener(OnSelectionCountChangeListener listener) {
        mSelectionListener = listener;
    }

    void setOnSiteClickListener(OnSiteClickListener listener) {
        mSiteSelectedListener = listener;
    }

    void setOnSiteLongClickListener(OnSiteLongClickListener listener) {
        mSiteLongPressListener = listener;
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
                    mSiteSelectedListener.onSiteClick(getItem(position));
                }
                if (mEnableSelection) {
                    toggleSelection(position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mSiteLongPressListener != null) {
                    mSiteLongPressListener.onSiteLongClick(getItem(position));
                }
                if (!mEnableSelection) {
                    setEnableSelection(true);
                    setItemSelected(position, true);
                }
                return true;
            }
        });

        boolean isSelected = mEnableSelection && isItemSelected(position);
        holder.layoutContainer.setBackgroundDrawable(isSelected ? mSelectedItemBackground : null);

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.isHidden) {
            holder.isSiteHidden = site.isHidden;
            holder.txtTitle.setTextColor(site.isHidden ? mTextColorHidden : mTextColorNormal);
            holder.txtDomain.setTextColor(site.isHidden ? mTextColorHidden : mTextColorNormal);
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
                mSelectionListener.onSelectedCountChanged(getSelectionCount());
        }
    }

    int getSelectionCount() {
        return mSelectedPositions.size();
    }

    void toggleSelection(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    void setItemSelected(int position, boolean isSelected) {
        if (isItemSelected(position) == isSelected) {
            return;
        }

        if (isSelected) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }

        notifyItemChanged(position);

        if (mSelectionListener != null) {
            mSelectionListener.onSelectedCountChanged(getSelectionCount());
        }
    }

    SitePickerActivity.SiteList getSelectedSites() {
        SitePickerActivity.SiteList sites = new SitePickerActivity.SiteList();
        if (!mEnableSelection) {
            return sites;
        }

        for (Integer position : mSelectedPositions) {
            if (isValidPosition(position))
                sites.add(mSites.get(position));
        }

        return sites;
    }
}
