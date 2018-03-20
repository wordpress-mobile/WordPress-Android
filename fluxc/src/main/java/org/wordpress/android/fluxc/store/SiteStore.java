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
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest;
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.utils.SiteErrorUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
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
    public static class RefreshSitesXMLRPCPayload extends Payload<BaseNetworkError> {
        public RefreshSitesXMLRPCPayload() {}
        public String username;
        public String password;
        public String url;
    }

    public static class NewSitePayload extends Payload<BaseNetworkError> {
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

    public static class FetchedPostFormatsPayload extends Payload<PostFormatsError> {
        public SiteModel site;
        public List<PostFormatModel> postFormats;
        public FetchedPostFormatsPayload(@NonNull SiteModel site, @NonNull List<PostFormatModel> postFormats) {
            this.site = site;
            this.postFormats = postFormats;
        }
    }

    public static class FetchedUserRolesPayload extends Payload<UserRolesError> {
        public SiteModel site;
        public List<RoleModel> roles;
        public FetchedUserRolesPayload(@NonNull SiteModel site, @NonNull List<RoleModel> roles) {
            this.site = site;
            this.roles = roles;
        }
    }

    public static class SuggestDomainsPayload extends Payload<BaseNetworkError> {
        public String query;
        public boolean onlyWordpressCom;
        public boolean includeWordpressCom;
        public boolean includeDotBlogSubdomain;
        public int quantity;
        public SuggestDomainsPayload(@NonNull String query, boolean onlyWordpressCom, boolean includeWordpressCom,
                                     boolean includeDotBlogSubdomain, int quantity) {
            this.query = query;
            this.onlyWordpressCom = onlyWordpressCom;
            this.includeWordpressCom = includeWordpressCom;
            this.includeDotBlogSubdomain = includeDotBlogSubdomain;
            this.quantity = quantity;
        }
    }

    public static class SuggestDomainsResponsePayload extends Payload<BaseNetworkError> {
        public String query;
        public List<DomainSuggestionResponse> suggestions;
        public SuggestDomainsResponsePayload(@NonNull String query, BaseNetworkError error) {
            this.query = query;
            this.error = error;
            this.suggestions = new ArrayList<>();
        }

        public SuggestDomainsResponsePayload(@NonNull String query, ArrayList<DomainSuggestionResponse> suggestions) {
            this.query = query;
            this.suggestions = suggestions;
        }
    }

    public static class ConnectSiteInfoPayload extends Payload<SiteError> {
        public String url;
        public boolean exists;
        public boolean isWordPress;
        public boolean hasJetpack;
        public boolean isJetpackActive;
        public boolean isJetpackConnected;
        public boolean isWPCom;

        public ConnectSiteInfoPayload(@NonNull String url, SiteError error) {
            this.url = url;
            this.error = error;
        }

        public String description() {
            return String.format("url: %s, e: %b, wp: %b, jp: %b, wpcom: %b",
                    url, exists, isWordPress, hasJetpack, isWPCom);
        }
    }

    public static class InitiateAutomatedTransferPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginSlugToInstall;

        public InitiateAutomatedTransferPayload(SiteModel site, String pluginSlugToInstall) {
            this.site = site;
            this.pluginSlugToInstall = pluginSlugToInstall;
        }
    }

    public static class AutomatedTransferEligibilityResponsePayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public boolean isEligible;

        public AutomatedTransferEligibilityResponsePayload(SiteModel site, boolean isEligible) {
            this.site = site;
            this.isEligible = isEligible;
        }

        public AutomatedTransferEligibilityResponsePayload(SiteModel site, BaseNetworkError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class InitiateAutomatedTransferResponsePayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String pluginSlugToInstall;
        public String status;
        public boolean success;
        public String transferId;

        public InitiateAutomatedTransferResponsePayload(SiteModel site, String pluginSlugToInstall) {
            this.site = site;
            this.pluginSlugToInstall = pluginSlugToInstall;
        }
    }

    public static class SiteError implements OnChangedError {
        public SiteErrorType type;
        public String message;

        public SiteError(SiteErrorType type) {
            this(type, "");
        }

        public SiteError(SiteErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class PostFormatsError implements OnChangedError {
        public PostFormatsErrorType type;
        public String message;

        public PostFormatsError(PostFormatsErrorType type) {
            this(type, "");
        }

        public PostFormatsError(PostFormatsErrorType type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class UserRolesError implements OnChangedError {
        public UserRolesErrorType type;
        public String message;

        public UserRolesError(UserRolesErrorType type) {
            this(type, "");
        }

        UserRolesError(UserRolesErrorType type, String message) {
            this.type = type;
            this.message = message;
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

    public static class DeleteSiteError implements OnChangedError {
        public DeleteSiteErrorType type;
        public String message;
        public DeleteSiteError(String errorType, @NonNull String message) {
            this.type = DeleteSiteErrorType.fromString(errorType);
            this.message = message;
        }
        public DeleteSiteError(DeleteSiteErrorType errorType) {
            this.type = errorType;
            this.message = "";
        }
    }

    public static class ExportSiteError implements OnChangedError {
        public ExportSiteErrorType type;

        public ExportSiteError(ExportSiteErrorType type) {
            this.type = type;
        }
    }

    // OnChanged Events
    public static class OnProfileFetched extends OnChanged<SiteError> {
        public SiteModel site;
        public OnProfileFetched(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnSiteChanged extends OnChanged<SiteError> {
        public int rowsAffected;
        public OnSiteChanged(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    public static class OnSiteRemoved extends OnChanged<SiteError> {
        public int mRowsAffected;
        public OnSiteRemoved(int rowsAffected) {
            mRowsAffected = rowsAffected;
        }
    }

    public static class OnAllSitesRemoved extends OnChanged<SiteError> {
        public int mRowsAffected;
        public OnAllSitesRemoved(int rowsAffected) {
            mRowsAffected = rowsAffected;
        }
    }

    public static class OnNewSiteCreated extends OnChanged<NewSiteError> {
        public boolean dryRun;
        public long newSiteRemoteId;
    }

    public static class OnSiteDeleted extends OnChanged<DeleteSiteError> {
        public OnSiteDeleted(DeleteSiteError error) {
            this.error = error;
        }
    }

    public static class OnSiteExported extends OnChanged<ExportSiteError> {
        public OnSiteExported() {
        }
    }

    public static class OnPostFormatsChanged extends OnChanged<PostFormatsError> {
        public SiteModel site;
        public OnPostFormatsChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnUserRolesChanged extends OnChanged<UserRolesError> {
        public SiteModel site;
        public OnUserRolesChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnURLChecked extends OnChanged<SiteError> {
        public String url;
        public boolean isWPCom;
        public OnURLChecked(@NonNull String url) {
            this.url = url;
        }
    }

    public static class OnConnectSiteInfoChecked extends OnChanged<SiteError> {
        public ConnectSiteInfoPayload info;
        public OnConnectSiteInfoChecked(@NonNull ConnectSiteInfoPayload info) {
            this.info = info;
        }
    }

    public static class OnWPComSiteFetched extends OnChanged<SiteError> {
        public String checkedUrl;
        public SiteModel site;
        public OnWPComSiteFetched(String checkedUrl, @NonNull SiteModel site) {
            this.checkedUrl = checkedUrl;
            this.site = site;
        }
    }

    public static class SuggestDomainError implements OnChangedError {
        public SuggestDomainErrorType type;
        public String message;
        public SuggestDomainError(@NonNull String apiErrorType, @NonNull String message) {
            this.type = SuggestDomainErrorType.fromString(apiErrorType);
            this.message = message;
        }
    }

    public static class OnSuggestedDomains extends OnChanged<SuggestDomainError> {
        public String query;
        public List<DomainSuggestionResponse> suggestions;
        public OnSuggestedDomains(@NonNull String query, @NonNull List<DomainSuggestionResponse> suggestions) {
            this.query = query;
            this.suggestions = suggestions;
        }
    }

    public static class OnAutomatedTransferAvailabilityChecked extends OnChanged<SiteError> {
        public SiteModel site;
        public OnAutomatedTransferAvailabilityChecked(SiteModel site, SiteError siteError) {
            this.site = site;
            this.error = siteError;
        }
    }

    public static class UpdateSitesResult {
        public int rowsAffected = 0;
        public boolean duplicateSiteFound = false;
    }

    public enum SiteErrorType {
        INVALID_SITE,
        UNKNOWN_SITE,
        DUPLICATE_SITE,
        INVALID_RESPONSE,
        UNAUTHORIZED,
        GENERIC_ERROR
    }

    public enum SuggestDomainErrorType {
        EMPTY_QUERY,
        INVALID_MINIMUM_QUANTITY,
        INVALID_MAXIMUM_QUANTITY,
        GENERIC_ERROR;

        public static SuggestDomainErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                for (SuggestDomainErrorType v : SuggestDomainErrorType.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum PostFormatsErrorType {
        INVALID_SITE,
        INVALID_RESPONSE,
        GENERIC_ERROR;
    }

    public enum UserRolesErrorType {
        GENERIC_ERROR
    }

    public enum DeleteSiteErrorType {
        INVALID_SITE,
        UNAUTHORIZED, // user don't have permission to delete
        AUTHORIZATION_REQUIRED, // missing access token
        GENERIC_ERROR;

        public static DeleteSiteErrorType fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                if (string.equals("unauthorized")) {
                    return UNAUTHORIZED;
                } else if (string.equals("authorization_required")) {
                    return AUTHORIZATION_REQUIRED;
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum ExportSiteErrorType {
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
        List<SiteModel> result = SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).getAsModel();
        if (result.size() > 0) {
            return result.get(0);
        }
        return null;
    }

    /**
     * Checks whether the store contains a site matching the given (local) id.
     */
    public boolean hasSiteWithLocalId(int id) {
        return SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).getAsCursor().getCount() > 0;
    }

    /**
     * Returns all .COM sites in the store.
     */
    public List<SiteModel> getWPComSites() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsModel();
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API).
     */
    public List<SiteModel> getSitesAccessedViaWPComRest() {
        return SiteSqlUtils.getSitesAccessedViaWPComRest().getAsModel();
    }

    /**
     * Returns the number of sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected
     * via WPCom REST API).
     */
    public int getSitesAccessedViaWPComRestCount() {
        return SiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one site accessed via WPCom REST API (WPCom sites or Jetpack
     * sites connected via WPCom REST API).
     */
    public boolean hasSitesAccessedViaWPComRest() {
        return getSitesAccessedViaWPComRestCount() != 0;
    }

    /**
     * Returns the number of .COM sites in the store.
     */
    public int getWPComSitesCount() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).getAsCursor().getCount();
    }

    /**
     * Returns sites with a name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getSitesByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getSitesByNameOrUrlMatching(searchString);
    }

    /**
     * Returns sites accessed via WPCom REST API (WPCom sites or Jetpack sites connected via WPCom REST API) with a
     * name or url matching the search string.
     */
    @NonNull
    public List<SiteModel> getSitesAccessedViaWPComRestByNameOrUrlMatching(@NonNull String searchString) {
        return SiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching(searchString);
    }

    /**
     * Checks whether the store contains at least one .COM site.
     */
    public boolean hasWPComSite() {
        return getWPComSitesCount() != 0;
    }

    /**
     * Returns sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    public List<SiteModel> getSitesAccessedViaXMLRPC() {
        return SiteSqlUtils.getSitesAccessedViaXMLRPC().getAsModel();
    }

    /**
     * Returns the number of sites accessed via XMLRPC (self-hosted sites or Jetpack sites accessed via XMLRPC).
     */
    public int getSitesAccessedViaXMLRPCCount() {
        return SiteSqlUtils.getSitesAccessedViaXMLRPC().getAsCursor().getCount();
    }

    /**
     * Checks whether the store contains at least one site accessed via XMLRPC (self-hosted sites or
     * Jetpack sites accessed via XMLRPC).
     */
    public boolean hasSiteAccessedViaXMLRPC() {
        return getSitesAccessedViaXMLRPCCount() != 0;
    }

    /**
     * Returns all visible sites as {@link SiteModel}s. All self-hosted sites over XML-RPC are visible by default.
     */
    public List<SiteModel> getVisibleSites() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).getAsModel();
    }

    /**
     * Returns the number of visible sites. All self-hosted sites over XML-RPC are visible by default.
     */
    public int getVisibleSitesCount() {
        return SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).getAsCursor().getCount();
    }

    /**
     * Returns all visible .COM sites as {@link SiteModel}s.
     */
    public List<SiteModel> getVisibleSitesAccessedViaWPCom() {
        return SiteSqlUtils.getVisibleSitesAccessedViaWPCom().getAsModel();
    }

    /**
     * Returns the number of visible .COM sites.
     */
    public int getVisibleSitesAccessedViaWPComCount() {
        return SiteSqlUtils.getVisibleSitesAccessedViaWPCom().getAsCursor().getCount();
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
        List<SiteModel> sites = WellSql.select(SiteModel.class)
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
        List<SiteModel> sites = WellSql.select(SiteModel.class)
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
        List<SiteModel> result = WellSql.select(SiteModel.class)
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
     * Given a .COM site ID (either a .COM site id, or the .COM id of a Jetpack site), returns the site as a
     * {@link SiteModel}.
     */
    public SiteModel getSiteBySiteId(long siteId) {
        if (siteId == 0) {
            return null;
        }

        List<SiteModel> sites = SiteSqlUtils.getSitesWith(SiteModelTable.SITE_ID, siteId).getAsModel();

        if (sites.isEmpty()) {
            return null;
        } else {
            return sites.get(0);
        }
    }

    public List<PostFormatModel> getPostFormats(SiteModel site) {
        return SiteSqlUtils.getPostFormats(site);
    }

    public List<RoleModel> getUserRoles(SiteModel site) {
        return SiteSqlUtils.getUserRoles(site);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (!(actionType instanceof SiteAction)) {
            return;
        }

        switch ((SiteAction) actionType) {
            case FETCH_PROFILE_XML_RPC:
                fetchProfileXmlRpc((SiteModel) action.getPayload());
                break;
            case FETCHED_PROFILE_XML_RPC:
                updateSiteProfile((SiteModel) action.getPayload());
                break;
            case FETCH_SITE:
                fetchSite((SiteModel) action.getPayload());
                break;
            case FETCH_SITES:
                mSiteRestClient.fetchSites();
                break;
            case FETCHED_SITES:
                handleFetchedSitesWPComRest((SitesModel) action.getPayload());
                break;
            case FETCH_SITES_XML_RPC:
                fetchSitesXmlRpc((RefreshSitesXMLRPCPayload) action.getPayload());
                break;
            case FETCHED_SITES_XML_RPC:
                updateSites((SitesModel) action.getPayload());
                break;
            case UPDATE_SITE:
                updateSite((SiteModel) action.getPayload());
                break;
            case UPDATE_SITES:
                updateSites((SitesModel) action.getPayload());
                break;
            case DELETE_SITE:
                deleteSite((SiteModel) action.getPayload());
                break;
            case DELETED_SITE:
                handleDeletedSite((DeleteSiteResponsePayload) action.getPayload());
                break;
            case EXPORT_SITE:
                exportSite((SiteModel) action.getPayload());
                break;
            case EXPORTED_SITE:
                handleExportedSite((ExportSiteResponsePayload) action.getPayload());
                break;
            case REMOVE_SITE:
                removeSite((SiteModel) action.getPayload());
                break;
            case REMOVE_ALL_SITES:
                removeAllSites();
                break;
            case REMOVE_WPCOM_AND_JETPACK_SITES:
                removeWPComAndJetpackSites();
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
            case FETCH_USER_ROLES:
                fetchUserRoles((SiteModel) action.getPayload());
                break;
            case FETCHED_USER_ROLES:
                updateUserRoles((FetchedUserRolesPayload) action.getPayload());
                break;
            case FETCH_CONNECT_SITE_INFO:
                fetchConnectSiteInfo((String) action.getPayload());
                break;
            case FETCHED_CONNECT_SITE_INFO:
                handleFetchedConnectSiteInfo((ConnectSiteInfoPayload) action.getPayload());
                break;
            case FETCH_WPCOM_SITE_BY_URL:
                fetchWPComSiteByUrl((String) action.getPayload());
                break;
            case FETCHED_WPCOM_SITE_BY_URL:
                handleFetchedWPComSiteByUrl((FetchWPComSiteResponsePayload) action.getPayload());
                break;
            case IS_WPCOM_URL:
                checkUrlIsWPCom((String) action.getPayload());
                break;
            case CHECKED_IS_WPCOM_URL:
                handleCheckedIsWPComUrl((IsWPComResponsePayload) action.getPayload());
                break;
            case SUGGEST_DOMAINS:
                suggestDomains((SuggestDomainsPayload) action.getPayload());
                break;
            case SUGGESTED_DOMAINS:
                handleSuggestedDomains((SuggestDomainsResponsePayload) action.getPayload());
                break;
            // Automated Transfer
            case CHECK_AUTOMATED_TRANSFER_ELIGIBILITY:
                checkAutomatedTransferEligibility((SiteModel) action.getPayload());
                break;
            case INITIATE_AUTOMATED_TRANSFER:
                initiateAutomatedTransfer((InitiateAutomatedTransferPayload) action.getPayload());
                break;
            case CHECK_AUTOMATED_TRANSFER_STATUS:
                checkAutomatedTransferStatus((SiteModel) action.getPayload());
                break;
            case CHECKED_AUTOMATED_TRANSFER_ELIGIBILITY:
                handleCheckedAutomatedTransferEligibility((AutomatedTransferEligibilityResponsePayload)
                        action.getPayload());
                break;
        }
    }

    private void fetchProfileXmlRpc(SiteModel site) {
        mSiteXMLRPCClient.fetchProfile(site);
    }

    private void fetchSite(SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchSite(site);
        } else {
            mSiteXMLRPCClient.fetchSite(site);
        }
    }

    private void fetchSitesXmlRpc(RefreshSitesXMLRPCPayload payload) {
        mSiteXMLRPCClient.fetchSites(payload.url, payload.username, payload.password);
    }

    private void updateSiteProfile(SiteModel siteModel) {
        OnProfileFetched event = new OnProfileFetched(siteModel);
        if (siteModel.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(siteModel.error);
        } else {
            try {
                SiteSqlUtils.insertOrUpdateSite(siteModel);
            } catch (DuplicateSiteException e) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        emitChange(event);
    }

    private void updateSite(SiteModel siteModel) {
        OnSiteChanged event = new OnSiteChanged(0);
        if (siteModel.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(siteModel.error);
        } else {
            try {
                event.rowsAffected = SiteSqlUtils.insertOrUpdateSite(siteModel);
            } catch (DuplicateSiteException e) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        emitChange(event);
    }

    private void updateSites(SitesModel sitesModel) {
        OnSiteChanged event = new OnSiteChanged(0);
        if (sitesModel.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(sitesModel.error);
        } else {
            UpdateSitesResult res = createOrUpdateSites(sitesModel);
            event.rowsAffected = res.rowsAffected;
            if (res.duplicateSiteFound) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        emitChange(event);
    }

    private void handleFetchedSitesWPComRest(SitesModel fetchedSites) {
        OnSiteChanged event = new OnSiteChanged(0);
        if (fetchedSites.isError()) {
            // TODO: what kind of error could we get here?
            event.error = SiteErrorUtils.genericToSiteError(fetchedSites.error);
        } else {
            UpdateSitesResult res = createOrUpdateSites(fetchedSites);
            event.rowsAffected = res.rowsAffected;
            if (res.duplicateSiteFound) {
                event.error = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
            SiteSqlUtils.removeWPComRestSitesAbsentFromList(fetchedSites.getSites());
        }
        emitChange(event);
    }

    private UpdateSitesResult createOrUpdateSites(SitesModel sites) {
        UpdateSitesResult result = new UpdateSitesResult();
        for (SiteModel site : sites.getSites()) {
            try {
                result.rowsAffected += SiteSqlUtils.insertOrUpdateSite(site);
            } catch (DuplicateSiteException caughtException) {
                result.duplicateSiteFound = true;
            }
        }
        return result;
    }

    private void deleteSite(SiteModel site) {
        // Not available for Jetpack sites
        if (!site.isWPCom()) {
            OnSiteDeleted event = new OnSiteDeleted(new DeleteSiteError(DeleteSiteErrorType.INVALID_SITE));
            emitChange(event);
            return;
        }
        mSiteRestClient.deleteSite(site);
    }

    private void handleDeletedSite(DeleteSiteResponsePayload payload) {
        OnSiteDeleted event = new OnSiteDeleted(payload.error);
        if (!payload.isError()) {
            SiteSqlUtils.deleteSite(payload.site);
        }
        emitChange(event);
    }

    private void exportSite(SiteModel site) {
        // Not available for Jetpack sites
        if (!site.isWPCom()) {
            OnSiteExported event = new OnSiteExported();
            event.error = new ExportSiteError(ExportSiteErrorType.INVALID_SITE);
            emitChange(event);
            return;
        }
        mSiteRestClient.exportSite(site);
    }

    private void handleExportedSite(ExportSiteResponsePayload payload) {
        OnSiteExported event = new OnSiteExported();
        if (payload.isError()) {
            // TODO: what kind of error could we get here?
            event.error = new ExportSiteError(ExportSiteErrorType.GENERIC_ERROR);
        }
        emitChange(event);
    }

    private void removeSite(SiteModel site) {
        int rowsAffected = SiteSqlUtils.deleteSite(site);
        emitChange(new OnSiteRemoved(rowsAffected));
    }

    private void removeAllSites() {
        int rowsAffected = SiteSqlUtils.deleteAllSites();
        OnAllSitesRemoved event = new OnAllSitesRemoved(rowsAffected);
        emitChange(event);
    }

    private void removeWPComAndJetpackSites() {
        // Logging out of WP.com. Drop all WP.com sites, and all Jetpack sites that were fetched over the WP.com
        // REST API only (they don't have a .org site id)
        List<SiteModel> wpcomAndJetpackSites = SiteSqlUtils.getSitesAccessedViaWPComRest().getAsModel();
        int rowsAffected = removeSites(wpcomAndJetpackSites);
        emitChange(new OnSiteRemoved(rowsAffected));
    }

    private int toggleSitesVisibility(SitesModel sites, boolean visible) {
        int rowsAffected = 0;
        for (SiteModel site : sites.getSites()) {
            rowsAffected += SiteSqlUtils.setSiteVisibility(site, visible);
        }
        return rowsAffected;
    }

    private void createNewSite(NewSitePayload payload) {
        mSiteRestClient.newSite(payload.siteName, payload.siteTitle, payload.language, payload.visibility,
                payload.dryRun);
    }

    private void handleCreateNewSiteCompleted(NewSiteResponsePayload payload) {
        OnNewSiteCreated onNewSiteCreated = new OnNewSiteCreated();
        onNewSiteCreated.error = payload.error;
        onNewSiteCreated.dryRun = payload.dryRun;
        onNewSiteCreated.newSiteRemoteId = payload.newSiteRemoteId;
        emitChange(onNewSiteCreated);
    }

    private void fetchPostFormats(SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchPostFormats(site);
        } else {
            mSiteXMLRPCClient.fetchPostFormats(site);
        }
    }

    private void updatePostFormats(FetchedPostFormatsPayload payload) {
        OnPostFormatsChanged event = new OnPostFormatsChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            SiteSqlUtils.insertOrReplacePostFormats(payload.site, payload.postFormats);
        }
        emitChange(event);
    }

    private void fetchUserRoles(SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchUserRoles(site);
        }
    }

    private void updateUserRoles(FetchedUserRolesPayload payload) {
        OnUserRolesChanged event = new OnUserRolesChanged(payload.site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            SiteSqlUtils.insertOrReplaceUserRoles(payload.site, payload.roles);
        }
        emitChange(event);
    }

    private int removeSites(List<SiteModel> sites) {
        int rowsAffected = 0;
        for (SiteModel site : sites) {
            rowsAffected += SiteSqlUtils.deleteSite(site);
        }
        return rowsAffected;
    }

    private void fetchConnectSiteInfo(String payload) {
        mSiteRestClient.fetchConnectSiteInfo(payload);
    }

    private void handleFetchedConnectSiteInfo(ConnectSiteInfoPayload payload) {
        OnConnectSiteInfoChecked event = new OnConnectSiteInfoChecked(payload);
        event.error = payload.error;
        emitChange(event);
    }

    private void fetchWPComSiteByUrl(String payload) {
        mSiteRestClient.fetchWPComSiteByUrl(payload);
    }

    private void handleFetchedWPComSiteByUrl(FetchWPComSiteResponsePayload payload) {
        OnWPComSiteFetched event = new OnWPComSiteFetched(payload.checkedUrl, payload.site);
        event.error = payload.error;
        emitChange(event);
    }

    private void checkUrlIsWPCom(String payload) {
        mSiteRestClient.checkUrlIsWPCom(payload);
    }

    private void handleCheckedIsWPComUrl(IsWPComResponsePayload payload) {
        OnURLChecked event = new OnURLChecked(payload.url);
        if (payload.isError()) {
            // Return invalid site for all errors (this endpoint seems a bit drunk).
            // Client likely needs to know if there was an error or not.
            event.error = new SiteError(SiteErrorType.INVALID_SITE);
        }
        event.isWPCom = payload.isWPCom;
        emitChange(event);
    }

    private void suggestDomains(SuggestDomainsPayload payload) {
        mSiteRestClient.suggestDomains(payload.query, payload.onlyWordpressCom, payload.includeWordpressCom,
                payload.includeDotBlogSubdomain, payload.quantity);
    }

    private void handleSuggestedDomains(SuggestDomainsResponsePayload payload) {
        OnSuggestedDomains event = new OnSuggestedDomains(payload.query, payload.suggestions);
        if (payload.isError()) {
            if (payload.error instanceof WPComGsonRequest.WPComGsonNetworkError) {
                event.error = new SuggestDomainError(((WPComGsonNetworkError) payload.error).apiError,
                        payload.error.message);
            } else {
                event.error = new SuggestDomainError("", payload.error.message);
            }
        }
        emitChange(event);
    }

    // Automated Transfers

    private void checkAutomatedTransferEligibility(SiteModel site) {
        mSiteRestClient.checkAutomatedTransferEligibility(site);
    }

    private void handleCheckedAutomatedTransferEligibility(AutomatedTransferEligibilityResponsePayload payload) {
        OnAutomatedTransferAvailabilityChecked event;
        // The site might have been updated while the request was going on. We get a new copy the DB instead of using
        // the payload.site to avoid missing any updates to it.
        SiteModel siteModel = getSiteByLocalId(payload.site.getId());
        SiteError siteError = null;
        if (payload.isError()) {
            siteError = new SiteError(SiteErrorType.GENERIC_ERROR, payload.error.message);
        } else if (siteModel == null) {
            // This really shouldn't happen, because it'd mean that the user started a plugin install and immediately
            // deleted their site before the request can be completed which is almost impossible. We are still adding
            // it here as a sanity check and avoid a possible NPE however unlikely it is.
            siteError = new SiteError(SiteErrorType.UNKNOWN_SITE);
        } else {
            siteModel.setIsEligibleForAutomatedTransfer(payload.isEligible);
            try {
                SiteSqlUtils.insertOrUpdateSite(siteModel);
            } catch (DuplicateSiteException e) {
                siteError = new SiteError(SiteErrorType.DUPLICATE_SITE);
            }
        }
        event = new OnAutomatedTransferAvailabilityChecked(siteModel, siteError);
        emitChange(event);
    }

    private void initiateAutomatedTransfer(InitiateAutomatedTransferPayload payload) {
        mSiteRestClient.initiateAutomatedTransfer(payload.site, payload.pluginSlugToInstall);
    }

    private void checkAutomatedTransferStatus(SiteModel site) {
        mSiteRestClient.checkAutomatedTransferStatus(site);
    }
}
