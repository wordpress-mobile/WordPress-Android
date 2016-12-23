package org.wordpress.android.fluxc.store;

import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.SelectMapper;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.Payload;
import org.wordpress.android.fluxc.action.SiteAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 */
@Singleton
public class SiteStore extends Store {
    // Payloads
    public static class RefreshSitesXMLRPCPayload extends Payload {
        public RefreshSitesXMLRPCPayload() {}
        public String username;
        public String password;
        public String url;
    }

    public static class NewSitePayload extends Payload {
        public String siteName;
        public String siteTitle;
        public String language;
        public SiteVisibility visibility;
        public boolean dryRun;
        public NewSitePayload(@NonNull String siteName, @NonNull String siteTitle, @NonNull String language,
                              SiteVisibility visibility, boolean dryRun) {
            this.siteName = siteName;
            this.siteTitle = siteTitle;
            this.language = language;
            this.visibility = visibility;
            this.dryRun = dryRun;
        }
    }

    public static class FetchedPostFormatsPayload extends Payload {
        public SiteModel site;
        public List<PostFormatModel> postFormats;
        public FetchedPostFormatsPayload(@NonNull SiteModel site, @NonNull List<PostFormatModel> postFormats) {
            this.site = site;
            this.postFormats = postFormats;
        }
    }

    public static class SiteError implements OnChangedError {
        public SiteErrorType type;

        public SiteError(SiteErrorType type) {
            this.type = type;
        }
    }

    public static class PostFormatsError implements OnChangedError {
        public PostFormatsErrorType type;

        public PostFormatsError(PostFormatsErrorType type) {
            this.type = type;
        }
    }

    public static class NewSiteError implements OnChangedError {
        public NewSiteErrorType type;
        public String message;
        public NewSiteError(NewSiteErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    // OnChanged Events
    public class OnSiteChanged extends OnChanged<SiteError> {
        public int rowsAffected;
        public OnSiteChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    public class OnSiteRemoved extends OnChanged<SiteError> {
        public int mRowsAffected;
        public OnSiteRemoved(int rowsAffected) {
            mRowsAffected = rowsAffected;
        }
    }

    public class OnNewSiteCreated extends OnChanged<NewSiteError> {
        public boolean dryRun;
    }

    public class OnPostFormatsChanged extends OnChanged<PostFormatsError> {
        public SiteModel site;
        public OnPostFormatsChanged(SiteModel site) {
            this.site = site;
        }
    }

    public enum SiteErrorType {
        INVALID_SITE,
        GENERIC_ERROR
    }

    public enum PostFormatsErrorType {
        INVALID_SITE,
        GENERIC_ERROR
    }

    // Enums
    public enum NewSiteErrorType {
        SITE_NAME_REQUIRED,
        SITE_NAME_NOT_ALLOWED,
        SITE_NAME_MUST_BE_AT_LEAST_FOUR_CHARACTERS,
        SITE_NAME_MUST_BE_LESS_THAN_SIXTY_FOUR_CHARACTERS,
        SITE_NAME_CONTAINS_INVALID_CHARACTERS,
        SITE_NAME_CANT_BE_USED,
        SITE_NAME_ONLY_LOWERCASE_LETTERS_AND_NUMBERS,
        SITE_NAME_MUST_INCLUDE_LETTERS,
        SITE_NAME_EXISTS,
        SITE_NAME_RESERVED,
        SITE_NAME_RESERVED_BUT_MAY_BE_AVAILABLE,
        SITE_NAME_INVALID,
        SITE_TITLE_INVALID,
        GENERIC_ERROR;

        // SiteStore semantics prefers SITE over BLOG but errors reported from the API use BLOG
        // these are used to convert API errors to the appropriate enum value in fromString
        private static final String BLOG = "BLOG";
        private static final String SITE = "SITE";

        public static NewSiteErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                String siteString = string.toUpperCase(Locale.US).replace(BLOG, SITE);
                for (NewSiteErrorType v : NewSiteErrorType.values()) {
                    if (siteString.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum SiteVisibility {
        PRIVATE(-1),
        BLOCK_SEARCH_ENGINE(0),
        PUBLIC(1);

        private final int mValue;

        SiteVisibility(int value) {
            this.mValue = value;
        }

        public int value() {
            return mValue;
        }
    }

    private SiteRestClient mSiteRestClient;
    private SiteXMLRPCClient mSiteXMLRPCClient;

    @Inject
    public SiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient, SiteXMLRPCClient siteXMLRPCClient) {
        super(dispatcher);
        mSiteRestClient = siteRestClient;
        mSiteXMLRPCClient = siteXMLRPCClient;
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "SiteStore onRegister");
    }

    /**
     * Returns all sites in the store as a {@link SiteModel} list.
     */
    public List<SiteModel> getSites() {
        return WellSql.select(SiteModel.class).getAsModel();
    }

    /**
     * Returns all sites in the store as a {@link Cursor}.
     */
    public Cursor getSitesCursor() {
        return WellSql.select(SiteModel.class).getAsCursor();
    }

    /**
     * Returns the number of sites of any kind in the store.
     */
    public int getSitesCount() {
        return getSitesCursor().getCount();
    }

    /**
     * Checks whether the store contains any sites of any kind.
     */
    public boolean hasSite() {
        return getSitesCount() != 0;
    }

    /**
     * Obtains the site with the given (local) id and returns it as a {@link SiteModel}.
     */
    public SiteModel getSiteByLocalId(int id) {
        List<SiteModel> result = SiteSqlUtils.getAllSitesWith(SiteModelTable.ID, id);
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    public boolean hasSiteWithLocalId(int id) {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.ID, id) > 0;
    }

    /**
     * Returns all .COM sites in the store.
     */
    public List<SiteModel> getWPComSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, true);
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    public int getWPComSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_WPCOM, true);
    }

