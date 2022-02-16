package org.wordpress.android.fluxc.persistence

import android.annotation.SuppressLint
import com.wellsql.generated.NotificationModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.SelectQuery.ORDER_DESCENDING
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Kind
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import org.wordpress.android.fluxc.tools.FormattableMeta
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSqlUtils @Inject constructor(private val formattableContentMapper: FormattableContentMapper) {
    private val dataUpdatesTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun insertOrUpdateNotification(notification: NotificationModel): Int {
        val notificationResult = WellSql.select(NotificationModelBuilder::class.java)
                .where().beginGroup()
                .equals(NotificationModelTable.ID, notification.noteId)
                .or()
                .beginGroup()
                .equals(NotificationModelTable.REMOTE_SITE_ID, notification.remoteSiteId)
                .equals(NotificationModelTable.REMOTE_NOTE_ID, notification.remoteNoteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel

        return if (notificationResult.isEmpty()) {
            // insert
            WellSql.insert(notification.toBuilder()).asSingleTransaction(true).execute()
            dataUpdatesTrigger.tryEmit(Unit)
            1
        } else {
            // update
            val oldId = notificationResult[0].id
            WellSql.update(NotificationModelBuilder::class.java).whereId(oldId).put(
                    notification.toBuilder(),
                    UpdateAllExceptId<NotificationModelBuilder>(NotificationModelBuilder::class.java)
            ).execute().also(::triggerUpdateIfNeeded)
        }
    }

    /**
     * @return The total records in the notification table.
     */
    fun getNotificationsCount() = WellSql.select(NotificationModelBuilder::class.java).count()

    @SuppressLint("WrongConstant")
    fun getNotifications(
        @SelectQuery.Order order: Int = ORDER_DESCENDING,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> {
        val conditionClauseBuilder = WellSql.select(NotificationModelBuilder::class.java)
                .where()

        if (filterByType != null || filterBySubtype != null) {
            conditionClauseBuilder.beginGroup()

            filterByType?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.TYPE, it)
            }

            if (filterByType != null && filterBySubtype != null) {
                conditionClauseBuilder.or()
            }

            filterBySubtype?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.SUBTYPE, it)
            }

            conditionClauseBuilder.endGroup()
        }

        return conditionClauseBuilder.endWhere()
                .orderBy(NotificationModelTable.TIMESTAMP, order)
                .asModel
                .map { it.build(formattableContentMapper) }
    }

    @SuppressLint("WrongConstant")
    fun getNotificationsForSite(
        site: SiteModel,
        @SelectQuery.Order order: Int = ORDER_DESCENDING,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> {
        val conditionClauseBuilder = WellSql.select(NotificationModelBuilder::class.java)
                .where()
                .equals(NotificationModelTable.REMOTE_SITE_ID, site.siteId)

        if (filterByType != null || filterBySubtype != null) {
            conditionClauseBuilder.beginGroup()

            filterByType?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.TYPE, it)
            }

            if (filterByType != null && filterBySubtype != null) {
                conditionClauseBuilder.or()
            }

            filterBySubtype?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.SUBTYPE, it)
            }

            conditionClauseBuilder.endGroup()
        }

        return conditionClauseBuilder.endWhere()
                .orderBy(NotificationModelTable.TIMESTAMP, order)
                .asModel
                .map { it.build(formattableContentMapper) }
    }

    fun observeNotificationsForSite(
        site: SiteModel,
        @SelectQuery.Order order: Int = ORDER_DESCENDING,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): Flow<List<NotificationModel>> {
        return dataUpdatesTrigger
                .onStart { emit(Unit) }
                .mapLatest {
                    getNotificationsForSite(site, order, filterByType, filterBySubtype)
                }
                .flowOn(Dispatchers.IO)
    }

    fun hasUnreadNotificationsForSite(
        site: SiteModel,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): Boolean {
        val conditionClauseBuilder = WellSql.select(NotificationModelBuilder::class.java)
                .where()
                .equals(NotificationModelTable.REMOTE_SITE_ID, site.siteId)
                .equals(NotificationModelTable.READ, 0)

        if (filterByType != null || filterBySubtype != null) {
            conditionClauseBuilder.beginGroup()

            filterByType?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.TYPE, it)
            }

            if (filterByType != null && filterBySubtype != null) {
                conditionClauseBuilder.or()
            }

            filterBySubtype?.let {
                conditionClauseBuilder.isIn(NotificationModelTable.SUBTYPE, it)
            }

            conditionClauseBuilder.endGroup()
        }

        return conditionClauseBuilder.endWhere().exists()
    }

    fun getNotificationByIdSet(idSet: NoteIdSet): NotificationModel? {
        val (id, remoteNoteId, remoteSiteId) = idSet
        return WellSql.select(NotificationModelBuilder::class.java)
                .where().beginGroup()
                .equals(NotificationModelTable.ID, id)
                .or()
                .beginGroup()
                .equals(NotificationModelTable.REMOTE_SITE_ID, remoteSiteId)
                .equals(NotificationModelTable.REMOTE_NOTE_ID, remoteNoteId)
                .endGroup()
                .endGroup().endWhere()
                .asModel
                .firstOrNull()?.build(formattableContentMapper)
    }

    fun getNotificationByRemoteId(remoteNoteId: Long): NotificationModel? {
        return WellSql.select(NotificationModelBuilder::class.java)
                .where()
                .equals(NotificationModelTable.REMOTE_NOTE_ID, remoteNoteId)
                .endWhere()
                .asModel
                .firstOrNull()?.build(formattableContentMapper)
    }

    fun deleteAllNotifications() = WellSql.delete(NotificationModelBuilder::class.java)
            .execute()
            .also(::triggerUpdateIfNeeded)

    fun deleteNotificationByRemoteId(remoteNoteId: Long): Int {
        return WellSql.delete(NotificationModelBuilder::class.java)
                .where().beginGroup()
                .equals(NotificationModelTable.REMOTE_NOTE_ID, remoteNoteId)
                .endGroup().endWhere()
                .execute()
                .also(::triggerUpdateIfNeeded)
    }

    private fun triggerUpdateIfNeeded(affectedRows: Int) {
        if (affectedRows != 0) dataUpdatesTrigger.tryEmit(Unit)
    }

    private fun NotificationModel.toBuilder(): NotificationModelBuilder {
        return NotificationModelBuilder(
                mId = this.noteId,
                remoteNoteId = this.remoteNoteId,
                remoteSiteId = this.remoteSiteId,
                noteHash = this.noteHash,
                type = this.type.toString(),
                subtype = this.subtype.toString(),
                read = this.read,
                icon = this.icon,
                noticon = this.noticon,
                timestamp = this.timestamp,
                url = this.url,
                title = this.title,
                formattableBody = this.body?.let { formattableContentMapper.mapFormattableContentListToJson(it) },
                formattableSubject = this.subject?.let { formattableContentMapper.mapFormattableContentListToJson(it) },
                formattableMeta = this.meta?.let { formattableContentMapper.mapFormattableMetaToJson(it) }
        )
    }

    @Table(name = "NotificationModel")
    data class NotificationModelBuilder(
        @PrimaryKey @Column private var mId: Int = -1,
        @Column var remoteNoteId: Long,
        @Column var remoteSiteId: Long,
        @Column var noteHash: Long,
        @Column var type: String,
        @Column var subtype: String? = null,
        @Column var read: Boolean = false,
        @Column var icon: String? = null,
        @Column var noticon: String? = null,
        @Column var timestamp: String? = null,
        @Column var url: String? = null,
        @Column var title: String? = null,
        @Column var formattableBody: String? = null,
        @Column var formattableSubject: String? = null,
        @Column var formattableMeta: String? = null
    ) : Identifiable {
        constructor() : this(-1, 0L, -1, 0, NotificationModel.Kind.STORE_ORDER.toString())
        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = this.mId

        fun build(formattableContentMapper: FormattableContentMapper): NotificationModel {
            val subkind: NotificationModel.Subkind? = subtype?.let { NotificationModel.Subkind.fromString(it) }

            val body: List<FormattableContent>? = formattableBody?.let {
                formattableContentMapper.mapToFormattableContentList(it)
            }
            val subject: List<FormattableContent>? = formattableSubject?.let {
                formattableContentMapper.mapToFormattableContentList(it)
            }
            val meta: FormattableMeta? = formattableMeta?.let {
                formattableContentMapper.mapToFormattableMeta(it)
            }
            return NotificationModel(
                    mId,
                    remoteNoteId,
                    remoteSiteId,
                    noteHash,
                    Kind.fromString(type),
                    subkind,
                    read,
                    icon,
                    noticon,
                    timestamp,
                    url,
                    title,
                    body,
                    subject,
                    meta
            )
        }
    }
}
