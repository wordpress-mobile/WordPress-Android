package org.wordpress.android.ui.stats;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

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

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup layoutContainer;
        private final TextView txtTitle;
        private final TextView txtDomain;
        private final WPNetworkImageView imgBlavatar;
        private final View divider;
        private Boolean isSiteHidden;

        public SiteViewHolder(View view) {
            super(view);
            layoutContainer = (ViewGroup) view.findViewById(R.id.layout_container);
            txtTitle = (TextView) view.findViewById(R.id.text_title);
            txtDomain = (TextView) view.findViewById(R.id.text_domain);
            imgBlavatar = (WPNetworkImageView) view.findViewById(R.id.image_blavatar);
            divider = view.findViewById(R.id.divider);
            isSiteHidden = null;

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
        mTextColorNormal = context.getResources().getColor(R.color.grey_dark);
        mTextColorHidden = context.getResources().getColor(R.color.grey);

        mSelectedItemBackground = new ColorDrawable(context.getResources().getColor(R.color.grey_lighten_20_translucent_50));

        loadSites();
    }

    @Override
    public int getItemCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).localId;
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

        holder.txtTitle.setText(site.getBlogNameOrHomeURL());
        holder.txtDomain.setText(site.homeURL);
        holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);

        if (site.localId == mPrimarySiteId) {
            holder.layoutContainer.setBackgroundDrawable(mSelectedItemBackground);
        } else {
            holder.layoutContainer.setBackgroundDrawable(null);
        }

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.isHidden) {
            holder.isSiteHidden = site.isHidden;
            holder.txtTitle.setTextColor(site.isHidden ? mTextColorHidden : mTextColorNormal);
            holder.txtTitle.setTypeface(holder.txtTitle.getTypeface(), site.isHidden ? Typeface.NORMAL : Typeface.BOLD);
            holder.imgBlavatar.setAlpha(site.isHidden ? 0.5f : 1f);
        }

        // hide the divider for the last item
        boolean isLastItem = (position == getItemCount() - 1);
        holder.divider.setVisibility(isLastItem ?  View.INVISIBLE : View.VISIBLE);
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
                        if (site1.blogId == mPrimarySiteId) {
                            return -1;
                        } else if (site2.blogId == mPrimarySiteId) {
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
                    // all self-hosted sites and all wp.com sites
                    return mSiteStore.getSites();
                } else {
                    // only wp.com and jetpack sites
                    return mSiteStore.getWPComAndJetpackSites();
                }
            } else {
                if (mShowSelfHostedSites) {
                    // all self-hosted sites plus visible wp.com and jetpack sites
                    List<SiteModel> out = mSiteStore.getVisibleWPComAndJetpackSites();
                    out.addAll(mSiteStore.getSelfHostedSites());
                    return out;
                } else {
                    // only visible wp.com and jetpack sites
                    return mSiteStore.getVisibleWPComAndJetpackSites();
                }
            }
        }
    }

    /**
     * SiteRecord is a simplified version of the full account (blog) record
     */
     static class SiteRecord {
        final int localId;
        final long blogId;
        final String blogName;
        final String homeURL;
        final String url;
        final String blavatarUrl;
        final boolean isDotComOrJetpack;
        final boolean isHidden;

        SiteRecord(SiteModel site) {
            localId = site.getId();
            blogId = site.getSiteId();
            blogName = SiteUtils.getSiteNameOrHomeURL(site);
            homeURL = SiteUtils.getHomeURLOrHostName(site);
            url = site.getUrl();
            blavatarUrl = SiteUtils.getSiteIconUrl(site, mBlavatarSz);
            isDotComOrJetpack = SiteUtils.isAccessibleViaWPComAPI(site);
            isHidden = !site.isVisible();
        }

        String getBlogNameOrHomeURL() {
            if (TextUtils.isEmpty(blogName)) {
                return homeURL;
            }
            return blogName;
        }
    }

   static class SiteList extends ArrayList<SiteRecord> {
        SiteList() { }
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
            for (SiteRecord site: sites) {
                i = indexOfSite(site);
                if (i == -1 || this.get(i).isHidden != site.isHidden) {
                    return false;
                }
            }
            return true;
        }

        int indexOfSite(SiteRecord site) {
            if (site != null && site.blogId > 0) {
                for (int i = 0; i < size(); i++) {
                    if (site.blogId == this.get(i).blogId) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
