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
    suspend fun getLatestFeatureAnnouncement(): FeatureAnnouncement? {
        return getFeatureAnnouncements().firstOrNull()
    }

    suspend fun getFeatureAnnouncements(): List<FeatureAnnouncement> {
        val featureAnnouncements = mutableListOf<FeatureAnnouncement>()
        val onWhatsNewFetched = whatsNewStore.fetchWhatsNew(
                buildConfigWrapper.getAppVersionCode().toString(), WP_ANDROID,false
        )
        onWhatsNewFetched.whatsNewItems?.map { featureAnnouncements.add(it.build()) }?.toList()
        return featureAnnouncements
    }

    fun WhatsNewAnnouncementModel.build(): FeatureAnnouncement {
        return FeatureAnnouncement(
                appVersionName,
                announcementVersion,
                minimumAppVersion,
                maximumAppVersion,
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

    suspend fun isAnnouncementOnUpgradeAvailable(): Boolean {
        val announcements = getFeatureAnnouncements()
        return announcements.isNotEmpty() && announcements[0].isLocalized && announcements[0].features.isNotEmpty()
    }
}
