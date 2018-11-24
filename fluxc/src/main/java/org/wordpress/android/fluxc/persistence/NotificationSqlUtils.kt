package org.wordpress.android.fluxc.persistence

import com.google.gson.Gson
import com.wellsql.generated.NotificationModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.NotificationModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import org.wordpress.android.fluxc.tools.FormattableMeta
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSqlUtils @Inject constructor(private val formattableContentMapper: FormattableContentMapper) {
    companion object {
        private val gson by lazy { Gson() }
    }

    fun insertOrUpdateNotifications(siteModel: SiteModel, noteModels: List<NotificationModel>): Int {
        val noteIds = noteModels.map { it.noteId }
        val notesToUpdate = WellSql.select(NotificationModelBuilder::class.java).where().beginGroup()
                .isIn(NotificationModelTable.ID, noteIds)
                .equals(NotificationModelTable.LOCAL_SITE_ID, siteModel.id)
                .endGroup()
                .endWhere()
                .asModel
                .map { it.id }
        val (existing, new) = noteModels
                .map { it.toBuilder(siteModel) }
                .partition { notesToUpdate.contains(it.id) }
        val insertQuery = WellSql.insert(new)
        val updateQueries = existing.map {
            WellSql.update(NotificationModelBuilder::class.java)
                    .where()
                    .equals(NotificationModelTable.ID, it.id)
                    .equals(NotificationModelTable.LOCAL_SITE_ID, it.localSiteId)
                    .endWhere()
                    .put(it, UpdateAllExceptId<NotificationModelBuilder>(NotificationModelBuilder::class.java))
        }

        // Execute queries and return total inserted + updated
        insertQuery.execute()
        return updateQueries.asSequence().map { it.execute() }.sum() + new.count()
    }

    fun getNotificationsForSite(
        site: SiteModel,
        @SelectQuery.Order order: Int,
        filterByType: List<String>? = null,
        filterBySubtype: List<String>? = null
    ): List<NotificationModel> {
        val conditionClauseBuilder = WellSql.select(NotificationModelBuilder::class.java)
                .where()
                .equals(NotificationModelTable.LOCAL_SITE_ID, site.id)

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

    fun deleteNotificationsForSite(site: SiteModel): Int {
        return WellSql.delete(NotificationModelBuilder::class.java)
                .where().beginGroup()
                .equals(NotificationModelTable.LOCAL_SITE_ID, site.id)
                .endGroup()
                .endWhere()
                .execute()
    }

    private fun NotificationModel.toBuilder(site: SiteModel): NotificationModelBuilder {
        return NotificationModelBuilder(
                mId = this.noteId,
                remoteNoteId = this.remoteNoteId,
                localSiteId = site.id,
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
        @Column var localSiteId: Int,
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
        constructor() : this(-1, 0L, 0, 0L, NotificationModel.Kind.STORE_ORDER.toString())
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
                    localSiteId,
                    noteHash,
                    NotificationModel.Kind.fromString(type),
                    subkind,
                    read,
                    icon,
                    noticon,
                    timestamp,
                    url,
                    title,
                    body,
                    subject,
                    meta)
        }
    }
}
