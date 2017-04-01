package org.wordpress.android.ui.main;

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
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class SitePickerAdapter extends RecyclerView.Adapter<SitePickerAdapter.SiteViewHolder> {

    interface OnSiteClickListener {
        void onSiteClick(SiteRecord site);
    }

    interface OnSelectedCountChangedListener {
        void onSelectedCountChanged(int numSelected);
    }

    interface OnDataLoadedListener {
        void onBeforeLoad(boolean isEmpty);
        void onAfterLoad();
    }

    private final int mTextColorNormal;
    private final int mTextColorHidden;

    private static int mBlavatarSz;

    private SiteList mSites = new SiteList();
    private final int mCurrentLocalId;

    private final Drawable mSelectedItemBackground;

    private final LayoutInflater mInflater;
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();

    private boolean mMultiSelectEnabledEh;
    private final boolean mInSearchModeEh;
    private boolean mShowHiddenSites = false;
    private boolean mShowSelfHostedSites = true;
    private String mLastSearch;
    private SiteList mAllSites;

    private OnSiteClickListener mSiteSelectedListener;
    private OnSelectedCountChangedListener mSelectedCountListener;
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

    public  SitePickerAdapter(Context context,
                              int currentLocalBlogId,
                              String lastSearch,
                              boolean inSearchModeEh,
                              OnDataLoadedListener dataLoadedListener) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setHasStableIds(true);

        mLastSearch = StringUtils.notNullStr(lastSearch);
        mAllSites = new SiteList();
        mInSearchModeEh = inSearchModeEh;
        mCurrentLocalId = currentLocalBlogId;
        mInflater = LayoutInflater.from(context);
        mDataLoadedListener = dataLoadedListener;

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

    void setOnSelectedCountChangedListener(OnSelectedCountChangedListener listener) {
        mSelectedCountListener = listener;
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
    public void onBindViewHolder(final SiteViewHolder holder, int position) {
        SiteRecord site = getItem(position);

        holder.txtTitle.setText(site.getBlogNameOrHomeURL());
        holder.txtDomain.setText(site.homeURL);
        holder.imgBlavatar.setImageUrl(site.blavatarUrl, WPNetworkImageView.ImageType.BLAVATAR);

        if (site.localId == mCurrentLocalId || (mMultiSelectEnabledEh && itemSelectedEh(position))) {
            holder.layoutContainer.setBackgroundDrawable(mSelectedItemBackground);
        } else {
            holder.layoutContainer.setBackgroundDrawable(null);
        }

        // different styling for visible/hidden sites
        if (holder.isSiteHidden == null || holder.isSiteHidden != site.hiddenEh) {
            holder.isSiteHidden = site.hiddenEh;
            holder.txtTitle.setTextColor(site.hiddenEh ? mTextColorHidden : mTextColorNormal);
            holder.txtTitle.setTypeface(holder.txtTitle.getTypeface(), site.hiddenEh ? Typeface.NORMAL : Typeface.BOLD);
            holder.imgBlavatar.setAlpha(site.hiddenEh ? 0.5f : 1f);
        }

        // only show divider after last recent pick
        boolean showDivider = site.isRecentPick
                && !mIsInSearchMode
                && position < getItemCount() - 1
                && !getItem(position + 1).recentPickEh;
        holder.divider.setVisibility(showDivider ?  View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int clickedPosition = holder.getAdapterPosition();
                if (validPositionEh(clickedPosition)) {
                    if (mMultiSelectEnabledEh) {
                        toggleSelection(clickedPosition);
                    } else if (mSiteSelectedListener != null) {
                        mSiteSelectedListener.onSiteClick(getItem(clickedPosition));
                    }
                } else {
                    AppLog.w(AppLog.T.MAIN, "site picker > invalid clicked position " + clickedPosition);
                }
            }
        });
    }

    public String getLastSearch() {
        return mLastSearch;
    }

    public void setLastSearch(String lastSearch) {
        mLastSearch = lastSearch;
    }

    public boolean getIsInSearchMode() {
        return mInSearchModeEh;
    }

    public void searchSites(String searchText) {
        mLastSearch = searchText;
        mSites = filteredSitesByText(mAllSites);

        notifyDataSetChanged();
    }

    private boolean validPositionEh(int position) {
        return (position >= 0 && position < mSites.size());
    }

    /*
     * called when the user chooses to edit the visibility of wp.com blogs
     */
    void setEnableEditMode(boolean enable) {
        if (mMultiSelectEnabledEh == enable) return;

        if (enable) {
            mShowHiddenSites = true;
            mShowSelfHostedSites = false;
        } else {
            mShowHiddenSites = false;
            mShowSelfHostedSites = true;
        }

        mMultiSelectEnabledEh = enable;
        mSelectedPositions.clear();

        loadSites();
    }

    int getNumSelected() {
        return mSelectedPositions.size();
    }

    int getNumHiddenSelected() {
        int numHidden = 0;
        for (Integer i: mSelectedPositions) {
            if (mSites.get(i).hiddenEh) {
                numHidden++;
            }
        }
        return numHidden;
    }

    int getNumVisibleSelected() {
        int numVisible = 0;
        for (Integer i: mSelectedPositions) {
            if (!mSites.get(i).hiddenEh) {
                numVisible++;
            }
        }
        return numVisible;
    }

    private void toggleSelection(int position) {
        setItemSelected(position, !itemSelectedEh(position));
    }

    private boolean itemSelectedEh(int position) {
        return mSelectedPositions.contains(position);
    }

    private void setItemSelected(int position, boolean selectedEh) {
        if (itemSelectedEh(position) == selectedEh) {
            return;
        }

        if (selectedEh) {
            mSelectedPositions.add(position);
        } else {
            mSelectedPositions.remove(position);
        }
        notifyItemChanged(position);

        if (mSelectedCountListener != null) {
            mSelectedCountListener.onSelectedCountChanged(getNumSelected());
        }
    }

    void selectAll() {
        if (mSelectedPositions.size() == mSites.size()) return;

        mSelectedPositions.clear();
        for (int i = 0; i < mSites.size(); i++) {
            mSelectedPositions.add(i);
        }
        notifyDataSetChanged();

        if (mSelectedCountListener != null) {
            mSelectedCountListener.onSelectedCountChanged(getNumSelected());
        }
    }

    void deselectAll() {
        if (mSelectedPositions.size() == 0) return;

        mSelectedPositions.clear();
        notifyDataSetChanged();

        if (mSelectedCountListener != null) {
            mSelectedCountListener.onSelectedCountChanged(getNumSelected());
        }
    }

    private SiteList getSelectedSites() {
        SiteList sites = new SiteList();
        if (!mMultiSelectEnabledEh) {
            return sites;
        }

        for (Integer position : mSelectedPositions) {
            if (validPositionEh(position))
                sites.add(mSites.get(position));
        }

        return sites;
    }

    SiteList getHiddenSites() {
        SiteList hiddenSites = new SiteList();
        for (SiteRecord site: mSites) {
            if (site.hiddenEh) {
                hiddenSites.add(site);
            }
        }

        return hiddenSites;
    }

    Set<SiteRecord> setVisibilityForSelectedSites(boolean makeVisible) {
        SiteList sites = getSelectedSites();
        Set<SiteRecord> siteRecordSet = new HashSet<>();
        if (sites != null && sites.size() > 0) {
            for (SiteRecord site: sites) {
                int index = mAllSites.indexOfSite(site);
                if (index > -1) {
                    SiteRecord siteRecord = mAllSites.get(index);
                    if (siteRecord.hiddenEh == makeVisible) {
                        // add it to change set
                        siteRecordSet.add(siteRecord);
                    }
                    siteRecord.hiddenEh = !makeVisible;
                }
            }
        }
        notifyDataSetChanged();
        return siteRecordSet;
    }

    void loadSites() {
        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private SiteList filteredSitesByTextIfInSearchMode(SiteList sites) {
        if (!mInSearchModeEh) {
            return sites;
        } else {
            return filteredSitesByText(sites);
        }
    }

    private SiteList filteredSitesByText(SiteList sites) {
        SiteList filteredSiteList = new SiteList();

        for (int i = 0; i < sites.size(); i++) {
            SiteRecord record = sites.get(i);
            String siteNameLowerCase = record.blogName.toLowerCase();
            String hostNameLowerCase = record.homeURL.toLowerCase();

            if (siteNameLowerCase.contains(mLastSearch.toLowerCase()) || hostNameLowerCase.contains(mLastSearch.toLowerCase())) {
                filteredSiteList.add(record);
            }
        }

        return filteredSiteList;
    }

    /*
     * AsyncTask which loads sites from database and populates the adapter
     */
    private boolean mTaskRunningEh;
    private class LoadSitesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mTaskRunningEh = true;
            if (mDataLoadedListener != null) {
                boolean isEmpty = mSites == null || mSites.size() == 0;
                mDataLoadedListener.onBeforeLoad(isEmpty);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mTaskRunningEh = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<SiteModel> siteModels;
            if (mInSearchModeEh) {
                siteModels = mSiteStore.getSites();
            } else {
                siteModels = getBlogsForCurrentView();
            }

            SiteList sites = new SiteList(siteModels);

            // sort primary blog to the top, otherwise sort by blog/host
            final long primaryBlogId = mAccountStore.getAccount().getPrimarySiteId();
            Collections.sort(sites, new Comparator<SiteRecord>() {
                public int compare(SiteRecord site1, SiteRecord site2) {
                    if (primaryBlogId > 0 && !mInSearchModeEh) {
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
            if (!mInSearchModeEh && sites.size() >= RECENTLY_PICKED_THRESHOLD) {
                ArrayList<Integer> pickedIds = AppPrefs.getRecentlyPickedSiteIds();
                for (int i = pickedIds.size() - 1; i > -1; i--) {
                    int thisId = pickedIds.get(i);
                    int indexOfSite = sites.indexOfSiteId(thisId);
                    if (indexOfSite > -1) {
                        SiteRecord site = sites.remove(indexOfSite);
                        site.recentPickEh = true;
                        sites.add(0, site);
                    }
                }
            }

            if (mSites == null || !mSites.sameListEh(sites)) {
                mAllSites = (SiteList) sites.clone();
                mSites = filteredSitesByTextIfInSearchMode(sites);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            notifyDataSetChanged();
            mTaskRunningEh = false;
            if (mDataLoadedListener != null) {
                mDataLoadedListener.onAfterLoad();
            }
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
                    // only visible wp.com and jetpack blogs
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
        final long siteId;
        final String blogName;
        final String homeURL;
        final String url;
        final String blavatarUrl;
        boolean hiddenEh;
        boolean recentPickEh;

        SiteRecord(SiteModel siteModel) {
            localId = siteModel.getId();
            siteId = siteModel.getSiteId();
            blogName = SiteUtils.getSiteNameOrHomeURL(siteModel);
            homeURL = SiteUtils.getHomeURLOrHostName(siteModel);
            url = siteModel.getUrl();
            blavatarUrl = SiteUtils.getSiteIconUrl(siteModel, mBlavatarSz);
            hiddenEh = !siteModel.isVisible();
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

        boolean sameListEh(SiteList sites) {
            if (sites == null || sites.size() != this.size()) {
                return false;
            }
            int i;
            for (SiteRecord site: sites) {
                i = indexOfSite(site);
                if (i == -1
                        || this.get(i).hiddenEh != site.isHidden
                        || this.get(i).recentPickEh != site.recentPickEh) {
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