    /**
     * Returns sites with a name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getSitesByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getAllSitesMatchingUrlOrNameWith(SiteModelTable.IS_WPCOM, true, searchString);
    }

    /**
     * Returns .COM sites with a name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getWPComSiteByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getAllSitesMatchingUrlOrName(searchString);
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    public boolean hasWPComSite() {
        return getWPComSitesCount() != 0;
    }

    /**
     * Returns all self-hosted sites in the store.
     */
    public List<SiteModel> getSelfHostedSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_WPCOM, false);
    }

    /**
     * Returns the number of self-hosted sites (can be Jetpack) in the store.
     */
    public int getSelfHostedSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_WPCOM, false);
    }

    /**
     * Checks whether the store contains at least one self-hosted site (can be Jetpack).
     */
    public boolean hasSelfHostedSite() {
        return getSelfHostedSitesCount() != 0;
    }

    /**
     * Returns all Jetpack sites in the store.
     */
    public List<SiteModel> getJetpackSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_JETPACK, true);
    }

    /**
     * Returns the number of Jetpack sites in the store.
     */
    public int getJetpackSitesCount() {
        return SiteSqlUtils.getNumberOfSitesWith(SiteModelTable.IS_JETPACK, true);
    }

    /**
     * Checks whether the store contains at least one Jetpack site.
     */
    public boolean hasJetpackSite() {
        return getJetpackSitesCount() != 0;
    }

    /**
     * Checks whether the store contains a self-hosted site matching the given (remote) site id and XML-RPC URL.
     */
    public boolean hasSelfHostedSiteWithSiteIdAndXmlRpcUrl(long selfHostedSiteId, String xmlRpcUrl) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Returns all visible sites as {@link SiteModel}s. All self-hosted sites over XML-RPC are visible by default.
     */
    public List<SiteModel> getVisibleSites() {
        return SiteSqlUtils.getAllSitesWith(SiteModelTable.IS_VISIBLE, true);
    }

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    public int getVisibleSitesCount() {
        return getVisibleSites().size();
    }

    /**
     * Returns all visible .COM sites as {@link SiteModel}s.
     */
    public List<SiteModel> getVisibleWPComSites() {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.IS_WPCOM, true)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere()
                .getAsModel();
    }

    /**
     * Returns the number of visible .COM sites.
     */
    public int getVisibleWPComSitesCount() {
        return getVisibleWPComSites().size();
    }

    /**
     * Checks whether the .COM site with the given (local) id is visible.
     */
    public boolean isWPComSiteVisibleByLocalId(int id) {
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .equals(SiteModelTable.IS_WPCOM, true)
                .equals(SiteModelTable.IS_VISIBLE, true)
                .endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a (remote) site id, returns the corresponding (local) id.
     */
    public int getLocalIdForRemoteSiteId(long siteId) {
        List<SiteModel> sites =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SITE_ID, siteId)
                .or()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, siteId)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (remote) self-hosted site id and XML-RPC url, returns the corresponding (local) id.
     */
    public int getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(long selfHostedSiteId, String xmlRpcUrl) {
        List<SiteModel> sites =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.SELF_HOSTED_SITE_ID, selfHostedSiteId)
                .equals(SiteModelTable.XMLRPC_URL, xmlRpcUrl)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.ID)));
                        return siteModel;
                    }
                });
        if (sites.size() > 0) {
            return sites.get(0).getId();
        }
        return 0;
    }

    /**
     * Given a (local) id, returns the (remote) site id. Searches first for .COM and Jetpack, then looks for self-hosted
     * sites.
     */
    public long getSiteIdForLocalId(int id) {
        List<SiteModel> result =  WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, id)
                .endGroup().endWhere()
                .getAsModel(new SelectMapper<SiteModel>() {
                    @Override
                    public SiteModel convert(Cursor cursor) {
                        SiteModel siteModel = new SiteModel();
                        siteModel.setSiteId(cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)));
                        siteModel.setSelfHostedSiteId(cursor.getLong(
                                cursor.getColumnIndex(SiteModelTable.SELF_HOSTED_SITE_ID)));
                        return siteModel;
                    }
                });
        if (result.isEmpty()) {
            return 0;
        }

        if (result.get(0).getSiteId() > 0) {
            return result.get(0).getSiteId();
        } else {
            return result.get(0).getSelfHostedSiteId();
        }
    }

    /**
     * Given a (remote) site id, returns true if the given site is WP.com or Jetpack-enabled
     * (returns false for non-Jetpack self-hosted sites).
     */
    public boolean hasWPComOrJetpackSiteWithSiteId(long siteId) {
        int localId = getLocalIdForRemoteSiteId(siteId);
        return WellSql.select(SiteModel.class)
                .where().beginGroup()
                .equals(SiteModelTable.ID, localId)
                .beginGroup()
                .equals(SiteModelTable.IS_WPCOM, true).or().equals(SiteModelTable.IS_JETPACK, true)
                .endGroup().endGroup().endWhere()
                .getAsCursor().getCount() > 0;
    }

    /**
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * {@link SiteModel}.
     */
    public SiteModel getSiteBySiteId(long siteId) {
        if (siteId == 0) {
            return null;
        }

        List<SiteModel> sites = SiteSqlUtils.getAllSitesWith(SiteModelTable.SITE_ID, siteId);

        if (sites.isEmpty()) {
            return null;
        } else {
            return sites.get(0);
        }
    }

    public List<PostFormatModel> getPostFormats(SiteModel site) {
        return SiteSqlUtils.getPostFormats(site);
    }

    public int deleteAllSites() {
        return SiteSqlUtils.deleteAllSites();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof SiteAction)) {
            return;
        }

        switch ((SiteAction) actionType) {
            case FETCH_SITE:
                fetchSite((SiteModel) action.getPayload());
                break;
            case FETCH_SITES:
                mSiteRestClient.fetchSites();
                break;
            case FETCH_SITES_XML_RPC:
                fetchSitesXmlRpc((RefreshSitesXMLRPCPayload) action.getPayload());
                break;
            case UPDATE_SITE:
                updateSite((SiteModel) action.getPayload());
                break;
            case UPDATE_SITES:
                updateSites((SitesModel) action.getPayload());
                break;
            case REMOVE_SITE:
                removeSite((SiteModel) action.getPayload());
                break;
            case REMOVE_WPCOM_SITES:
                removeWPComSites();
                break;
            case SHOW_SITES:
                toggleSitesVisibility((SitesModel) action.getPayload(), true);
                break;
            case HIDE_SITES:
                toggleSitesVisibility((SitesModel) action.getPayload(), false);
                break;
            case CREATE_NEW_SITE:
                createNewSite((NewSitePayload) action.getPayload());
                break;
            case CREATED_NEW_SITE:
                handleCreateNewSiteCompleted((NewSiteResponsePayload) action.getPayload());
                break;
            case FETCH_POST_FORMATS:
                fetchPostFormats((SiteModel) action.getPayload());
                break;
            case FETCHED_POST_FORMATS:
                updatePostFormats((FetchedPostFormatsPayload) action.getPayload());
                break;
        }
    }

    private void removeSite(SiteModel site) {
        int rowsAffected = SiteSqlUtils.deleteSite(site);
        // TODO: This should be captured by 'QuickPressShortcutsStore' so it can handle deleting any QP shortcuts
        // TODO: Probably, we can inject QuickPressShortcutsStore into SiteStore and act on it directly
        // See WordPressDB.deleteQuickPressShortcutsForLocalTableBlogId(Context ctx, int siteId)
        emitChange(new OnSiteRemoved(rowsAffected));
    }

    private void removeWPComSites() {
        // Logging out of WP.com. Drop all WP.com sites, and all Jetpack sites that were fetched over the WP.com
        // REST API only (they don't have a .org site id)
        List<SiteModel> wpcomSites = SiteSqlUtils.getAllWPComSites();
        int rowsAffected = removeSites(wpcomSites);
        // TODO: Same as above, this needs to be captured and handled by QuickPressShortcutsStore
        emitChange(new OnSiteRemoved(rowsAffected));
    }

    private void createNewSite(NewSitePayload payload) {
        mSiteRestClient.newSite(payload.siteName, payload.siteTitle, payload.language, payload.visibility,
                payload.dryRun);
    }

    private void handleCreateNewSiteCompleted(NewSiteResponsePayload payload) {
        OnNewSiteCreated onNewSiteCreated = new OnNewSiteCreated();
        onNewSiteCreated.error = payload.error;
        onNewSiteCreated.dryRun = payload.dryRun;
        emitChange(onNewSiteCreated);
    }

    private void fetchSite(SiteModel site) {
        if (site.isWPCom()) {
            mSiteRestClient.fetchSite(site);
        } else {
            mSiteXMLRPCClient.fetchSite(site);
        }
    }

    private void fetchSitesXmlRpc(RefreshSitesXMLRPCPayload payload) {
        mSiteXMLRPCClient.fetchSites(payload.url, payload.username, payload.password);
    }

    private void fetchPostFormats(SiteModel site) {
        if (site.isWPCom()) {
            mSiteRestClient.fetchPostFormats(site);
        } else {
            mSiteXMLRPCClient.fetchPostFormats(site);
        }
    }

    private void updateSite(SiteModel siteModel) {
        OnSiteChanged event;
        if (siteModel.isError()) {
            event = new OnSiteChanged(0);
            // TODO: what kind of error could we get here?
            event.error = new SiteError(SiteErrorType.GENERIC_ERROR);
        } else {
            int rowsAffected = SiteSqlUtils.insertOrUpdateSite(siteModel);
            event = new OnSiteChanged(rowsAffected);
        }
        emitChange(event);
    }

    private void updateSites(SitesModel sitesModel) {
        OnSiteChanged event;
        if (sitesModel.isError()) {
            event = new OnSiteChanged(0);
            // TODO: what kind of error could we get here?
            event.error = new SiteError(SiteErrorType.GENERIC_ERROR);
        } else {
            int rowsAffected = createOrUpdateSites(sitesModel);
            event = new OnSiteChanged(rowsAffected);
        }
        emitChange(event);
    }

    private void updatePostFormats(FetchedPostFormatsPayload payload) {
        OnPostFormatsChanged event = new OnPostFormatsChanged(payload.site);
        if (payload.isError()) {
            // TODO: what kind of error could we get here?
            event.error = new PostFormatsError(PostFormatsErrorType.GENERIC_ERROR);
        } else {
            SiteSqlUtils.insertOrReplacePostFormats(payload.site, payload.postFormats);
        }
        emitChange(event);
    }

    private int createOrUpdateSites(SitesModel sites) {
        int rowsAffected = 0;
        for (SiteModel site : sites.getSites()) {
            rowsAffected += SiteSqlUtils.insertOrUpdateSite(site);
        }
        return rowsAffected;
    }

    private int removeSites(List<SiteModel> sites) {
        int rowsAffected = 0;
        for (SiteModel site : sites) {
            rowsAffected += SiteSqlUtils.deleteSite(site);
        }
        return rowsAffected;
    }

    private int toggleSitesVisibility(SitesModel sites, boolean visible) {
        int rowsAffected = 0;
        for (SiteModel site : sites.getSites()) {
            rowsAffected += SiteSqlUtils.setSiteVisibility(site, visible);
        }
        return rowsAffected;
    }
}
