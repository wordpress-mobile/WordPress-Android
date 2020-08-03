package org.wordpress.android.ui.main;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ViewUtilsKt;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

public class SitePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface OnSiteClickListener {
        void onSiteClick(SiteRecord site);

        boolean onSiteLongClick(SiteRecord site);
    }

    interface OnSelectedCountChangedListener {
        void onSelectedCountChanged(int numSelected);
    }

    public interface OnDataLoadedListener {
        void onBeforeLoad(boolean isEmpty);

        void onAfterLoad();
    }

    public interface ViewHolderHandler<T extends RecyclerView.ViewHolder> {
        T onCreateViewHolder(LayoutInflater layoutInflater, ViewGroup parent, boolean attachToRoot);

        void onBindViewHolder(T holder, SiteList sites);
    }

    private final @LayoutRes int mItemLayoutReourceId;

    private static int mBlavatarSz;

    private SiteList mSites = new SiteList();
    private final int mCurrentLocalId;
    private int mSelectedLocalId;

    private final int mSelectedItemBackground;

    private final float mDisabledSiteOpacity;

    private final LayoutInflater mInflater;
    private final HashSet<Integer> mSelectedPositions = new HashSet<>();
    private final ViewHolderHandler mHeaderHandler;
    private final ViewHolderHandler mFooterHandler;

    private boolean mIsMultiSelectEnabled;
    private final boolean mIsInSearchMode;
    private boolean mShowHiddenSites = false;
    private boolean mShowSelfHostedSites = true;
    private String mLastSearch;
    private SiteList mAllSites;
    private ArrayList<Integer> mIgnoreSitesIds;

    private OnSiteClickListener mSiteSelectedListener;
    private OnSelectedCountChangedListener mSelectedCountListener;
    private OnDataLoadedListener mDataLoadedListener;

    private boolean mIsSingleItemSelectionEnabled;
    private int mSelectedItemPos;

    // show recently picked first if there are at least this many blogs
    private static final int RECENTLY_PICKED_THRESHOLD = 11;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup mLayoutContainer;
        private final TextView mTxtTitle;
        private final TextView mTxtDomain;
        private final ImageView mImgBlavatar;
        private final View mDivider;
        private Boolean mIsSiteHidden;
        private final RadioButton mSelectedRadioButton;

        SiteViewHolder(View view) {
            super(view);
            mLayoutContainer = view.findViewById(R.id.layout_container);
            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtDomain = view.findViewById(R.id.text_domain);
            mImgBlavatar = view.findViewById(R.id.image_blavatar);
            mDivider = view.findViewById(R.id.divider);
            mIsSiteHidden = null;
            mSelectedRadioButton = view.findViewById(R.id.radio_selected);
        }
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             OnDataLoadedListener dataLoadedListener,
                             boolean isInEditMode
    ) {
        this(context, itemLayoutResourceId, currentLocalBlogId, lastSearch, isInSearchMode, dataLoadedListener,
                null, null, null, isInEditMode);
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             OnDataLoadedListener dataLoadedListener,
                             ViewHolderHandler<?> headerHandler,
                             ArrayList<Integer> ignoreSitesIds
    ) {
        this(context, itemLayoutResourceId, currentLocalBlogId, lastSearch, isInSearchMode, dataLoadedListener,
                headerHandler, null, ignoreSitesIds, false);
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             OnDataLoadedListener dataLoadedListener,
                             ViewHolderHandler<?> headerHandler,
                             ViewHolderHandler<?> footerHandler,
                             ArrayList<Integer> ignoreSitesIds,
                             boolean isInEditMode
    ) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setHasStableIds(true);

        mLastSearch = StringUtils.notNullStr(lastSearch);
        mAllSites = new SiteList();
        mIsInSearchMode = isInSearchMode;
        mItemLayoutReourceId = itemLayoutResourceId;
        mCurrentLocalId = currentLocalBlogId;
        mSelectedLocalId = mCurrentLocalId;
        mInflater = LayoutInflater.from(context);
        mDataLoadedListener = dataLoadedListener;

        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz);

        TypedValue disabledAlpha = new TypedValue();
        context.getResources().getValue(R.dimen.material_emphasis_disabled, disabledAlpha, true);
        mDisabledSiteOpacity = disabledAlpha.getFloat();
        mSelectedItemBackground = ColorUtils
                .setAlphaComponent(ContextExtensionsKt.getColorFromAttribute(context, R.attr.colorOnSurface),
                        context.getResources().getInteger(R.integer.selected_list_item_opacity));

        mHeaderHandler = headerHandler;
        mFooterHandler = footerHandler;
        mSelectedItemPos = getPositionOffset();

        mIgnoreSitesIds = ignoreSitesIds;

        mShowHiddenSites = isInEditMode; // If site picker is in edit mode, show hidden sites.

        loadSites();
    }

    @Override
    public int getItemCount() {
        return (mHeaderHandler != null ? 1 : 0) + mSites.size() + (mFooterHandler != null ? 1 : 0);
    }

    private int getSitesCount() {
        return mSites.size();
    }

    @Override
    public long getItemId(int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_HEADER) {
            return -1;
        } else if (viewType == VIEW_TYPE_FOOTER) {
            return -2;
        } else {
            return getItem(position).mLocalId;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderHandler != null && position == 0) {
            return VIEW_TYPE_HEADER;
        } else if (mFooterHandler != null && position == getItemCount() - 1) {
            return VIEW_TYPE_FOOTER;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    private SiteRecord getItem(int position) {
        return mSites.get(position - getPositionOffset());
    }

    private int getPositionOffset() {
        return (mHeaderHandler == null ? 0 : 1);
    }

    void setOnSelectedCountChangedListener(OnSelectedCountChangedListener listener) {
        mSelectedCountListener = listener;
    }

    void setOnSiteClickListener(OnSiteClickListener listener) {
        mSiteSelectedListener = listener;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return mHeaderHandler.onCreateViewHolder(mInflater, parent, false);
        } else if (viewType == VIEW_TYPE_FOOTER) {
            return mFooterHandler.onCreateViewHolder(mInflater, parent, false);
        } else {
            View itemView = mInflater.inflate(mItemLayoutReourceId, parent, false);
            return new SiteViewHolder(itemView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, int position) {
        int viewType = getItemViewType(position);

        if (viewType == VIEW_TYPE_HEADER) {
            mHeaderHandler.onBindViewHolder(viewHolder, mSites);
            return;
        }

        if (viewType == VIEW_TYPE_FOOTER) {
            mFooterHandler.onBindViewHolder(viewHolder, mSites);
            return;
        }

        SiteRecord site = getItem(position);

        final SiteViewHolder holder = (SiteViewHolder) viewHolder;
        holder.mTxtTitle.setText(site.getBlogNameOrHomeURL());
        holder.mTxtDomain.setText(site.mHomeURL);
        mImageManager.loadWithRoundedCorners(holder.mImgBlavatar, ImageType.BLAVATAR_ROUNDED, site.mBlavatarUrl);


        if ((site.mLocalId == mCurrentLocalId && !mIsMultiSelectEnabled)
            || (mIsMultiSelectEnabled && isItemSelected(position))) {
            holder.mLayoutContainer.setBackgroundColor(mSelectedItemBackground);
        } else {
            holder.mLayoutContainer.setBackground(null);
        }

        // different styling for visible/hidden sites
        if (holder.mIsSiteHidden == null || holder.mIsSiteHidden != site.mIsHidden) {
            holder.mIsSiteHidden = site.mIsHidden;
            holder.mTxtTitle.setAlpha(site.mIsHidden ? mDisabledSiteOpacity : 1f);
            holder.mImgBlavatar.setAlpha(site.mIsHidden ? mDisabledSiteOpacity : 1f);
        }

        if (holder.mDivider != null) {
            // only show divider after last recent pick
            boolean showDivider = site.mIsRecentPick
                                  && !mIsInSearchMode
                                  && position < getItemCount() - 1
                                  && !getItem(position + 1).mIsRecentPick;
            holder.mDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }

        if (mIsMultiSelectEnabled || mSiteSelectedListener != null) {
            holder.itemView.setOnClickListener(view -> {
                int clickedPosition = holder.getAdapterPosition();
                if (isValidPosition(clickedPosition)) {
                    if (mIsMultiSelectEnabled) {
                        toggleSelection(clickedPosition);
                    } else if (mSiteSelectedListener != null) {

                        mSiteSelectedListener.onSiteClick(getItem(clickedPosition));
                    }
                } else {
                    AppLog.w(AppLog.T.MAIN, "site picker > invalid clicked position " + clickedPosition);
                }
            });

            holder.itemView.setOnLongClickListener(view -> {
                int clickedPosition = holder.getAdapterPosition();
                if (isValidPosition(clickedPosition)) {
                    if (mIsMultiSelectEnabled) {
                        toggleSelection(clickedPosition);
                        return true;
                    } else if (mSiteSelectedListener != null) {
                        return mSiteSelectedListener.onSiteLongClick(getItem(clickedPosition));
                    }
                } else {
                    AppLog.w(AppLog.T.MAIN, "site picker > invalid clicked position " + clickedPosition);
                }
                return false;
            });
            ViewUtilsKt.redirectContextClickToLongPressListener(holder.itemView);
        }

        if (mIsSingleItemSelectionEnabled) {
            if (getSitesCount() <= 1) {
                holder.mSelectedRadioButton.setVisibility(View.GONE);
            } else {
                holder.mSelectedRadioButton.setVisibility(View.VISIBLE);
                holder.mSelectedRadioButton.setChecked(mSelectedItemPos == position);
                holder.mLayoutContainer.setOnClickListener(v -> selectSingleItem(holder.getAdapterPosition()));
            }
        } else {
            if (holder.mSelectedRadioButton != null) {
                holder.mSelectedRadioButton.setVisibility(View.GONE);
            }
        }
    }

    private void selectSingleItem(final int newItemPosition) {
        // clear last selected item
        notifyItemChanged(mSelectedItemPos);
        mSelectedItemPos = newItemPosition;
        // select new item
        notifyItemChanged(mSelectedItemPos);
    }

    public void setSingleItemSelectionEnabled(final boolean enabled) {
        if (enabled != mIsSingleItemSelectionEnabled) {
            mIsSingleItemSelectionEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    public void findAndSelect(final int lastUsedBlogLocalId) {
        int positionInSitesArray = mSites.indexOfSiteId(lastUsedBlogLocalId);
        if (positionInSitesArray != -1) {
            selectSingleItem(positionInSitesArray + getPositionOffset());
        }
    }

    public int getSelectedItemLocalId() {
        return mSites.size() != 0 ? getItem(mSelectedItemPos).mLocalId : -1;
    }

    public int getItemPosByLocalId(int localId) {
        int positionInSitesArray = mSites.indexOfSiteId(localId);

        return mSites.size() != 0 && positionInSitesArray > -1 ? positionInSitesArray : -1;
    }

    String getLastSearch() {
        return mLastSearch;
    }

    void setLastSearch(String lastSearch) {
        mLastSearch = lastSearch;
    }

    boolean getIsInSearchMode() {
        return mIsInSearchMode;
    }

    void searchSites(String searchText) {
        mLastSearch = searchText;
        mSites = filteredSitesByText(mAllSites);

        notifyDataSetChanged();
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < mSites.size());
    }

    /*
     * called when the user chooses to edit the visibility of wp.com blogs
     */
    void setEnableEditMode(boolean enable, HashSet<Integer> selectedPositions) {
        if (mIsMultiSelectEnabled == enable) {
            return;
        }

        if (enable) {
            mShowHiddenSites = true;
            mShowSelfHostedSites = false;
        } else {
            mShowHiddenSites = false;
            mShowSelfHostedSites = true;
        }

        mIsMultiSelectEnabled = enable;

        // If the selectedPositions value that is passed from the activity isn't empty,
        // then we add that to the Adapter's mSelectedPositions.
        // Otherwise, reset the selected positions.
        if (!selectedPositions.isEmpty()) {
            mSelectedPositions.addAll(selectedPositions);
        } else {
            mSelectedPositions.clear();
        }

        loadSites();
    }

    int getNumSelected() {
        return mSelectedPositions.size();
    }

    int getNumHiddenSelected() {
        int numHidden = 0;
        for (Integer i : mSelectedPositions) {
            if (isValidPosition(i) && mSites.get(i).mIsHidden) {
                numHidden++;
            }
        }
        return numHidden;
    }

    int getNumVisibleSelected() {
        int numVisible = 0;
        for (Integer i : mSelectedPositions) {
            if (i < mSites.size() && !mSites.get(i).mIsHidden) {
                numVisible++;
            }
        }
        return numVisible;
    }

    private void toggleSelection(int position) {
        setItemSelected(position, !isItemSelected(position));
    }

    private boolean isItemSelected(int position) {
        return mSelectedPositions.contains(position);
    }

    private void setItemSelected(int position, boolean isSelected) {
        if (isItemSelected(position) == isSelected) {
            return;
        }

        if (isSelected) {
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
        if (mSelectedPositions.size() == mSites.size()) {
            return;
        }

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
        if (mSelectedPositions.size() == 0) {
            return;
        }

        mSelectedPositions.clear();
        notifyDataSetChanged();

        if (mSelectedCountListener != null) {
            mSelectedCountListener.onSelectedCountChanged(getNumSelected());
        }
    }

    private SiteList getSelectedSites() {
        SiteList sites = new SiteList();
        if (!mIsMultiSelectEnabled) {
            return sites;
        }

        for (Integer position : mSelectedPositions) {
            if (isValidPosition(position)) {
                sites.add(mSites.get(position));
            }
        }

        return sites;
    }

    public HashSet<Integer> getSelectedPositions() {
        return mSelectedPositions;
    }

    SiteList getHiddenSites() {
        SiteList hiddenSites = new SiteList();
        for (SiteRecord site : mSites) {
            if (site.mIsHidden) {
                hiddenSites.add(site);
            }
        }

        return hiddenSites;
    }

    Set<SiteRecord> setVisibilityForSelectedSites(boolean makeVisible) {
        SiteList sites = getSelectedSites();
        Set<SiteRecord> changeSet = new HashSet<>();
        if (sites != null && sites.size() > 0) {
            ArrayList<Integer> recentIds = AppPrefs.getRecentlyPickedSiteIds();
            int currentSiteId = AppPrefs.getSelectedSite();
            for (SiteRecord site : sites) {
                int index = mAllSites.indexOfSite(site);
                if (index > -1) {
                    SiteRecord siteRecord = mAllSites.get(index);
                    if (siteRecord.mIsHidden == makeVisible) {
                        changeSet.add(siteRecord);
                        siteRecord.mIsHidden = !makeVisible;
                        if (!makeVisible
                            && siteRecord.mLocalId != currentSiteId
                            && recentIds.contains(siteRecord.mLocalId)) {
                            AppPrefs.removeRecentlyPickedSiteId(siteRecord.mLocalId);
                        }
                    }
                }
            }

            if (!changeSet.isEmpty()) {
                notifyDataSetChanged();
            }
        }

        return changeSet;
    }

    void loadSites() {
        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private SiteList filteredSitesByTextIfInSearchMode(SiteList sites) {
        if (!mIsInSearchMode) {
            return sites;
        } else {
            return filteredSitesByText(sites);
        }
    }

    private SiteList filteredSitesByText(SiteList sites) {
        SiteList filteredSiteList = new SiteList();

        for (int i = 0; i < sites.size(); i++) {
            SiteRecord record = sites.get(i);
            String siteNameLowerCase = record.mBlogName.toLowerCase(Locale.getDefault());
            String hostNameLowerCase = record.mHomeURL.toLowerCase(Locale.ROOT);

            if (siteNameLowerCase.contains(mLastSearch.toLowerCase(Locale.getDefault())) || hostNameLowerCase
                    .contains(mLastSearch.toLowerCase(Locale.ROOT))) {
                filteredSiteList.add(record);
            }
        }

        return filteredSiteList;
    }

    /*
     * AsyncTask which loads sites from database and populates the adapter
     */
    private class LoadSitesTask extends AsyncTask<Void, Void, SiteList[]> {
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
        protected SiteList[] doInBackground(Void... params) {
            List<SiteModel> siteModels = getBlogsForCurrentView();

            if (mIgnoreSitesIds != null) {
                List<SiteModel> unignoredSiteModels = new ArrayList<>();
                for (SiteModel site : siteModels) {
                    if (!mIgnoreSitesIds.contains(site.getId())) {
                        unignoredSiteModels.add(site);
                    }
                }
                siteModels = unignoredSiteModels;
            }

            SiteList sites = new SiteList(siteModels);

            // sort primary blog to the top, otherwise sort by blog/host
            final long primaryBlogId = mAccountStore.getAccount().getPrimarySiteId();
            Collections.sort(sites, (site1, site2) -> {
                if (primaryBlogId > 0 && !mIsInSearchMode) {
                    if (site1.mSiteId == primaryBlogId) {
                        return -1;
                    } else if (site2.mSiteId == primaryBlogId) {
                        return 1;
                    }
                }
                return site1.getBlogNameOrHomeURL().compareToIgnoreCase(site2.getBlogNameOrHomeURL());
            });

            // flag recently-picked sites and move them to the top if there are enough sites and
            // the user isn't searching
            if (!mIsInSearchMode && sites.size() >= RECENTLY_PICKED_THRESHOLD) {
                ArrayList<Integer> pickedIds = AppPrefs.getRecentlyPickedSiteIds();
                for (int i = pickedIds.size() - 1; i > -1; i--) {
                    int thisId = pickedIds.get(i);
                    int indexOfSite = sites.indexOfSiteId(thisId);
                    if (indexOfSite > -1) {
                        SiteRecord site = sites.remove(indexOfSite);
                        site.mIsRecentPick = true;
                        sites.add(0, site);
                    }
                }
            }

            if (mSites == null || !mSites.isSameList(sites)) {
                SiteList allSites = (SiteList) sites.clone();
                SiteList filteredSites = filteredSitesByTextIfInSearchMode(sites);

                return new SiteList[]{allSites, filteredSites};
            }

            return null;
        }

        @Override
        protected void onPostExecute(SiteList[] updatedSiteLists) {
            if (updatedSiteLists != null) {
                mAllSites = updatedSiteLists[0];
                mSites = updatedSiteLists[1];
                notifyDataSetChanged();
            }
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
    public static class SiteRecord {
        private final int mLocalId;
        private final long mSiteId;
        private final String mBlogName;
        private final String mHomeURL;
        private final String mBlavatarUrl;
        private boolean mIsHidden;
        private boolean mIsRecentPick;

        SiteRecord(SiteModel siteModel) {
            mLocalId = siteModel.getId();
            mSiteId = siteModel.getSiteId();
            mBlogName = SiteUtils.getSiteNameOrHomeURL(siteModel);
            mHomeURL = SiteUtils.getHomeURLOrHostName(siteModel);
            mBlavatarUrl = SiteUtils.getSiteIconUrl(siteModel, mBlavatarSz);
            mIsHidden = !siteModel.isVisible();
        }

        String getBlogNameOrHomeURL() {
            if (TextUtils.isEmpty(mBlogName)) {
                return mHomeURL;
            }
            return mBlogName;
        }

        public int getLocalId() {
            return mLocalId;
        }

        public boolean isHidden() {
            return mIsHidden;
        }

        public void setHidden(boolean hidden) {
            mIsHidden = hidden;
        }

        public String getHomeURL() {
            return mHomeURL;
        }

        public String getBlavatarUrl() {
            return mBlavatarUrl;
        }

        public long getSiteId() {
            return mSiteId;
        }
    }

    public static class SiteList extends ArrayList<SiteRecord> {
        SiteList() {
        }

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
            for (SiteRecord site : sites) {
                i = indexOfSite(site);
                if (i == -1
                    || this.get(i).mIsHidden != site.mIsHidden
                    || this.get(i).mIsRecentPick != site.mIsRecentPick) {
                    return false;
                }
            }
            return true;
        }

        int indexOfSite(SiteRecord site) {
            if (site != null && site.mSiteId > 0) {
                for (int i = 0; i < size(); i++) {
                    if (site.mSiteId == this.get(i).mSiteId) {
                        return i;
                    }
                }
            }
            return -1;
        }

        int indexOfSiteId(int localId) {
            for (int i = 0; i < size(); i++) {
                if (localId == this.get(i).mLocalId) {
                    return i;
                }
            }
            return -1;
        }
    }
}
