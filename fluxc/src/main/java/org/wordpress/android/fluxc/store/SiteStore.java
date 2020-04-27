package org.wordpress.android.fluxc.store;

import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import org.wordpress.android.fluxc.model.PlanModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;
import org.wordpress.android.fluxc.network.rest.wpcom.site.DomainSuggestionResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie;
import org.wordpress.android.fluxc.network.rest.wpcom.site.AtomicCookie;
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookieResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.DeleteSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.ExportSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.FetchWPComSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.IsWPComResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient.NewSiteResponsePayload;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedCountryResponse;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SupportedStateResponse;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.PostSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.utils.SiteErrorUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SQLite based only. There is no in memory copy of mapped data, everything is queried from the DB.
 */
@Singleton
public class SiteStore extends Store {
    // Payloads
    public static class CompleteQuickStartPayload extends Payload<BaseNetworkError> {
        public SiteModel site;
        public String variant;

        public CompleteQuickStartPayload(@NonNull SiteModel site, String variant) {
            this.site = site;
            this.variant = variant;
        }
    }

    public static class RefreshSitesXMLRPCPayload extends Payload<BaseNetworkError> {
        public RefreshSitesXMLRPCPayload() {
        }

        public String username;
        public String password;
        public String url;
    }

    @SuppressWarnings("WeakerAccess")
    public static class NewSitePayload extends Payload<BaseNetworkError> {
        @NonNull public String siteName;
        @NonNull public String language;
        @NonNull public SiteVisibility visibility;
        @Nullable public Long segmentId;
        @NonNull public boolean dryRun;

        public NewSitePayload(@NonNull String siteName, @NonNull String language,
                              @NonNull SiteVisibility visibility, boolean dryRun) {
            this(siteName, language, visibility, null, dryRun);
        }

