package org.wordpress.android.ui.stats;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.main.SitePickerAdapter.SiteRecord;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

public class StatsWidgetConfigureAdapter extends RecyclerView.Adapter<StatsWidgetConfigureAdapter.SiteViewHolder> {
    interface OnSiteClickListener {
        void onSiteClick(SiteRecord site);
    }

    private final int mTextColorNormal;
    private final int mTextColorHidden;

    private static int mBlavatarSz;

    private SiteList mSites = new SiteList();
    private final long mPrimarySiteId;

    private final Drawable mSelectedItemBackground;

    private final LayoutInflater mInflater;

    private boolean mShowHiddenSites = false;
    private boolean mShowSelfHostedSites = true;

    private OnSiteClickListener mSiteSelectedListener;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup mLayoutContainer;
        private final TextView mTxtTitle;
        private final TextView mTxtDomain;
        private final ImageView mImgBlavatar;
        private final View mDivider;
        private Boolean mIsSiteHidden;

        SiteViewHolder(View view) {
            super(view);
            mLayoutContainer = view.findViewById(R.id.layout_container);
            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtDomain = view.findViewById(R.id.text_domain);
            mImgBlavatar = view.findViewById(R.id.image_blavatar);
            mDivider = view.findViewById(R.id.divider);
            mIsSiteHidden = null;

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mSiteSelectedListener != null) {
                        int clickedPosition = getAdapterPosition();
                        mSiteSelectedListener.onSiteClick(getItem(clickedPosition));
                    }
                }
            });
        }
    }

    public StatsWidgetConfigureAdapter(Context context, long primarySiteId) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setHasStableIds(true);

        mPrimarySiteId = primarySiteId;
        mInflater = LayoutInflater.from(context);

        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz);
        mTextColorNormal = context.getResources().getColor(R.color.neutral_70);
        mTextColorHidden = context.getResources().getColor(R.color.neutral_30);

        mSelectedItemBackground =
                new ColorDrawable(context.getResources().getColor(R.color.gray_5));

        loadSites();
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getLocalId();
    }

    private SiteRecord getItem(int position) {
        return mSites.get(position);
    }

    public void setOnSiteClickListener(OnSiteClickListener listener) {
        mSiteSelectedListener = listener;
    }

    @Override
    public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.site_picker_listitem, parent, false);
        return new SiteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SiteViewHolder holder, int position) {
        SiteRecord site = getItem(position);

        holder.mTxtTitle.setText(site.getBlogNameOrHomeURL());
        holder.mTxtDomain.setText(site.getHomeURL());
        mImageManager.load(holder.mImgBlavatar, ImageType.BLAVATAR, site.getBlavatarUrl());

        if (site.getLocalId() == mPrimarySiteId) {
            holder.mLayoutContainer.setBackground(mSelectedItemBackground);
        } else {
            holder.mLayoutContainer.setBackground(null);
        }

        // different styling for visible/hidden sites
        if (holder.mIsSiteHidden == null || holder.mIsSiteHidden != site.isHidden()) {
            holder.mIsSiteHidden = site.isHidden();
            holder.mTxtTitle.setTextColor(site.isHidden() ? mTextColorHidden : mTextColorNormal);
            holder.mTxtTitle
                    .setTypeface(holder.mTxtTitle.getTypeface(), site.isHidden() ? Typeface.NORMAL : Typeface.BOLD);
            holder.mImgBlavatar.setAlpha(site.isHidden() ? 0.5f : 1f);
        }

        // hide the divider for the last item
        boolean isLastItem = (position == getItemCount() - 1);
        holder.mDivider.setVisibility(isLastItem ? View.INVISIBLE : View.VISIBLE);
    }


    private void loadSites() {
        if (mIsTaskRunning) {
            AppLog.w(AppLog.T.UTILS, "site picker > already loading sites");
        } else {
            new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask which loads sites from database and populates the adapter
     */
    private boolean mIsTaskRunning;

    private class LoadSitesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsTaskRunning = true;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsTaskRunning = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<SiteModel> siteModels = getBlogsForCurrentView();
            SiteList sites = new SiteList(siteModels);

            // sort by blog/host
            Collections.sort(sites, new Comparator<SiteRecord>() {
                public int compare(SiteRecord site1, SiteRecord site2) {
                    if (mPrimarySiteId > 0) {
                        if (site1.getSiteId() == mPrimarySiteId) {
                            return -1;
                        } else if (site2.getSiteId() == mPrimarySiteId) {
                            return 1;
                        }
                    }
                    return site1.getBlogNameOrHomeURL().compareToIgnoreCase(site2.getBlogNameOrHomeURL());
                }
            });

            if (mSites == null || !mSites.isSameList(sites)) {
                mSites = sites;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            notifyDataSetChanged();
            mIsTaskRunning = false;
        }

        private List<SiteModel> getBlogsForCurrentView() {
            if (mShowHiddenSites) {
                if (mShowSelfHostedSites) {
                    return mSiteStore.getSites();
                } else {
                    return mSiteStore.getSitesAccessedViaWPComRest();
                }
            } else {
                if (mShowSelfHostedSites) {
                    List<SiteModel> out = mSiteStore.getVisibleSitesAccessedViaWPCom();
                    out.addAll(mSiteStore.getSitesAccessedViaXMLRPC());
                    return out;
                } else {
                    return mSiteStore.getVisibleSitesAccessedViaWPCom();
                }
            }
        }
    }

    static class SiteList extends ArrayList<SiteRecord> {
        SiteList() {
        }

        SiteList(List<SiteModel> sites) {
            if (sites != null) {
                for (SiteModel site : sites) {
                    add(new SiteRecord(site));
                }
            }
        }

        boolean isSameList(SiteList sites) {
            if (sites == null || sites.size() != this.size()) {
                return false;
            }
            int i;
            for (SiteRecord site : sites) {
                i = indexOfSite(site);
                if (i == -1 || this.get(i).isHidden() != site.isHidden()) {
                    return false;
                }
            }
            return true;
        }

        int indexOfSite(SiteRecord site) {
            if (site != null && site.getSiteId() > 0) {
                for (int i = 0; i < size(); i++) {
                    if (site.getSiteId() == this.get(i).getSiteId()) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
