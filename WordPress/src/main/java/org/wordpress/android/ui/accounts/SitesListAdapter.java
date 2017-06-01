package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.graphics.Typeface;
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
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;

public class SitesListAdapter extends RecyclerView.Adapter<SitesListAdapter.SiteViewHolder> {

    interface OnDataLoadedListener {
        void onBeforeLoad(boolean isEmpty);
        void onAfterLoad();
    }

    private final int mTextColorNormal;
    private final int mTextColorHidden;

    private static int mBlavatarSz;

    private SiteList mSites = new SiteList();

    private final LayoutInflater mInflater;

    private boolean mShowHiddenSites = false;
    private boolean mShowSelfHostedSites = true;
    private SiteList mAllSites;

    private OnDataLoadedListener mDataLoadedListener;

    // show recently picked first if there are at least this many blogs
    private static final int RECENTLY_PICKED_THRESHOLD = 15;

    @Inject AccountStore mAccountStore;
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
        }
    }

    public SitesListAdapter(Context context,
                              OnDataLoadedListener dataLoadedListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setHasStableIds(true);

        mAllSites = new SiteList();
        mInflater = LayoutInflater.from(context);
        mDataLoadedListener = dataLoadedListener;

        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz);
        mTextColorNormal = context.getResources().getColor(R.color.grey_dark);
        mTextColorHidden = context.getResources().getColor(R.color.grey);

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

    @Override
    public SiteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.site_picker_listitem, parent, false);
        return new SiteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final SiteViewHolder holder, int position) {
        SiteRecord site = getItem(position);

        holder.txtTitle.setText(site.getBlogNameOrHomeURL());
        holder.txtDomain.setText(site.homeURL);
        holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.isHidden) {
            holder.isSiteHidden = site.isHidden;
            holder.txtTitle.setTextColor(site.isHidden ? mTextColorHidden : mTextColorNormal);
            holder.txtTitle.setTypeface(holder.txtTitle.getTypeface(), site.isHidden ? Typeface.NORMAL : Typeface.BOLD);
            holder.imgBlavatar.setAlpha(site.isHidden ? 0.5f : 1f);
        }

        // only show divider after last recent pick
        boolean showDivider = site.isRecentPick
                && position < getItemCount() - 1
                && !getItem(position + 1).isRecentPick;
        holder.divider.setVisibility(showDivider ?  View.VISIBLE : View.GONE);
    }

    void loadSites() {
        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /*
     * AsyncTask which loads sites from database and populates the adapter
     */
    private class LoadSitesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mDataLoadedListener != null) {
                boolean isEmpty = mSites == null || mSites.size() == 0;
                mDataLoadedListener.onBeforeLoad(isEmpty);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<SiteModel> siteModels;
            siteModels = getBlogsForCurrentView();

            SiteList sites = new SiteList(siteModels);

            // sort primary blog to the top, otherwise sort by blog/host
            final long primaryBlogId = mAccountStore.getAccount().getPrimarySiteId();
            Collections.sort(sites, new Comparator<SiteRecord>() {
                public int compare(SiteRecord site1, SiteRecord site2) {
                    if (primaryBlogId > 0) {
                        if (site1.siteId == primaryBlogId) {
                            return -1;
                        } else if (site2.siteId == primaryBlogId) {
                            return 1;
                        }
                    }
                    return site1.getBlogNameOrHomeURL().compareToIgnoreCase(site2.getBlogNameOrHomeURL());
                }
            });

            // flag recently-picked sites and move them to the top if there are enough sites and
            // the user isn't searching
            if (sites.size() >= RECENTLY_PICKED_THRESHOLD) {
                ArrayList<Integer> pickedIds = AppPrefs.getRecentlyPickedSiteIds();
                for (int i = pickedIds.size() - 1; i > -1; i--) {
                    int thisId = pickedIds.get(i);
                    int indexOfSite = sites.indexOfSiteId(thisId);
                    if (indexOfSite > -1) {
                        SiteRecord site = sites.remove(indexOfSite);
                        site.isRecentPick = true;
                        sites.add(0, site);
                    }
                }
            }

            if (mSites == null || !mSites.isSameList(sites)) {
                mAllSites = (SiteList) sites.clone();
                mSites = sites;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            notifyDataSetChanged();
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onAfterLoad();
            }
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

    /**
     * SiteRecord is a simplified version of the full account (blog) record
     */
     static class SiteRecord {
        final int localId;
        final long siteId;
        final String blogName;
        final String homeURL;
        final String url;
        final String blavatarUrl;
        boolean isHidden;
        boolean isRecentPick;

        SiteRecord(SiteModel siteModel) {
            localId = siteModel.getId();
            siteId = siteModel.getSiteId();
            blogName = SiteUtils.getSiteNameOrHomeURL(siteModel);
            homeURL = SiteUtils.getHomeURLOrHostName(siteModel);
            url = siteModel.getUrl();
            blavatarUrl = SiteUtils.getSiteIconUrl(siteModel, mBlavatarSz);
            isHidden = !siteModel.isVisible();
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
        SiteList(List<SiteModel> siteModels) {
            if (siteModels != null) {
                for (SiteModel siteModel : siteModels) {
                    add(new SiteRecord(siteModel));
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
                if (i == -1
                        || this.get(i).isHidden != site.isHidden
                        || this.get(i).isRecentPick != site.isRecentPick) {
                    return false;
                }
            }
            return true;
        }

        int indexOfSite(SiteRecord site) {
            if (site != null && site.siteId > 0) {
                for (int i = 0; i < size(); i++) {
                    if (site.siteId == this.get(i).siteId) {
                        return i;
                    }
                }
            }
            return -1;
        }

        int indexOfSiteId(int localId) {
            for (int i = 0; i < size(); i++) {
                if (localId == this.get(i).localId) {
                    return i;
                }
            }
            return -1;
        }
    }

    /*
     * same as Long.compare() which wasn't added until API 19
     */
    private static int compareTimestamps(long timestamp1, long timestamp2) {
        return timestamp1 < timestamp2 ? -1 : (timestamp1 == timestamp2 ? 0 : 1);
    }
}