        public NewSitePayload(@NonNull String siteName, @NonNull String language,
                              @NonNull SiteVisibility visibility, @Nullable Long segmentId, boolean dryRun) {
            this.siteName = siteName;
            this.language = language;
            this.visibility = visibility;
            this.segmentId = segmentId;
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

    public static class DesignateMobileEditorForAllSitesPayload extends Payload<SiteEditorsError> {
        public String editor;
        public boolean setOnlyIfEmpty;

        public DesignateMobileEditorForAllSitesPayload(@NonNull String editorName) {
            this.editor = editorName;
            this.setOnlyIfEmpty = true;
        }

        public DesignateMobileEditorForAllSitesPayload(@NonNull String editorName, boolean setOnlyIfEmpty) {
            this.editor = editorName;
            this.setOnlyIfEmpty = setOnlyIfEmpty;
        }
    }

    public static class DesignateMobileEditorPayload extends Payload<SiteEditorsError> {
        public SiteModel site;
        public String editor;

        public DesignateMobileEditorPayload(@NonNull SiteModel site, @NonNull String editorName) {
            this.site = site;
            this.editor = editorName;
        }
    }

    public static class FetchedEditorsPayload extends Payload<SiteEditorsError> {
        public SiteModel site;
        public String webEditor;
        public String mobileEditor;

        public FetchedEditorsPayload(@NonNull SiteModel site, @NonNull String webEditor, @NonNull String mobileEditor) {
            this.site = site;
            this.mobileEditor = mobileEditor;
            this.webEditor = webEditor;
        }
    }

    public static class DesignateMobileEditorForAllSitesResponsePayload extends Payload<SiteEditorsError> {
        public Map<String, String> editors;

        public DesignateMobileEditorForAllSitesResponsePayload(Map<String, String> editors) {
            this.editors = editors;
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

    public static class FetchedPlansPayload extends Payload<PlansError> {
        public SiteModel site;
        @Nullable public List<PlanModel> plans;

        public FetchedPlansPayload(SiteModel site, @Nullable List<PlanModel> plans) {
            this.site = site;
            this.plans = plans;
        }

        public FetchedPlansPayload(SiteModel site, @NonNull PlansError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class FetchedPrivateAtomicCookiePayload extends Payload<PrivateAtomicCookieError> {
        public SiteModel site;
        @Nullable public PrivateAtomicCookieResponse cookie;

        public FetchedPrivateAtomicCookiePayload(SiteModel site, @Nullable PrivateAtomicCookieResponse cookie) {
            this.site = site;
            this.cookie = cookie;
        }
    }

    public static class FetchPrivateAtomicCookiePayload {
        public long siteId;

        public FetchPrivateAtomicCookiePayload(long siteId) {
            this.siteId = siteId;
        }
    }

    public static class SuggestDomainsPayload extends Payload<BaseNetworkError> {
        @NonNull public String query;
        @Nullable public Boolean onlyWordpressCom;
        @Nullable public Boolean includeWordpressCom;
        @Nullable public Boolean includeDotBlogSubdomain;
        @Nullable public String tlds;
        @Nullable public Long segmentId;
        public int quantity;
        public boolean includeVendorDot;

        public SuggestDomainsPayload(@NonNull String query, boolean onlyWordpressCom, boolean includeWordpressCom,
                                     boolean includeDotBlogSubdomain, int quantity, boolean includeVendorDot) {
            this.query = query;
            this.onlyWordpressCom = onlyWordpressCom;
            this.includeWordpressCom = includeWordpressCom;
            this.includeDotBlogSubdomain = includeDotBlogSubdomain;
            this.tlds = tlds;
            this.quantity = quantity;
            this.includeVendorDot = includeVendorDot;
        }

        public SuggestDomainsPayload(@NonNull String query, long segmentId, int quantity, boolean includeVendorDot) {
            this.query = query;
            this.segmentId = segmentId;
            this.quantity = quantity;
            this.includeVendorDot = includeVendorDot;
        }

        public SuggestDomainsPayload(@NonNull String query, int quantity, String tlds) {
            this.query = query;
            this.quantity = quantity;
            this.tlds = tlds;
        }
    }

    public static class SuggestDomainsResponsePayload extends Payload<SuggestDomainError> {
        public String query;
        public List<DomainSuggestionResponse> suggestions;

        public SuggestDomainsResponsePayload(@NonNull String query, SuggestDomainError error) {
            this.query = query;
            this.error = error;
            this.suggestions = new ArrayList<>();
        }

        public SuggestDomainsResponsePayload(@NonNull String query, List<DomainSuggestionResponse> suggestions) {
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
        public String urlAfterRedirects;

        public ConnectSiteInfoPayload(@NonNull String url, SiteError error) {
            this.url = url;
            this.error = error;
        }

        public String description() {
            return String.format("url: %s, e: %b, wp: %b, jp: %b, wpcom: %b, urlAfterRedirects: %s",
                    url, exists, isWordPress, hasJetpack, isWPCom, urlAfterRedirects);
        }
    }

    public static class DesignatePrimaryDomainPayload extends Payload<DesignatePrimaryDomainError> {
        public SiteModel site;
        public String domain;

        public DesignatePrimaryDomainPayload(SiteModel site, @NonNull String domainName) {
            this.site = site;
            this.domain = domainName;
        }
    }

    public static class InitiateAutomatedTransferPayload extends Payload<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public @NonNull String pluginSlugToInstall;

        public InitiateAutomatedTransferPayload(SiteModel site, String pluginSlugToInstall) {
            this.site = site;
            this.pluginSlugToInstall = pluginSlugToInstall;
        }
    }

    public static class AutomatedTransferEligibilityResponsePayload extends Payload<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public boolean isEligible;
        public @NonNull List<String> errorCodes;

        public AutomatedTransferEligibilityResponsePayload(@NonNull SiteModel site,
                                                           boolean isEligible,
                                                           @NonNull List<String> errors) {
            this.site = site;
            this.isEligible = isEligible;
            this.errorCodes = errors;
        }

        public AutomatedTransferEligibilityResponsePayload(@NonNull SiteModel site, AutomatedTransferError error) {
            this.site = site;
            this.error = error;
            this.errorCodes = new ArrayList<>();
        }
    }

    public static class InitiateAutomatedTransferResponsePayload extends Payload<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public @NonNull String pluginSlugToInstall;
        public boolean success;

        public InitiateAutomatedTransferResponsePayload(@NonNull SiteModel site, @NonNull String pluginSlugToInstall) {
            this.site = site;
            this.pluginSlugToInstall = pluginSlugToInstall;
        }
    }

    public static class AutomatedTransferStatusResponsePayload extends Payload<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public @NonNull String status;
        public int currentStep;
        public int totalSteps;

        public AutomatedTransferStatusResponsePayload(@NonNull SiteModel site,
                                                      @NonNull String status,
                                                      int currentStep,
                                                      int totalSteps) {
            this.site = site;
            this.status = status;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
        }

        public AutomatedTransferStatusResponsePayload(@NonNull SiteModel site, AutomatedTransferError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class DomainAvailabilityResponsePayload extends Payload<DomainAvailabilityError> {
        public @Nullable DomainAvailabilityStatus status;
        public @Nullable DomainMappabilityStatus mappable;
        public boolean supportsPrivacy;

        public DomainAvailabilityResponsePayload(@Nullable DomainAvailabilityStatus status,
                                                 @Nullable DomainMappabilityStatus mappable,
                                                 @Nullable boolean supportsPrivacy) {
            this.status = status;
            this.mappable = mappable;
            this.supportsPrivacy = supportsPrivacy;
        }

        public DomainAvailabilityResponsePayload(@NonNull DomainAvailabilityError error) {
            this.error = error;
        }
    }

    public static class DomainSupportedStatesResponsePayload extends Payload<DomainSupportedStatesError> {
        public @Nullable List<SupportedStateResponse> supportedStates;

        public DomainSupportedStatesResponsePayload(@Nullable List<SupportedStateResponse> supportedStates) {
            this.supportedStates = supportedStates;
        }

        public DomainSupportedStatesResponsePayload(@NonNull DomainSupportedStatesError error) {
            this.error = error;
        }
    }

    public static class DomainSupportedCountriesResponsePayload extends Payload<DomainSupportedCountriesError> {
        public List<SupportedCountryResponse> supportedCountries;

        public DomainSupportedCountriesResponsePayload(@Nullable List<SupportedCountryResponse> supportedCountries) {
            this.supportedCountries = supportedCountries;
        }

        public DomainSupportedCountriesResponsePayload(@NonNull DomainSupportedCountriesError error) {
            this.error = error;
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

    public static class SiteEditorsError implements OnChangedError {
        public SiteEditorsErrorType type;
        public String message;

        public SiteEditorsError(SiteEditorsErrorType type) {
            this(type, "");
        }

        SiteEditorsError(SiteEditorsErrorType type, String message) {
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

    public static class AutomatedTransferError implements OnChangedError {
        public final @NonNull AutomatedTransferErrorType type;
        public final @Nullable String message;

        public AutomatedTransferError(@Nullable String type, @Nullable String message) {
            this.type = AutomatedTransferErrorType.fromString(type);
            this.message = message;
        }
    }

    public static class DomainAvailabilityError implements OnChangedError {
        @NonNull public DomainAvailabilityErrorType type;
        @Nullable public String message;

        public DomainAvailabilityError(@NonNull DomainAvailabilityErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public DomainAvailabilityError(@NonNull DomainAvailabilityErrorType type) {
            this.type = type;
        }
    }

    public static class DomainSupportedStatesError implements OnChangedError {
        @NonNull public DomainSupportedStatesErrorType type;
        @Nullable public String message;

        public DomainSupportedStatesError(@NonNull DomainSupportedStatesErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }

        public DomainSupportedStatesError(@NonNull DomainSupportedStatesErrorType type) {
            this.type = type;
        }
    }

    public static class DomainSupportedCountriesError implements OnChangedError {
        @NonNull public DomainSupportedCountriesErrorType type;
        @Nullable public String message;

        public DomainSupportedCountriesError(
                @NonNull DomainSupportedCountriesErrorType type,
                @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class QuickStartError implements OnChangedError {
        @NonNull public QuickStartErrorType type;
        @Nullable public String message;

        public QuickStartError(@NonNull QuickStartErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class DesignatePrimaryDomainError implements OnChangedError {
        @NonNull public DesignatePrimaryDomainErrorType type;
        @Nullable public String message;

        public DesignatePrimaryDomainError(@NonNull DesignatePrimaryDomainErrorType type, @Nullable String message) {
            this.type = type;
            this.message = message;
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

    public static class OnSiteEditorsChanged extends OnChanged<SiteEditorsError> {
        public SiteModel site;
        public int rowsAffected;

        public OnSiteEditorsChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnAllSitesMobileEditorChanged extends OnChanged<SiteEditorsError> {
        public int rowsAffected;
        public boolean isNetworkResponse; // True when all sites are self-hosted or wpcom backend response

        public OnAllSitesMobileEditorChanged() {
        }
    }

    public static class OnUserRolesChanged extends OnChanged<UserRolesError> {
        public SiteModel site;

        public OnUserRolesChanged(SiteModel site) {
            this.site = site;
        }
    }

    public static class OnPlansFetched extends OnChanged<PlansError> {
        public SiteModel site;
        public @Nullable List<PlanModel> plans;

        public OnPlansFetched(SiteModel site, @Nullable List<PlanModel> plans, @Nullable PlansError error) {
            this.site = site;
            this.plans = plans;
            this.error = error;
        }
    }

    public static class OnPrivateAtomicCookieFetched extends OnChanged<PrivateAtomicCookieError> {
        public SiteModel site;
        public boolean success;

        public OnPrivateAtomicCookieFetched(@Nullable SiteModel site, boolean success,
                                            @Nullable PrivateAtomicCookieError error) {
            this.site = site;
            this.success = success;
            this.error = error;
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

    public static class OnDomainAvailabilityChecked extends OnChanged<DomainAvailabilityError> {
        public @Nullable DomainAvailabilityStatus status;
        public @Nullable DomainMappabilityStatus mappable;
        public boolean supportsPrivacy;

        public OnDomainAvailabilityChecked(@Nullable DomainAvailabilityStatus status,
                                           @Nullable DomainMappabilityStatus mappable,
                                           boolean supportsPrivacy,
                                           @Nullable DomainAvailabilityError error) {
            this.status = status;
            this.mappable = mappable;
            this.supportsPrivacy = supportsPrivacy;
            this.error = error;
        }
    }

    public enum DomainAvailabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        TLD_NOT_SUPPORTED,
        TRANSFERRABLE_DOMAIN,
        AVAILABLE,
        UNKNOWN_STATUS;

        public static DomainAvailabilityStatus fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                for (DomainAvailabilityStatus v : DomainAvailabilityStatus.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return UNKNOWN_STATUS;
        }
    }

    public enum DomainMappabilityStatus {
        BLACKLISTED_DOMAIN,
        INVALID_TLD,
        INVALID_DOMAIN,
        MAPPABLE_DOMAIN,
        UNKNOWN_STATUS;

        public static DomainMappabilityStatus fromString(final String string) {
            if (!TextUtils.isEmpty(string)) {
                for (DomainMappabilityStatus v : DomainMappabilityStatus.values()) {
                    if (string.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return UNKNOWN_STATUS;
        }
    }

    public static class OnDomainSupportedStatesFetched extends OnChanged<DomainSupportedStatesError> {
        public @Nullable List<SupportedStateResponse> supportedStates;

        public OnDomainSupportedStatesFetched(@Nullable List<SupportedStateResponse> supportedStates,
                                              @Nullable DomainSupportedStatesError error) {
            this.supportedStates = supportedStates;
            this.error = error;
        }
    }

    public static class OnDomainSupportedCountriesFetched extends OnChanged<DomainSupportedCountriesError> {
        public @Nullable List<SupportedCountryResponse> supportedCountries;

        public OnDomainSupportedCountriesFetched(@Nullable List<SupportedCountryResponse> supportedCountries,
                                                 @Nullable DomainSupportedCountriesError error) {
            this.supportedCountries = supportedCountries;
            this.error = error;
        }
    }

    public static class PlansError implements OnChangedError {
        @NonNull public PlansErrorType type;
        @Nullable public String message;

        public PlansError(@Nullable String type, @Nullable String message) {
            this.type = PlansErrorType.fromString(type);
            this.message = message;
        }

        public PlansError(@NonNull PlansErrorType type) {
            this.type = type;
        }
    }

    public static class PrivateAtomicCookieError implements OnChangedError {
        @NonNull public AccessCookieErrorType type;
        @Nullable public String message;

        public PrivateAtomicCookieError(@NonNull AccessCookieErrorType type, @NonNull String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static class OnAutomatedTransferEligibilityChecked extends OnChanged<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public boolean isEligible;
        public @NonNull List<String> eligibilityErrorCodes;

        public OnAutomatedTransferEligibilityChecked(@NonNull SiteModel site,
                                                     boolean isEligible,
                                                     @NonNull List<String> eligibilityErrorCodes,
                                                     @Nullable AutomatedTransferError error) {
            this.site = site;
            this.isEligible = isEligible;
            this.eligibilityErrorCodes = eligibilityErrorCodes;
            this.error = error;
        }
    }

    public static class OnAutomatedTransferInitiated extends OnChanged<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public @NonNull String pluginSlugToInstall;

        public OnAutomatedTransferInitiated(@NonNull SiteModel site,
                                            @NonNull String pluginSlugToInstall,
                                            AutomatedTransferError error) {
            this.site = site;
            this.pluginSlugToInstall = pluginSlugToInstall;
            this.error = error;
        }
    }

    public static class OnAutomatedTransferStatusChecked extends OnChanged<AutomatedTransferError> {
        public @NonNull SiteModel site;
        public boolean isCompleted;
        public int currentStep;
        public int totalSteps;

        public OnAutomatedTransferStatusChecked(@NonNull SiteModel site, boolean isCompleted, int currentStep,
                                                int totalSteps) {
            this.site = site;
            this.isCompleted = isCompleted;
            this.currentStep = currentStep;
            this.totalSteps = totalSteps;
        }

        public OnAutomatedTransferStatusChecked(@NonNull SiteModel site, AutomatedTransferError error) {
            this.site = site;
            this.error = error;
        }
    }

    public static class QuickStartCompletedResponsePayload extends OnChanged<QuickStartError> {
        public @NonNull SiteModel site;
        public boolean success;

        public QuickStartCompletedResponsePayload(@NonNull SiteModel site, boolean status) {
            this.site = site;
            this.success = status;
        }
    }

    public static class OnQuickStartCompleted extends OnChanged<QuickStartError> {
        public @NonNull SiteModel site;
        public boolean success;

        OnQuickStartCompleted(@NonNull SiteModel site, boolean status) {
            this.site = site;
            this.success = status;
        }
    }

    public static class DesignatedPrimaryDomainPayload extends OnChanged<DesignatePrimaryDomainError> {
        public @NonNull SiteModel site;
        public boolean success;

        public DesignatedPrimaryDomainPayload(@NonNull SiteModel site, boolean status) {
            this.site = site;
            this.success = status;
        }
    }

    public static class OnPrimaryDomainDesignated extends OnChanged<DesignatePrimaryDomainError> {
        public @NonNull SiteModel site;
        public boolean success;

        public OnPrimaryDomainDesignated(@NonNull SiteModel site, boolean status) {
            this.site = site;
            this.success = status;
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
        EMPTY_RESULTS,
        EMPTY_QUERY,
        INVALID_MINIMUM_QUANTITY,
        INVALID_MAXIMUM_QUANTITY,
        INVALID_QUERY,
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

    public enum PlansErrorType {
        NOT_AVAILABLE,
        AUTHORIZATION_REQUIRED,
        UNAUTHORIZED,
        UNKNOWN_BLOG,
        GENERIC_ERROR;

        public static PlansErrorType fromString(String type) {
            if (!TextUtils.isEmpty(type)) {
                for (PlansErrorType v : PlansErrorType.values()) {
                    if (type.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum AccessCookieErrorType {
        GENERIC_ERROR,
        INVALID_RESPONSE,
        SITE_MISSING_FROM_STORE,
        NON_PRIVATE_AT_SITE
    }

    public enum UserRolesErrorType {
        GENERIC_ERROR
    }

    public enum SiteEditorsErrorType {
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

    public enum AutomatedTransferErrorType {
        AT_NOT_ELIGIBLE, // occurs if AT is initiated when the site is not eligible
        NOT_FOUND, // occurs if transfer status of a site with no active transfer is checked
        GENERIC_ERROR;

        public static AutomatedTransferErrorType fromString(String type) {
            if (!TextUtils.isEmpty(type)) {
                for (AutomatedTransferErrorType v : AutomatedTransferErrorType.values()) {
                    if (type.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum DomainAvailabilityErrorType {
        INVALID_DOMAIN_NAME,
        GENERIC_ERROR
    }

    public enum DomainSupportedStatesErrorType {
        INVALID_COUNTRY_CODE,
        INVALID_QUERY,
        GENERIC_ERROR;

        public static DomainSupportedStatesErrorType fromString(String type) {
            if (!TextUtils.isEmpty(type)) {
                for (DomainSupportedStatesErrorType v : DomainSupportedStatesErrorType.values()) {
                    if (type.equalsIgnoreCase(v.name())) {
                        return v;
                    }
                }
            }
            return GENERIC_ERROR;
        }
    }

    public enum DomainSupportedCountriesErrorType {
        GENERIC_ERROR
    }

    public enum QuickStartErrorType {
        GENERIC_ERROR
    }

    public enum DesignatePrimaryDomainErrorType {
        GENERIC_ERROR
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

    public enum CompleteQuickStartVariant {
        NEXT_STEPS("next-steps");

        private final String mString;

        CompleteQuickStartVariant(final String s) {
            mString = s;
        }

        @Override
        public String toString() {
            return mString;
        }
    }

    private SiteRestClient mSiteRestClient;
    private SiteXMLRPCClient mSiteXMLRPCClient;
    private PostSqlUtils mPostSqlUtils;
    private PrivateAtomicCookie mPrivateAtomicCookie;

    @Inject
    public SiteStore(Dispatcher dispatcher, PostSqlUtils postSqlUtils, SiteRestClient siteRestClient,
                     SiteXMLRPCClient siteXMLRPCClient, PrivateAtomicCookie privateAtomicCookie) {
        super(dispatcher);
        mSiteRestClient = siteRestClient;
        mSiteXMLRPCClient = siteXMLRPCClient;
        mPostSqlUtils = postSqlUtils;
        mPrivateAtomicCookie = privateAtomicCookie;
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
     * Returns the number of sites of any kind in the store.
     */
    public int getSitesCount() {
        return (int) WellSql.select(SiteModel.class).count();
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
        return SiteSqlUtils.getSitesWith(SiteModelTable.ID, id).exists();
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
        return (int) SiteSqlUtils.getSitesAccessedViaWPComRest().count();
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
        return (int) SiteSqlUtils.getSitesWith(SiteModelTable.IS_WPCOM, true).count();
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
        return (int) SiteSqlUtils.getSitesAccessedViaXMLRPC().count();
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
        return (int) SiteSqlUtils.getSitesWith(SiteModelTable.IS_VISIBLE, true).count();
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
        return (int) SiteSqlUtils.getVisibleSitesAccessedViaWPCom().count();
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
                      .exists();
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
                                                siteModel.setSiteId(
                                                        cursor.getInt(cursor.getColumnIndex(SiteModelTable.SITE_ID)));
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
            case FETCH_SITE_EDITORS:
                fetchSiteEditors((SiteModel) action.getPayload());
                break;
            case DESIGNATE_MOBILE_EDITOR:
                designateMobileEditor((DesignateMobileEditorPayload) action.getPayload());
                break;
            case DESIGNATE_MOBILE_EDITOR_FOR_ALL_SITES:
                designateMobileEditorForAllSites((DesignateMobileEditorForAllSitesPayload) action.getPayload());
                break;
            case FETCHED_SITE_EDITORS:
                updateSiteEditors((FetchedEditorsPayload) action.getPayload());
                break;
            case DESIGNATED_MOBILE_EDITOR_FOR_ALL_SITES:
                handleDesignatedMobileEditorForAllSites(
                        (DesignateMobileEditorForAllSitesResponsePayload) action.getPayload());
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
            case FETCH_PLANS:
                fetchPlans((SiteModel) action.getPayload());
                break;
            case FETCHED_PLANS:
                handleFetchedPlans((FetchedPlansPayload) action.getPayload());
                break;
            case CHECK_DOMAIN_AVAILABILITY:
                checkDomainAvailability((String) action.getPayload());
                break;
            case CHECKED_DOMAIN_AVAILABILITY:
                handleCheckedDomainAvailability((DomainAvailabilityResponsePayload) action.getPayload());
                break;
            case FETCH_DOMAIN_SUPPORTED_STATES:
                fetchSupportedStates((String) action.getPayload());
                break;
            case FETCHED_DOMAIN_SUPPORTED_STATES:
                handleFetchedSupportedStates((DomainSupportedStatesResponsePayload) action.getPayload());
                break;
            case FETCH_DOMAIN_SUPPORTED_COUNTRIES:
                mSiteRestClient.fetchSupportedCountries();
                break;
            case FETCHED_DOMAIN_SUPPORTED_COUNTRIES:
                handleFetchedSupportedCountries((DomainSupportedCountriesResponsePayload) action.getPayload());
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
            case INITIATED_AUTOMATED_TRANSFER:
                handleInitiatedAutomatedTransfer((InitiateAutomatedTransferResponsePayload) action.getPayload());
                break;
            case CHECKED_AUTOMATED_TRANSFER_STATUS:
                handleCheckedAutomatedTransferStatus((AutomatedTransferStatusResponsePayload) action.getPayload());
                break;
            case COMPLETE_QUICK_START:
                completeQuickStart((CompleteQuickStartPayload) action.getPayload());
                break;
            case COMPLETED_QUICK_START:
                handleQuickStartCompleted((QuickStartCompletedResponsePayload) action.getPayload());
                break;
            case DESIGNATE_PRIMARY_DOMAIN:
                designatePrimaryDomain((DesignatePrimaryDomainPayload) action.getPayload());
                break;
            case DESIGNATED_PRIMARY_DOMAIN:
                handleDesignatedPrimaryDomain((DesignatedPrimaryDomainPayload) action.getPayload());
                break;
            case FETCH_PRIVATE_ATOMIC_COOKIE:
                fetchPrivateAtomicCookie((FetchPrivateAtomicCookiePayload) action.getPayload());
                break;
            case FETCHED_PRIVATE_ATOMIC_COOKIE:
                handleFetchedPrivateAtomicCookie((FetchedPrivateAtomicCookiePayload) action.getPayload());
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
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                SiteModel freshSiteFromDB = getSiteByLocalId(siteModel.getId());
                if (freshSiteFromDB != null) {
                    siteModel.setMobileEditor(freshSiteFromDB.getMobileEditor());
                    siteModel.setWebEditor(freshSiteFromDB.getWebEditor());
                }
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
            SiteSqlUtils.removeWPComRestSitesAbsentFromList(mPostSqlUtils, fetchedSites.getSites());
        }
        emitChange(event);
    }

    private UpdateSitesResult createOrUpdateSites(SitesModel sites) {
        UpdateSitesResult result = new UpdateSitesResult();
        for (SiteModel site : sites.getSites()) {
            try {
                // The REST API doesn't return info about the editor(s). Make sure to copy current values
                // available on the DB. Otherwise the apps will receive an update site without editor prefs set.
                // The apps will dispatch the action to update editor(s) when necessary.
                SiteModel siteFromDB = getSiteBySiteId(site.getSiteId());
                if (siteFromDB != null) {
                    site.setMobileEditor(siteFromDB.getMobileEditor());
                    site.setWebEditor(siteFromDB.getWebEditor());
                }
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
        mSiteRestClient.newSite(payload.siteName, payload.language, payload.visibility,
                payload.segmentId, payload.dryRun);
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

    private void fetchSiteEditors(SiteModel site) {
        if (site.isUsingWpComRestApi()) {
            mSiteRestClient.fetchSiteEditors(site);
        }
    }

    private void designateMobileEditor(DesignateMobileEditorPayload payload) {
        // wpcom sites sync the new value with the backend
        if (payload.site.isUsingWpComRestApi()) {
            mSiteRestClient.designateMobileEditor(payload.site, payload.editor);
        }

        // Update the editor pref on the DB, and emit the change immediately
        SiteModel site = payload.site;
        site.setMobileEditor(payload.editor);
        OnSiteEditorsChanged event = new OnSiteEditorsChanged(site);
        try {
            event.rowsAffected = SiteSqlUtils.insertOrUpdateSite(site);
        } catch (Exception e) {
            event.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
        }
        emitChange(event);
    }

    private void designateMobileEditorForAllSites(DesignateMobileEditorForAllSitesPayload payload) {
        int rowsAffected = 0;
        OnAllSitesMobileEditorChanged event = new OnAllSitesMobileEditorChanged();
        boolean wpcomPostRequestRequired = false;
        for (SiteModel site : getSites()) {
            site.setMobileEditor(payload.editor);
            if (!wpcomPostRequestRequired && site.isUsingWpComRestApi()) {
                wpcomPostRequestRequired = true;
            }
            try {
                rowsAffected += SiteSqlUtils.insertOrUpdateSite(site);
            } catch (Exception e) {
                event.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
            }
        }

        if (wpcomPostRequestRequired) {
            mSiteRestClient.designateMobileEditorForAllSites(payload.editor, payload.setOnlyIfEmpty);
            event.isNetworkResponse = false;
        } else {
            event.isNetworkResponse = true;
        }

        event.rowsAffected = rowsAffected;
        emitChange(event);
    }

    private void updateSiteEditors(FetchedEditorsPayload payload) {
        SiteModel site = payload.site;
        OnSiteEditorsChanged event = new OnSiteEditorsChanged(site);
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            site.setMobileEditor(payload.mobileEditor);
            site.setWebEditor(payload.webEditor);
            try {
                event.rowsAffected = SiteSqlUtils.insertOrUpdateSite(site);
            } catch (Exception e) {
                event.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
            }
        }

        emitChange(event);
    }

    private void handleDesignatedMobileEditorForAllSites(DesignateMobileEditorForAllSitesResponsePayload payload) {
        OnAllSitesMobileEditorChanged event = new OnAllSitesMobileEditorChanged();
        if (payload.isError()) {
            event.error = payload.error;
        } else {
            // Loop over the returned sites and make sure we've the fresh values for editor prop stored locally
            for (Map.Entry<String, String> entry : payload.editors.entrySet()) {
                SiteModel currentModel = getSiteBySiteId(Long.parseLong(entry.getKey()));

                if (currentModel == null) {
                    // this could happen when a site was added to the current account with another app, or on the web
                    AppLog.e(T.API, "handleDesignatedMobileEditorForAllSites - The backend returned info for "
                                    + "the following siteID " + entry.getKey() + " but there is no site with that "
                                    + "remote ID in SiteStore.");
                    continue;
                }

                if (currentModel.getMobileEditor() == null
                    || !currentModel.getMobileEditor().equals(entry.getValue())) {
                    // the current editor is either null or != from the value on the server. Update it
                    currentModel.setMobileEditor(entry.getValue());
                    try {
                        event.rowsAffected += SiteSqlUtils.insertOrUpdateSite(currentModel);
                    } catch (Exception e) {
                        event.error = new SiteEditorsError(SiteEditorsErrorType.GENERIC_ERROR);
                    }
                }
            }
        }
        event.isNetworkResponse = true;
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
                payload.includeDotBlogSubdomain, payload.segmentId, payload.quantity, payload.includeVendorDot,
                payload.tlds);
    }

    private void handleSuggestedDomains(SuggestDomainsResponsePayload payload) {
        OnSuggestedDomains event = new OnSuggestedDomains(payload.query, payload.suggestions);
        if (payload.isError()) {
            event.error = payload.error;
        }
        emitChange(event);
    }

    private void fetchPrivateAtomicCookie(FetchPrivateAtomicCookiePayload payload) {
        SiteModel site = getSiteBySiteId(payload.siteId);

        if (site == null) {
            PrivateAtomicCookieError cookieError = new PrivateAtomicCookieError(
                    AccessCookieErrorType.SITE_MISSING_FROM_STORE,
                    "Requested site is missing from the store.");
            emitChange(new OnPrivateAtomicCookieFetched(null, false, cookieError));
            return;
        }

        if (!site.isPrivateWPComAtomic()) {
            PrivateAtomicCookieError cookieError = new PrivateAtomicCookieError(
                    AccessCookieErrorType.NON_PRIVATE_AT_SITE,
                    "Cookie can only be requested for private atomic site.");
            emitChange(new OnPrivateAtomicCookieFetched(site, false, cookieError));
            return;
        }

        mSiteRestClient.fetchAccessCookie(site);
    }

    private void handleFetchedPrivateAtomicCookie(FetchedPrivateAtomicCookiePayload payload) {
        if (payload.cookie == null || payload.cookie.getCookies().isEmpty()) {
            emitChange(new OnPrivateAtomicCookieFetched(payload.site, false,
                    new PrivateAtomicCookieError(AccessCookieErrorType.INVALID_RESPONSE,
                            "Cookie is missing from response.")));
             mPrivateAtomicCookie.set(null);
            return;
        }

        AtomicCookie siteCookie = payload.cookie.getCookies().get(0);
        mPrivateAtomicCookie.set(siteCookie);
        emitChange(new OnPrivateAtomicCookieFetched(payload.site, true, payload.error));
    }

    private void fetchPlans(SiteModel siteModel) {
        if (siteModel.isUsingWpComRestApi()) {
            mSiteRestClient.fetchPlans(siteModel);
        } else {
            PlansError plansError = new PlansError(PlansErrorType.NOT_AVAILABLE);
            handleFetchedPlans(new FetchedPlansPayload(siteModel, plansError));
        }
    }

    private void handleFetchedPlans(FetchedPlansPayload payload) {
        emitChange(new OnPlansFetched(payload.site, payload.plans, payload.error));
    }

    private void checkDomainAvailability(String domainName) {
        if (TextUtils.isEmpty(domainName)) {
            DomainAvailabilityError error =
                    new DomainAvailabilityError(DomainAvailabilityErrorType.INVALID_DOMAIN_NAME);
            handleCheckedDomainAvailability(new DomainAvailabilityResponsePayload(error));
        } else {
            mSiteRestClient.checkDomainAvailability(domainName);
        }
    }

    private void handleCheckedDomainAvailability(DomainAvailabilityResponsePayload payload) {
        emitChange(
                new OnDomainAvailabilityChecked(
                        payload.status,
                        payload.mappable,
                        payload.supportsPrivacy,
                        payload.error));
    }

    private void fetchSupportedStates(String countryCode) {
        if (TextUtils.isEmpty(countryCode)) {
            DomainSupportedStatesError error =
                    new DomainSupportedStatesError(DomainSupportedStatesErrorType.INVALID_COUNTRY_CODE);
            handleFetchedSupportedStates(new DomainSupportedStatesResponsePayload(error));
        } else {
            mSiteRestClient.fetchSupportedStates(countryCode);
        }
    }

    private void handleFetchedSupportedStates(DomainSupportedStatesResponsePayload payload) {
        emitChange(new OnDomainSupportedStatesFetched(payload.supportedStates, payload.error));
    }

    private void handleFetchedSupportedCountries(DomainSupportedCountriesResponsePayload payload) {
        emitChange(new OnDomainSupportedCountriesFetched(payload.supportedCountries, payload.error));
    }

    // Automated Transfers

    private void checkAutomatedTransferEligibility(SiteModel site) {
        mSiteRestClient.checkAutomatedTransferEligibility(site);
    }

    private void handleCheckedAutomatedTransferEligibility(AutomatedTransferEligibilityResponsePayload payload) {
        emitChange(new OnAutomatedTransferEligibilityChecked(payload.site, payload.isEligible, payload.errorCodes,
                payload.error));
    }

    private void initiateAutomatedTransfer(InitiateAutomatedTransferPayload payload) {
        mSiteRestClient.initiateAutomatedTransfer(payload.site, payload.pluginSlugToInstall);
    }

    private void handleInitiatedAutomatedTransfer(InitiateAutomatedTransferResponsePayload payload) {
        emitChange(new OnAutomatedTransferInitiated(payload.site, payload.pluginSlugToInstall, payload.error));
    }

    private void checkAutomatedTransferStatus(SiteModel site) {
        mSiteRestClient.checkAutomatedTransferStatus(site);
    }

    private void handleCheckedAutomatedTransferStatus(AutomatedTransferStatusResponsePayload payload) {
        OnAutomatedTransferStatusChecked event;
        if (!payload.isError()) {
            // We can't rely on the currentStep and totalSteps as it may not be equal when the transfer is complete
            boolean isTransferCompleted = payload.status.equalsIgnoreCase("complete");
            event = new OnAutomatedTransferStatusChecked(payload.site, isTransferCompleted, payload.currentStep,
                    payload.totalSteps);
        } else {
            event = new OnAutomatedTransferStatusChecked(payload.site, payload.error);
        }
        emitChange(event);
    }

    private void completeQuickStart(@NonNull CompleteQuickStartPayload payload) {
        mSiteRestClient.completeQuickStart(payload.site, payload.variant);
    }

    private void handleQuickStartCompleted(QuickStartCompletedResponsePayload payload) {
        OnQuickStartCompleted event = new OnQuickStartCompleted(payload.site, payload.success);
        event.error = payload.error;
        emitChange(event);
    }

    private void designatePrimaryDomain(@NonNull DesignatePrimaryDomainPayload payload) {
        mSiteRestClient.designatePrimaryDomain(payload.site, payload.domain);
    }

    private void handleDesignatedPrimaryDomain(@NonNull DesignatedPrimaryDomainPayload payload) {
        OnPrimaryDomainDesignated event = new OnPrimaryDomainDesignated(payload.site, payload.success);
        event.error = payload.error;
        emitChange(event);
    }
}
