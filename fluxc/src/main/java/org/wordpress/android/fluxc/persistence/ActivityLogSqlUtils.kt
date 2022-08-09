package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ActivityLogTable
import com.wellsql.generated.BackupDownloadStatusTable
import com.wellsql.generated.RewindStatusCredentialsTable
import com.wellsql.generated.RewindStatusTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.BackupDownloadStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Credentials
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogSqlUtils @Inject constructor(private val formattableContentMapper: FormattableContentMapper) {
    fun insertOrUpdateActivities(siteModel: SiteModel, activityModels: List<ActivityLogModel>): Int {
        val activityIds = activityModels.map { it.activityID }
        val activitiesToUpdate = WellSql.select(ActivityLogBuilder::class.java).where()
                .isIn(ActivityLogTable.ACTIVITY_ID, activityIds)
                .endWhere()
                .asModel
                .map { it.activityID }
        val (existing, new) = activityModels
                .map { it.toBuilder(siteModel) }
                .partition { activitiesToUpdate.contains(it.activityID) }
        val insertQuery = WellSql.insert(new)
        val updateQueries = existing.map {
            WellSql.update(ActivityLogBuilder::class.java)
                    .where()
                    .equals(ActivityLogTable.ACTIVITY_ID, it.activityID)
                    .equals(ActivityLogTable.LOCAL_SITE_ID, it.localSiteId)
                    .endWhere()
                    .put(it, UpdateAllExceptId<ActivityLogBuilder>(ActivityLogBuilder::class.java))
        }
        insertQuery.execute()
        return updateQueries.map { it.execute() }.sum() + new.count()
    }

    fun getActivitiesForSite(site: SiteModel, @SelectQuery.Order order: Int): List<ActivityLogModel> {
        return WellSql.select(ActivityLogBuilder::class.java)
                .where()
                .equals(ActivityLogTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(ActivityLogTable.PUBLISHED, order)
                .asModel
                .map { it.build(formattableContentMapper) }
    }

    fun getRewindableActivitiesForSite(site: SiteModel, @SelectQuery.Order order: Int): List<ActivityLogModel> {
        return WellSql.select(ActivityLogBuilder::class.java)
                .where()
                .equals(ActivityLogTable.LOCAL_SITE_ID, site.id)
                .equals(ActivityLogTable.REWINDABLE, true)
                .endWhere()
                .orderBy(ActivityLogTable.PUBLISHED, order)
                .asModel
                .map { it.build(formattableContentMapper) }
    }

    fun getActivityByRewindId(rewindId: String): ActivityLogModel? {
        return WellSql.select(ActivityLogBuilder::class.java)
                .where()
                .equals(ActivityLogTable.REWIND_ID, rewindId)
                .endWhere()
                .asModel
                .firstOrNull()
                ?.build(formattableContentMapper)
    }

    fun getActivityByActivityId(activityId: String): ActivityLogModel? {
        return WellSql.select(ActivityLogBuilder::class.java)
                .where()
                .equals(ActivityLogTable.ACTIVITY_ID, activityId)
                .endWhere()
                .asModel
                .firstOrNull()
                ?.build(formattableContentMapper)
    }

    fun deleteActivityLog(site: SiteModel): Int {
        return WellSql
                .delete(ActivityLogBuilder::class.java)
                .where()
                .equals(ActivityLogTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    fun deleteRewindStatus(site: SiteModel): Int {
        return WellSql
                .delete(RewindStatusBuilder::class.java)
                .where()
                .equals(RewindStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    fun deleteBackupDownloadStatus(site: SiteModel): Int {
        return WellSql
                .delete(BackupDownloadStatusBuilder::class.java)
                .where()
                .equals(BackupDownloadStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
    }

    fun replaceRewindStatus(site: SiteModel, rewindStatusModel: RewindStatusModel) {
        val rewindStatusBuilder = rewindStatusModel.toBuilder(site)
        WellSql.delete(RewindStatusBuilder::class.java)
                .where()
                .equals(RewindStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
        WellSql.delete(CredentialsBuilder::class.java)
                .where()
                .equals(RewindStatusCredentialsTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
        WellSql.insert(rewindStatusBuilder).execute()
        WellSql.insert(rewindStatusModel.credentials?.map { it.toBuilder(rewindStatusBuilder.id, site) } ?: listOf())
                .execute()
    }

    fun getRewindStatusForSite(site: SiteModel): RewindStatusModel? {
        val rewindStatusBuilder = getRewindStatusBuilder(site)
        val credentials = rewindStatusBuilder?.id?.let { getCredentialsBuilder(it) }
        return rewindStatusBuilder?.build(credentials?.map { it.build() })
    }

    fun getBackupDownloadStatusForSite(site: SiteModel): BackupDownloadStatusModel? {
        val downloadStatusBuilder = getBackupDownloadStatusBuilder(site)
        return downloadStatusBuilder?.build()
    }

    fun replaceBackupDownloadStatus(site: SiteModel, backupDownloadStatusModel: BackupDownloadStatusModel) {
        val backupDownloadStatusBuilder = backupDownloadStatusModel.toBuilder(site)
        WellSql.delete(BackupDownloadStatusBuilder::class.java)
                .where()
                .equals(BackupDownloadStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .execute()
        WellSql.insert(backupDownloadStatusBuilder).execute()
    }

    private fun getRewindStatusBuilder(site: SiteModel): RewindStatusBuilder? {
        return WellSql.select(RewindStatusBuilder::class.java)
                .where()
                .equals(RewindStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    private fun getCredentialsBuilder(rewindId: Int): List<CredentialsBuilder> {
        return WellSql.select(CredentialsBuilder::class.java)
                .where()
                .equals(RewindStatusCredentialsTable.REWIND_STATE_ID, rewindId)
                .endWhere()
                .asModel
    }

    private fun getBackupDownloadStatusBuilder(site: SiteModel): BackupDownloadStatusBuilder? {
        return WellSql.select(BackupDownloadStatusBuilder::class.java)
                .where()
                .equals(RewindStatusTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .asModel
                .firstOrNull()
    }

    private fun ActivityLogModel.toBuilder(site: SiteModel): ActivityLogBuilder {
        return ActivityLogBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                activityID = this.activityID,
                summary = this.summary,
                formattableContent = this.content?.let { formattableContentMapper.mapFormattableContentToJson(it) }
                        ?: "",
                name = this.name,
                type = this.type,
                gridicon = this.gridicon,
                status = this.status,
                rewindable = this.rewindable,
                rewindID = this.rewindID,
                published = this.published.time,
                displayName = this.actor?.displayName,
                actorType = this.actor?.type,
                wpcomUserID = this.actor?.wpcomUserID,
                avatarURL = this.actor?.avatarURL,
                role = this.actor?.role
        )
    }

    private fun RewindStatusModel.toBuilder(site: SiteModel): RewindStatusBuilder {
        return RewindStatusBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                state = this.state.value,
                lastUpdated = this.lastUpdated.time,
                reason = this.reason.value,
                canAutoconfigure = this.canAutoconfigure,
                rewindId = this.rewind?.rewindId,
                restoreId = this.rewind?.restoreId,
                rewindStatus = this.rewind?.status?.value,
                rewindProgress = this.rewind?.progress,
                rewindReason = this.rewind?.reason,
                message = this.rewind?.message,
                currentEntry = this.rewind?.currentEntry
        )
    }

    private fun Credentials.toBuilder(rewindStatusId: Int, site: SiteModel): CredentialsBuilder {
        return CredentialsBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                rewindStateId = rewindStatusId,
                type = this.type,
                role = this.role,
                host = this.host,
                port = this.port,
                stillValid = this.stillValid
        )
    }

    private fun BackupDownloadStatusModel.toBuilder(site: SiteModel): BackupDownloadStatusBuilder {
        return BackupDownloadStatusBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                downloadId = this.downloadId,
                rewindId = this.rewindId,
                backupPoint = this.backupPoint.time,
                startedAt = this.startedAt.time,
                progress = this.progress,
                downloadCount = this.downloadCount,
                validUntil = this.validUntil?.time,
                url = this.url
        )
    }

    @Table(name = "ActivityLog")
    data class ActivityLogBuilder(
        @PrimaryKey
        @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var activityID: String,
        @Column var summary: String,
        @Column var formattableContent: String,
        @Column var name: String? = null,
        @Column var type: String? = null,
        @Column var gridicon: String? = null,
        @Column var status: String? = null,
        @Column var rewindable: Boolean? = null,
        @Column var rewindID: String? = null,
        @Column var published: Long,
        @Column var discarded: Boolean? = null,
        @Column var displayName: String? = null,
        @Column var actorType: String? = null,
        @Column var wpcomUserID: Long? = null,
        @Column var avatarURL: String? = null,
        @Column var role: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, "", "", "", published = 0)

        override fun setId(id: Int) {
            mId = id
        }

        override fun getId() = mId

        @Suppress("ComplexCondition")
        fun build(formattableContentMapper: FormattableContentMapper): ActivityLogModel {
            val actor = if (
                actorType != null ||
                displayName != null ||
                wpcomUserID != null ||
                avatarURL != null ||
                role != null
            ) {
                ActivityLogModel.ActivityActor(displayName, actorType, wpcomUserID, avatarURL, role)
            } else {
                null
            }
            return ActivityLogModel(
                activityID,
                summary,
                formattableContentMapper.mapToFormattableContent(formattableContent),
                name,
                type,
                gridicon,
                status,
                rewindable,
                rewindID,
                Date(published),
                actor
            )
        }
    }

    @Table(name = "RewindStatus")
    data class RewindStatusBuilder(
        @PrimaryKey
        @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var state: String,
        @Column var lastUpdated: Long,
        @Column var reason: String? = null,
        @Column var canAutoconfigure: Boolean? = null,
        @Column var rewindId: String? = null,
        @Column var restoreId: Long? = null,
        @Column var rewindStatus: String? = null,
        @Column var rewindProgress: Int? = null,
        @Column var rewindReason: String? = null,
        @Column var message: String? = null,
        @Column var currentEntry: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, "", 0)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun build(credentials: List<Credentials>?): RewindStatusModel {
            val restoreStatus = RewindStatusModel.Rewind.build(
                    rewindId,
                    restoreId,
                    rewindStatus,
                    rewindProgress,
                    rewindReason,
                    message,
                    currentEntry
            )
            return RewindStatusModel(
                    RewindStatusModel.State.fromValue(state) ?: RewindStatusModel.State.UNKNOWN,
                    RewindStatusModel.Reason.fromValue(reason),
                    Date(lastUpdated),
                    canAutoconfigure,
                    credentials,
                    restoreStatus
            )
        }
    }

    @Table(name = "RewindStatusCredentials")
    data class CredentialsBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var rewindStateId: Int,
        @Column var type: String,
        @Column var role: String,
        @Column var stillValid: Boolean,
        @Column var host: String? = null,
        @Column var port: Int? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, 0, "", "", false)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun build(): Credentials {
            return Credentials(type, role, host, port, stillValid)
        }
    }

    @Table(name = "BackupDownloadStatus")
    data class BackupDownloadStatusBuilder(
        @PrimaryKey
        @Column private var mId: Int = -1,
        @Column var localSiteId: Int,
        @Column var remoteSiteId: Long,
        @Column var downloadId: Long,
        @Column var rewindId: String,
        @Column var backupPoint: Long,
        @Column var startedAt: Long,
        @Column var progress: Int? = null,
        @Column var downloadCount: Int? = null,
        @Column var validUntil: Long? = null,
        @Column var url: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0, 0, 0, "", 0, 0)

        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        fun build(): BackupDownloadStatusModel {
            return BackupDownloadStatusModel(
                    downloadId,
                    rewindId,
                    Date(backupPoint),
                    Date(startedAt),
                    progress,
                    downloadCount,
                    validUntil?.let {
                        Date(it)
                    },
                    url
            )
        }
    }
}
