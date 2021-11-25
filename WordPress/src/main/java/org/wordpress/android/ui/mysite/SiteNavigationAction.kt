package org.wordpress.android.ui.mysite

import androidx.annotation.StringRes
import com.wordpress.stories.compose.frame.StorySaveEvents.StorySaveResult
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.PagePostCreationSourcesDetail
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
    data class OpenAdmin(val site: SiteModel) : SiteNavigationAction()
    data class OpenPeople(val site: SiteModel) : SiteNavigationAction()
    data class OpenSharing(val site: SiteModel) : SiteNavigationAction()
    data class OpenDomains(val site: SiteModel) : SiteNavigationAction()
    data class OpenSiteSettings(val site: SiteModel) : SiteNavigationAction()
    data class OpenThemes(val site: SiteModel) : SiteNavigationAction()
    data class OpenPlugins(val site: SiteModel) : SiteNavigationAction()
    data class OpenMedia(val site: SiteModel) : SiteNavigationAction()
    data class OpenComments(val site: SiteModel) : SiteNavigationAction()
    data class OpenUnifiedComments(val site: SiteModel) : SiteNavigationAction()
    object StartWPComLoginForJetpackStats : SiteNavigationAction()
    data class OpenStats(val site: SiteModel) : SiteNavigationAction()
    data class ConnectJetpackForStats(val site: SiteModel) : SiteNavigationAction()
    data class OpenJetpackSettings(val site: SiteModel) : SiteNavigationAction()
    data class OpenStories(val site: SiteModel, val event: StorySaveResult) : SiteNavigationAction()
    data class AddNewStory(
        val site: SiteModel,
        val source: PagePostCreationSourcesDetail
    ) : SiteNavigationAction()

    data class AddNewStoryWithMediaIds(
        val site: SiteModel,
        val source: PagePostCreationSourcesDetail,
        val mediaIds: List<Long>
    ) : SiteNavigationAction()

    data class AddNewStoryWithMediaUris(
        val site: SiteModel,
        val source: PagePostCreationSourcesDetail,
        val mediaUris: List<String>
    ) : SiteNavigationAction()

    data class OpenDomainRegistration(val site: SiteModel) : SiteNavigationAction()
    data class AddNewSite(val isSignedInWpCom: Boolean) : SiteNavigationAction()
    data class ShowQuickStartDialog(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val positiveButtonLabel: Int,
        @StringRes val negativeButtonLabel: Int
    ) : SiteNavigationAction()
    data class OpenQuickStartFullScreenDialog(
        val type: QuickStartTaskType,
        @StringRes val title: Int
    ) : SiteNavigationAction()
    data class OpenDraftsPosts(val site: SiteModel) : SiteNavigationAction()
    data class OpenScheduledPosts(val site: SiteModel) : SiteNavigationAction()
    data class OpenEditorToCreateNewPost(val site: SiteModel) : SiteNavigationAction()
    data class EditPost(val site: SiteModel, val postId: Int) : SiteNavigationAction()
}
