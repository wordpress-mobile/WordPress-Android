package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ActivityLogTable
import com.wellsql.generated.RewindStatusTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogSqlUtils
@Inject constructor() {
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
                .map { it.build() }
    }

    fun deleteActivityLog(): Int {
        return WellSql.delete(ActivityLogBuilder::class.java).execute()
    }

    fun insertOrUpdateRewindStatus(site: SiteModel, rewindStatusModel: RewindStatusModel) {
        val existingRewindStatus = getRewindStatusBuilder(site)
        val rewindStatusBuilder = rewindStatusModel.toBuilder(site)
        if (existingRewindStatus != null) {
            WellSql.update(RewindStatusBuilder::class.java)
                    .where()
                    .equals(RewindStatusTable.ID, existingRewindStatus.id)
                    .equals(ActivityLogTable.LOCAL_SITE_ID, existingRewindStatus.localSiteId)
                    .endWhere()
                    .put(rewindStatusBuilder, UpdateAllExceptId<RewindStatusBuilder>(RewindStatusBuilder::class.java))
        } else {
            WellSql.insert(rewindStatusBuilder)
        }
    }

    fun getRewindStatusForSite(site: SiteModel): RewindStatusModel? {
        return getRewindStatusBuilder(site)?.build()
    }

    private fun getRewindStatusBuilder(site: SiteModel): RewindStatusBuilder? {
        return WellSql.select(RewindStatusBuilder::class.java)
                .where()
                .equals(ActivityLogTable.LOCAL_SITE_ID, site.id)
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
                text = this.text,
                name = this.name,
                type = this.type,
                gridicon = this.gridicon,
                status = this.status,
                rewindable = this.rewindable,
                rewindID = this.rewindID,
                published = this.published.time,
                discarded = this.discarded,
                displayName = this.actor?.displayName,
                actorType = this.actor?.type,
                wpcomUserID = this.actor?.wpcomUserID,
                avatarURL = this.actor?.avatarURL,
                role = this.actor?.role)
    }

    private fun RewindStatusModel.toBuilder(site: SiteModel): RewindStatusBuilder {
        return RewindStatusBuilder(
                localSiteId = site.id,
                remoteSiteId = site.siteId,
                rewindState = this.state.value,
                reason = this.reason,
                restoreId = this.restore?.id,
                restoreFailureReason = this.restore?.failureReason,
                restoreMessage = this.restore?.message,
                restoreProgress = this.restore?.progress,
                restoreErrorCode = this.restore?.errorCode,
                restoreState = this.restore?.status?.value)
    }

    @Table(name = "ActivityLog")
    data class ActivityLogBuilder(@PrimaryKey
                                  @Column private var mId: Int = -1,
                                  @Column var localSiteId: Int,
                                  @Column var remoteSiteId: Long,
                                  @Column var activityID: String,
                                  @Column var summary: String,
                                  @Column var text: String,
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
                                  @Column var role: String? = null) : Identifiable {
        constructor() : this(-1, 0, 0, "", "", "", published = 0)

        override fun setId(id: Int) {
            mId = id
        }

        override fun getId() = mId

        fun build(): ActivityLogModel {
            var actor: ActivityLogModel.ActivityActor? = null
            if (actorType != null || displayName != null || wpcomUserID != null || avatarURL != null || role != null) {
                actor = ActivityLogModel.ActivityActor(displayName, type, wpcomUserID, avatarURL, role)
            }
            return ActivityLogModel(activityID,
                    summary,
                    text,
                    name,
                    type,
                    gridicon,
                    status,
                    rewindable,
                    rewindID,
                    Date(published),
                    discarded,
                    actor)
        }
    }

    @Table(name = "RewindStatus")
    data class RewindStatusBuilder(@PrimaryKey
                                   @Column private var mId: Int = -1,
                                   @Column var localSiteId: Int,
                                   @Column var remoteSiteId: Long,
                                   @Column var rewindState: String? = null,
                                   @Column var reason: String? = null,
                                   @Column var restoreId: String? = null,
                                   @Column var restoreState: String? = null,
                                   @Column var restoreProgress: Int? = null,
                                   @Column var restoreMessage: String? = null,
                                   @Column var restoreErrorCode: String? = null,
                                   @Column var restoreFailureReason: String? = null) : Identifiable {
        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        constructor() : this(-1, 0, 0)

        fun build(): RewindStatusModel {
            val restoreStatus = RewindStatusModel.RestoreStatus.build(restoreId,
                    restoreState,
                    restoreProgress,
                    restoreMessage,
                    restoreErrorCode,
                    restoreFailureReason)
            return RewindStatusModel(rewindState?.let { RewindStatusModel.State.fromValue(it) }
                    ?: RewindStatusModel.State.UNKNOWN, reason, restoreStatus)
        }
    }
}
