package org.wordpress.android.ui.whatsnew

import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.store.WhatsNewStore
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewAppId.WP_ANDROID
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.StringUtils
import javax.inject.Inject

class FeatureAnnouncementProvider @Inject constructor(
    private val whatsNewStore: WhatsNewStore,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    suspend fun getLatestFeatureAnnouncement(fromCache: Boolean): FeatureAnnouncement? {
        return getFeatureAnnouncements(fromCache).firstOrNull()
    }

    suspend fun getFeatureAnnouncements(fromCache: Boolean): List<FeatureAnnouncement> {
        val featureAnnouncements = mutableListOf<FeatureAnnouncement>()

        val onWhatsNewFetched = if (fromCache) {
            whatsNewStore.fetchCachedAnnouncements()
        } else {
            whatsNewStore.fetchRemoteAnnouncements(
                    buildConfigWrapper.getAppVersionName(), WP_ANDROID
            )
        }
        onWhatsNewFetched.whatsNewItems?.map { featureAnnouncements.add(it.build()) }?.toList()
        return featureAnnouncements
    }

    fun WhatsNewAnnouncementModel.build(): FeatureAnnouncement {
        return FeatureAnnouncement(
                appVersionName,
                announcementVersion,
                minimumAppVersion,
                maximumAppVersion,
                appVersionTargets,
                detailsUrl,
                isLocalized,
                features.map { it.build() })
    }

    fun WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature.build(): FeatureAnnouncementItem {
        return FeatureAnnouncementItem(
                StringUtils.notNullStr(title),
                StringUtils.notNullStr(subtitle),
                StringUtils.notNullStr(iconBase64),
                StringUtils.notNullStr(iconUrl)
        )
    }
}
