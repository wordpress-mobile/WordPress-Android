package org.wordpress.android.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.main.utils.SiteRecordUtil;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.extensions.ContextExtensionsKt;
import org.wordpress.android.util.extensions.ViewExtensionsKt;
import org.wordpress.android.util.image.ImageManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class SitePickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnSiteClickListener {
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

        void onBindViewHolder(T holder, List<SiteRecord> sites);
    }

    /**
     * Represents the available SitePicker modes
     */
    public enum SitePickerMode {
        DEFAULT_MODE,
        REBLOG_SELECT_MODE, // used when the site is not selected yet (first step of reblogging)
        REBLOG_CONTINUE_MODE, // used when the site is selected (second step of reblogging)
        BLOGGING_PROMPTS_MODE, // used when choose a site for prompts
        SIMPLE_MODE; // used when select a non-self-hosted site for purchasing a domain

        public boolean isReblogMode() {
            return this == REBLOG_SELECT_MODE || this == REBLOG_CONTINUE_MODE;
        }

        public boolean isBloggingPromptsMode() {
            return this == BLOGGING_PROMPTS_MODE;
        }
    }

    private final @LayoutRes int mItemLayoutReourceId;

    static int mBlavatarSz;

    @NonNull private List<SiteRecord> mSites = new ArrayList<>();
    private final int mCurrentLocalId;
    private int mSelectedLocalId;

    private final int mSelectedItemBackground;

    private final float mDisabledSiteOpacity;

    private final LayoutInflater mInflater;
    @NonNull private final HashSet<Integer> mSelectedPositions = new HashSet<>();
    @Nullable private final ViewHolderHandler mHeaderHandler;
    @Nullable private final ViewHolderHandler mFooterHandler;

    private boolean mIsMultiSelectEnabled;
    private final boolean mIsInSearchMode;
    private boolean mShowHiddenSites = false;
    private final boolean mShowAndReturn;
    private boolean mShowSelfHostedSites = true;
    private String mLastSearch;
    private List<SiteRecord> mAllSites = new ArrayList<>();
    @Nullable private final ArrayList<Integer> mIgnoreSitesIds;

    private OnSiteClickListener mSiteSelectedListener;
    private OnSelectedCountChangedListener mSelectedCountListener;
    @NonNull private final OnDataLoadedListener mDataLoadedListener;

    private boolean mIsSingleItemSelectionEnabled;
    private int mSelectedItemPos;

    private SitePickerMode mSitePickerMode = SitePickerMode.DEFAULT_MODE;

    // show recently picked first if there are at least this many blogs
    private static final int RECENTLY_PICKED_THRESHOLD = 11;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_FOOTER = 2;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;
    @Inject BuildConfigWrapper mBuildConfigWrapper;
    @Inject SelectedSiteRepository mSelectedSiteRepository;

    class SiteViewHolder extends RecyclerView.ViewHolder {
        private final ViewGroup mLayoutContainer;
        private final TextView mTxtTitle;
        private final TextView mTxtDomain;
        private final ImageView mImgBlavatar;
        @Nullable private final View mItemDivider;
        @Nullable private final View mDivider;
        private Boolean mIsSiteHidden;
        private final RadioButton mSelectedRadioButton;

        SiteViewHolder(View view) {
            super(view);
            mLayoutContainer = view.findViewById(R.id.layout_container);
            mTxtTitle = view.findViewById(R.id.text_title);
            mTxtDomain = view.findViewById(R.id.text_domain);
            mImgBlavatar = view.findViewById(R.id.image_blavatar);
            mItemDivider = view.findViewById(R.id.item_divider);
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
                             @NonNull OnDataLoadedListener dataLoadedListener,
                             SitePickerMode sitePickerMode,
                             boolean isInEditMode
    ) {
        this(context, itemLayoutResourceId, currentLocalBlogId, lastSearch, isInSearchMode, dataLoadedListener,
                null, null, null, sitePickerMode, isInEditMode, false);
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             @NonNull OnDataLoadedListener dataLoadedListener,
                             @NonNull ViewHolderHandler<?> headerHandler,
                             @Nullable ArrayList<Integer> ignoreSitesIds
    ) {
        this(context, itemLayoutResourceId, currentLocalBlogId, lastSearch, isInSearchMode, dataLoadedListener,
                headerHandler, null, ignoreSitesIds, SitePickerMode.DEFAULT_MODE, false, false);
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             @NonNull OnDataLoadedListener dataLoadedListener,
                             @NonNull ViewHolderHandler<?> headerHandler,
                             @Nullable ViewHolderHandler<?> footerHandler,
                             ArrayList<Integer> ignoreSitesIds,
                             SitePickerMode sitePickerMode,
                             boolean showAndReturn
    ) {
        this(context, itemLayoutResourceId, currentLocalBlogId, lastSearch, isInSearchMode, dataLoadedListener,
                headerHandler, footerHandler, ignoreSitesIds, sitePickerMode, false, showAndReturn);
    }

    public SitePickerAdapter(Context context,
                             @LayoutRes int itemLayoutResourceId,
                             int currentLocalBlogId,
                             String lastSearch,
                             boolean isInSearchMode,
                             @NonNull OnDataLoadedListener dataLoadedListener,
                             @Nullable ViewHolderHandler<?> headerHandler,
                             @Nullable ViewHolderHandler<?> footerHandler,
                             @Nullable ArrayList<Integer> ignoreSitesIds,
                             SitePickerMode sitePickerMode,
                             boolean isInEditMode,
                             boolean showAndReturn
    ) {
        super();
        ((WordPress) context.getApplicationContext()).component().inject(this);

        setHasStableIds(true);

        mLastSearch = StringUtils.notNullStr(lastSearch);
        mIsInSearchMode = isInSearchMode;
        mItemLayoutReourceId = itemLayoutResourceId;
        mCurrentLocalId = currentLocalBlogId;
        mSelectedLocalId = mCurrentLocalId;
        mInflater = LayoutInflater.from(context);
        mDataLoadedListener = dataLoadedListener;

        mBlavatarSz = context.getResources().getDimensionPixelSize(R.dimen.blavatar_sz);

        TypedValue disabledAlpha = new TypedValue();
        context.getResources().getValue(
                com.google.android.material.R.dimen.material_emphasis_disabled,
                disabledAlpha,
                true
        );
        mDisabledSiteOpacity = disabledAlpha.getFloat();
        mSelectedItemBackground = ColorUtils
                .setAlphaComponent(
                        ContextExtensionsKt.getColorFromAttribute(
                                context,
                                com.google.android.material.R.attr.colorOnSurface
                        ),
                        context.getResources().getInteger(R.integer.selected_list_item_opacity)
                );

        mHeaderHandler = headerHandler;
        mFooterHandler = footerHandler;
        mSelectedItemPos = getPositionOffset();

        mIgnoreSitesIds = ignoreSitesIds;

        mSitePickerMode = sitePickerMode;

        if (sitePickerMode == SitePickerMode.SIMPLE_MODE) {
            mShowSelfHostedSites = false;
        }

        mShowHiddenSites = isInEditMode; // If site picker is in edit mode, show hidden sites.

        mShowAndReturn = showAndReturn;

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
            return getItem(position).getLocalId();
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

    @SuppressLint("NotifyDataSetChanged")
    public void setOnSiteClickListener(OnSiteClickListener listener) {
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
        holder.mTxtDomain.setText(site.getHomeURL());
        mImageManager.loadImageWithCorners(holder.mImgBlavatar, site.getBlavatarType(), site.getBlavatarUrl(),
                DisplayUtils.dpToPx(holder.itemView.getContext(), 4));

        if ((site.getLocalId() == mCurrentLocalId && !mIsMultiSelectEnabled
             && mSitePickerMode == SitePickerMode.DEFAULT_MODE)
            || (mIsMultiSelectEnabled && isItemSelected(position))
            || (mSitePickerMode == SitePickerMode.REBLOG_CONTINUE_MODE && mSelectedLocalId == site.getLocalId())) {
            holder.mLayoutContainer.setBackgroundColor(mSelectedItemBackground);
        } else {
            holder.mLayoutContainer.setBackground(null);
        }

        // different styling for visible/hidden sites
        if (holder.mIsSiteHidden == null || holder.mIsSiteHidden != site.isHidden()) {
            holder.mIsSiteHidden = site.isHidden();
            holder.mTxtTitle.setAlpha(site.isHidden() ? mDisabledSiteOpacity : 1f);
            holder.mImgBlavatar.setAlpha(site.isHidden() ? mDisabledSiteOpacity : 1f);
        }

        if (holder.mItemDivider != null) {
            boolean showDivider = position < getItemCount() - 1;
            holder.mItemDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }
        if (holder.mDivider != null) {
            // only show divider after last recent pick
            boolean showDivider = site.isRecentPick()
                                  && !mIsInSearchMode
                                  && position < getItemCount() - 1
                                  && !getItem(position + 1).isRecentPick();
            holder.mDivider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }

        if (mIsMultiSelectEnabled || mSiteSelectedListener != null) {
            holder.itemView.setOnClickListener(view -> {
                int clickedPosition = holder.getBindingAdapterPosition();
                if (isValidPosition(clickedPosition)) {
                    if (mIsMultiSelectEnabled) {
                        toggleSelection(clickedPosition);
                    } else if (mSiteSelectedListener != null) {
                        if (mSitePickerMode.isReblogMode()) {
                            mSitePickerMode = SitePickerMode.REBLOG_CONTINUE_MODE;
                            mSelectedLocalId = site.getLocalId();
                            selectSingleItem(clickedPosition);
                        }

                        mSiteSelectedListener.onSiteClick(getItem(clickedPosition));
                    }
                } else {
                    AppLog.w(AppLog.T.MAIN, "site picker > invalid clicked position " + clickedPosition);
                }
            });

            if (!mSitePickerMode.isReblogMode()) {
                holder.itemView.setOnLongClickListener(view -> {
                    int clickedPosition = holder.getBindingAdapterPosition();
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
                ViewExtensionsKt.redirectContextClickToLongPressListener(holder.itemView);
            }
        }

        if (mIsSingleItemSelectionEnabled) {
            if (getSitesCount() <= 1) {
                holder.mSelectedRadioButton.setVisibility(View.GONE);
            } else {
                holder.mSelectedRadioButton.setVisibility(View.VISIBLE);
                holder.mSelectedRadioButton.setChecked(mSelectedItemPos == position);
                holder.mLayoutContainer.setOnClickListener(v -> selectSingleItem(holder.getBindingAdapterPosition()));
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

    @SuppressLint("NotifyDataSetChanged")
    public void setSingleItemSelectionEnabled(final boolean enabled) {
        if (enabled != mIsSingleItemSelectionEnabled) {
            mIsSingleItemSelectionEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    public void findAndSelect(final int lastUsedBlogLocalId) {
        int positionInSitesArray = SiteRecordUtil.indexOf(mSites, lastUsedBlogLocalId);
        if (positionInSitesArray != -1) {
            selectSingleItem(positionInSitesArray + getPositionOffset());
        }
    }

    public int getSelectedItemLocalId() {
        return mSites.size() != 0 ? getItem(mSelectedItemPos).getLocalId() : -1;
    }

    public int getItemPosByLocalId(int localId) {
        int positionInSitesArray = SiteRecordUtil.indexOf(mSites, localId);

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

    @SuppressLint("NotifyDataSetChanged")
    void searchSites(String searchText) {
        mLastSearch = searchText;
        mSites = SiteRecordUtil.filteredSites(mAllSites, searchText);

        notifyDataSetChanged();
    }

    private boolean isValidPosition(int position) {
        if (isNewLoginEpilogueScreenEnabled()) {
            return (position >= 0 && position <= mSites.size());
        } else {
            return (position >= 0 && position < mSites.size());
        }
    }

    private boolean isNewLoginEpilogueScreenEnabled() {
        return mBuildConfigWrapper.isSiteCreationEnabled()
               && !mShowAndReturn;
    }

    /*
     * called when the user chooses to edit the visibility of wp.com blogs
     */
    void setEnableEditMode(boolean enable, @NonNull HashSet<Integer> selectedPositions) {
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

        // If the selectedPositions value that is passed from the activity isn't empty, then we add that to the
        // Adapter's mSelectedPositions. Otherwise, reset the selected positions.
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
            if (isValidPosition(i) && mSites.get(i).isHidden()) {
                numHidden++;
            }
        }
        return numHidden;
    }

    int getNumVisibleSelected() {
        int numVisible = 0;
        for (Integer i : mSelectedPositions) {
            if (i < mSites.size() && !mSites.get(i).isHidden()) {
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

    @SuppressLint("NotifyDataSetChanged")
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

    @SuppressLint("NotifyDataSetChanged")
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

    @SuppressLint("NotifyDataSetChanged")
    void clearReblogSelection() {
        mSitePickerMode = SitePickerMode.REBLOG_SELECT_MODE;
        notifyDataSetChanged();
    }

    @NonNull
    private List<SiteRecord> getSelectedSites() {
        ArrayList<SiteRecord> sites = new ArrayList<>();
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

    List<SiteRecord> getHiddenSites() {
        ArrayList<SiteRecord> hiddenSites = new ArrayList<>();
        for (SiteRecord site : mSites) {
            if (site.isHidden()) {
                hiddenSites.add(site);
            }
        }

        return hiddenSites;
    }

    @SuppressLint("NotifyDataSetChanged")
    Set<SiteRecord> setVisibilityForSelectedSites(boolean makeVisible) {
        List<SiteRecord> sites = getSelectedSites();
        Set<SiteRecord> changeSet = new HashSet<>();
        if (sites.size() > 0) {
            ArrayList<Integer> recentIds = AppPrefs.getRecentlyPickedSiteIds();
            int selectedSiteLocalId = mSelectedSiteRepository.getSelectedSiteLocalId();
            for (SiteRecord site : sites) {
                int index = SiteRecordUtil.indexOf(mAllSites, site);
                if (index > -1) {
                    SiteRecord siteRecord = mAllSites.get(index);
                    if (siteRecord.isHidden() == makeVisible) {
                        changeSet.add(siteRecord);
                        siteRecord.setHidden(!makeVisible);
                        if (!makeVisible
                            && siteRecord.getLocalId() != selectedSiteLocalId
                            && recentIds.contains(siteRecord.getLocalId())) {
                            AppPrefs.removeRecentlyPickedSiteId(siteRecord.getLocalId());
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

    @SuppressWarnings("deprecation")
    void loadSites() {
        new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private List<SiteRecord> filteredSitesByTextIfInSearchMode(List<SiteRecord> sites) {
        if (!mIsInSearchMode) {
            return sites;
        } else {
            return SiteRecordUtil.filteredSites(sites, mLastSearch);
        }
    }

    public List<SiteModel> getBlogsForCurrentView() {
        if (mSitePickerMode.isReblogMode() || mSitePickerMode.isBloggingPromptsMode()) {
            // If we are reblogging we only want to select or search into the WPCom visible sites.
            return mSiteStore.getVisibleSitesAccessedViaWPCom();
        } else if (mIsInSearchMode) {
            return mSiteStore.getSites();
        }
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

    /*
     * AsyncTask which loads sites from database and populates the adapter
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class LoadSitesTask extends AsyncTask<Void, Void, List<SiteRecord>[]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            boolean isEmpty = mSites == null || mSites.size() == 0;
            mDataLoadedListener.onBeforeLoad(isEmpty);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected List<SiteRecord>[] doInBackground(Void... params) {
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

            List<SiteRecord> sites = SiteRecordUtil.createRecords(siteModels);

            // sort primary blog to the top, otherwise sort by blog/host
            final long primaryBlogId = mAccountStore.getAccount().getPrimarySiteId();
            sites = SiteRecordUtil.sort(sites, primaryBlogId);

            // flag recently-picked sites and move them to the top if there are enough sites and
            // the user isn't searching
            if (!mIsInSearchMode && sites.size() >= RECENTLY_PICKED_THRESHOLD) {
                ArrayList<Integer> pickedIds = AppPrefs.getRecentlyPickedSiteIds();
                for (int i = pickedIds.size() - 1; i > -1; i--) {
                    int thisId = pickedIds.get(i);
                    int indexOfSite = SiteRecordUtil.indexOf(sites, thisId);
                    if (indexOfSite > -1) {
                        SiteRecord site = sites.remove(indexOfSite);
                        site.setRecentPick(true);
                        sites.add(0, site);
                    }
                }
            }

            if (!SiteRecordUtil.isSameList(mSites, sites)) {
                List<SiteRecord>[] arrayOfLists = new List[2];
                arrayOfLists[0] = sites;
                arrayOfLists[1] = filteredSitesByTextIfInSearchMode(sites);

                return arrayOfLists;
            }

            return null;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void onPostExecute(List<SiteRecord>[] updatedSiteLists) {
            if (updatedSiteLists != null) {
                mAllSites = updatedSiteLists[0];
                mSites = updatedSiteLists[1];
                notifyDataSetChanged();
            }
            mDataLoadedListener.onAfterLoad();
        }
    }
}
