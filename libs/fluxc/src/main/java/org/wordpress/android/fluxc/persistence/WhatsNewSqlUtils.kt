package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.WhatsNewAnnouncementFeatureTable
import com.wellsql.generated.WhatsNewAnnouncementTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsNewSqlUtils
@Inject constructor() {

    fun hasCachedAnnouncements(): Boolean {
        return WellSql.select(WhatsNewAnnouncementBuilder::class.java).count() > 0
    }

    fun getAnnouncements(): List<WhatsNewAnnouncementModel> {
        val announcements = mutableListOf<WhatsNewAnnouncementModel>()

        val announcementModels = WellSql.select(WhatsNewAnnouncementBuilder::class.java)
                .orderBy(WhatsNewAnnouncementTable.ANNOUNCEMENT_ID, SelectQuery.ORDER_ASCENDING)
                .asModel

        for (announcementModel in announcementModels) {
            val features = getAnnouncementFeatures(announcementModel.announcementId)
            announcements.add(announcementModel.build(features))
        }

        return announcements
    }

    private fun getAnnouncementFeatures(announcementId: Int): List<WhatsNewAnnouncementFeatureBuilder> {
        return WellSql.select(WhatsNewAnnouncementFeatureBuilder::class.java)
                .where().beginGroup()
                .equals(WhatsNewAnnouncementFeatureTable.ANNOUNCEMENT_ID, announcementId)
                .endGroup().endWhere()
                .asModel
    }

    fun updateAnnouncementCache(announcements: List<WhatsNewAnnouncementModel>?) {
        // we want the local store to be 1 to 1 representation of endpoint announcements
        WellSql.delete(WhatsNewAnnouncementBuilder::class.java).execute()
        WellSql.delete(WhatsNewAnnouncementFeatureBuilder::class.java).execute()

        if (announcements == null || announcements.isEmpty()) {
            return
        }

        val announcementBuilders = announcements.map { it.toBuilder() }

        val featureBuilders = mutableListOf<WhatsNewAnnouncementFeatureBuilder>()
        for (announcement in announcements) {
            featureBuilders.addAll(announcement.features.map { it.toBuilder(announcement.announcementVersion) })
        }

        WellSql.insert(announcementBuilders).asSingleTransaction(true).execute()
        WellSql.insert(featureBuilders).asSingleTransaction(true).execute()
    }

    @Table(name = "WhatsNewAnnouncement")
    class WhatsNewAnnouncementBuilder(
        @PrimaryKey(autoincrement = false) @Column var announcementId: Int = 0,
        @Column var appVersionName: String = "",
        @Column var minimumAppVersionCode: Int,
        @Column var localized: Boolean,
        @Column var responseLocale: String,
        @Column var detailsUrl: String? = null
    ) : Identifiable {
        constructor() : this(-1, "", -1, false, "", "")

        fun build(featuresBuilders: List<WhatsNewAnnouncementFeatureBuilder>): WhatsNewAnnouncementModel {
            val features = featuresBuilders.map { it.build() }
            return WhatsNewAnnouncementModel(
                    appVersionName,
                    announcementId,
                    minimumAppVersionCode,
                    detailsUrl,
                    localized,
                    responseLocale,
                    features
            )
        }

        override fun getId(): Int {
            return this.announcementId
        }

        override fun setId(id: Int) {
            this.announcementId = id
        }
    }

    @Table(name = "WhatsNewAnnouncementFeature")
    class WhatsNewAnnouncementFeatureBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var announcementId: Int = 0,
        @Column var title: String? = null,
        @Column var subtitle: String? = null,
        @Column var iconUrl: String? = null,
        @Column var iconBase64: String? = null
    ) : Identifiable {
        constructor() : this(-1, -1, "", "", "", "")

        fun build(): WhatsNewAnnouncementFeature {
            return WhatsNewAnnouncementFeature(title, subtitle, iconBase64, iconUrl)
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }

    private fun WhatsNewAnnouncementFeature.toBuilder(announcementId: Int): WhatsNewAnnouncementFeatureBuilder {
        return WhatsNewAnnouncementFeatureBuilder(
                announcementId = announcementId,
                title = this.title,
                subtitle = this.subtitle,
                iconBase64 = this.iconBase64,
                iconUrl = this.iconUrl
        )
    }

    private fun WhatsNewAnnouncementModel.toBuilder(): WhatsNewAnnouncementBuilder {
        return WhatsNewAnnouncementBuilder(
                announcementId = this.announcementVersion,
                appVersionName = this.appVersionName,
                minimumAppVersionCode = this.minimumAppVersionCode,
                localized = this.isLocalized,
                responseLocale = this.responseLocale,
                detailsUrl = this.detailsUrl
        )
    }
}
