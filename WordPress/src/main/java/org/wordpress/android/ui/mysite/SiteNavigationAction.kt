package org.wordpress.android.ui.mysite

import androidx.annotation.StringRes
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
import org.wordpress.android.ui.quickstart.QuickStartEvent
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.util.UriWrapper

sealed class SiteNavigationAction {
    object OpenMeScreen : SiteNavigationAction()
    data class OpenSite(val site: SiteModel) : SiteNavigationAction()
    data class OpenSitePicker(val site: SiteModel) : SiteNavigationAction()
    data class OpenMediaPicker(val site: SiteModel) : SiteNavigationAction()
    data class OpenCropActivity(val imageUri: UriWrapper) : SiteNavigationAction()
    data class OpenActivityLog(val site: SiteModel) : SiteNavigationAction()
    data class OpenBackup(val site: SiteModel) : SiteNavigationAction()
    data class OpenScan(val site: SiteModel) : SiteNavigationAction()
    data class OpenPlan(val site: SiteModel) : SiteNavigationAction()
    data class OpenPosts(val site: SiteModel) : SiteNavigationAction()
    data class OpenPages(val site: SiteModel) : SiteNavigationAction()
    data class OpenHomepage(
        val site: SiteModel,
        val homepageLocalId: Int,
        val isNewSite: Boolean
    ) : SiteNavigationAction()

    data class OpenAdmin(val site: SiteModel) : SiteNavigationAction()
    data class OpenPeople(val site: SiteModel) : SiteNavigationAction()
    data class OpenSelfHostedUsers(val site: SiteModel) : SiteNavigationAction()
    data class OpenSharing(val site: SiteModel) : SiteNavigationAction()
    data class OpenDomains(val site: SiteModel) : SiteNavigationAction()
    data class OpenSiteSettings(val site: SiteModel) : SiteNavigationAction()
    data class OpenThemes(val site: SiteModel) : SiteNavigationAction()
    data class OpenPlugins(val site: SiteModel) : SiteNavigationAction()
    data class OpenMedia(val site: SiteModel) : SiteNavigationAction()
    data class OpenMore(val site:SiteModel, val quickStartEvent: QuickStartEvent?) : SiteNavigationAction()
    data class OpenUnifiedComments(val site: SiteModel) : SiteNavigationAction()
    object StartWPComLoginForJetpackStats : SiteNavigationAction()
    data class OpenStats(val site: SiteModel) : SiteNavigationAction()
    data class ConnectJetpackForStats(val site: SiteModel) : SiteNavigationAction()
    data class OpenDomainRegistration(val site: SiteModel) : SiteNavigationAction()
    data class OpenPaidDomainSearch(val site: SiteModel) : SiteNavigationAction()
    data class OpenFreeDomainSearch(val site: SiteModel) : SiteNavigationAction()
    data class AddNewSite(val hasAccessToken: Boolean, val source: SiteCreationSource) : SiteNavigationAction()
    data class ShowQuickStartDialog(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val positiveButtonLabel: Int,
        @StringRes val negativeButtonLabel: Int,
        val isNewSite: Boolean
    ) : SiteNavigationAction()

    data class OpenQuickStartFullScreenDialog(
        val type: QuickStartTaskType,
        @StringRes val title: Int
    ) : SiteNavigationAction()

    data class OpenDraftsPosts(val site: SiteModel) : SiteNavigationAction()
    data class OpenScheduledPosts(val site: SiteModel) : SiteNavigationAction()
    data class EditDraftPost(val site: SiteModel, val postId: Int) : SiteNavigationAction()
    data class EditScheduledPost(val site: SiteModel, val postId: Int) : SiteNavigationAction()
    data class OpenStatsByDay(val site: SiteModel) : SiteNavigationAction()
    data class OpenExternalUrl(val url: String) : SiteNavigationAction()
    data class OpenUrlInWebView(val url: String) : SiteNavigationAction()
    data class OpenDeepLink(val url: String) : SiteNavigationAction()
    object OpenJetpackPoweredBottomSheet : SiteNavigationAction()
    object OpenJetpackMigrationDeleteWP : SiteNavigationAction()
    data class OpenJetpackFeatureOverlay(val source: JetpackFeatureCollectionOverlaySource) : SiteNavigationAction()
    data class OpenPromoteWithBlazeOverlay(val source: BlazeFlowSource, val shouldShowBlazeOverlay: Boolean = false) :
        SiteNavigationAction()

    object ShowJetpackRemovalStaticPostersView : SiteNavigationAction()
    data class OpenActivityLogDetail(val site: SiteModel, val activityId: String, val isRewindable: Boolean) :
        SiteNavigationAction()

    data class TriggerCreatePageFlow(val site: SiteModel) : SiteNavigationAction()
    data class OpenPagesDraftsTab(val site: SiteModel, val pageId: Int) : SiteNavigationAction()
    data class OpenPagesScheduledTab(val site: SiteModel, val pageId: Int) : SiteNavigationAction()
    data class OpenCampaignListingPage(val campaignListingPageSource: CampaignListingPageSource) :
        SiteNavigationAction()

    data class OpenCampaignDetailPage(val campaignId: String, val campaignDetailPageSource: CampaignDetailPageSource) :
        SiteNavigationAction()

    object OpenDashboardPersonalization : SiteNavigationAction()

    data class OpenBloganuaryNudgeOverlay(val isPromptsEnabled: Boolean): SiteNavigationAction()
    data class OpenSiteMonitoring(val site: SiteModel) : SiteNavigationAction()
}

sealed class BloggingPromptCardNavigationAction: SiteNavigationAction() {
    data class SharePrompt(val message: String) : BloggingPromptCardNavigationAction()
    data class AnswerPrompt(val selectedSite: SiteModel, val promptId: Int) :
        BloggingPromptCardNavigationAction()
    data class ViewAnswers(val readerTag: ReaderTag): BloggingPromptCardNavigationAction()
    object LearnMore: BloggingPromptCardNavigationAction()
    object ViewMore: BloggingPromptCardNavigationAction()
    data class CardRemoved(val undoClick: ()-> Unit): BloggingPromptCardNavigationAction()
}
