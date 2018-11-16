package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.NoteModelTable
import com.yarolegovich.wellsql.SelectQuery
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.NoteModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableContentMapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteSqlUtils @Inject constructor(private val formattableContentMapper: FormattableContentMapper) {
    /**
     * TODO comments
     */
    fun insertOrUpdateNotes(siteModel: SiteModel, noteModels: List<NoteModel>): Int {
        val noteIds = noteModels.map { it.noteId }
        val notesToUpdate = WellSql.select(NoteModelBuilder::class.java).where().beginGroup()
                .isIn(NoteModelTable.ID, noteIds)
                .equals(NoteModelTable.LOCAL_SITE_ID, siteModel.id)
                .endGroup()
                .endWhere()
                .asModel
                .map { it.id }
        val (existing, new) = noteModels
                .map { it.toBuilder(siteModel) }
                .partition { notesToUpdate.contains(it.id) }
        val insertQuery = WellSql.insert(new)
        val updateQueries = existing.map {
            WellSql.update(NoteModelBuilder::class.java)
                    .where()
                    .equals(NoteModelTable.ID, it.id)
                    .equals(NoteModelTable.LOCAL_SITE_ID, it.localSiteId)
                    .endWhere()
                    .put(it, UpdateAllExceptId<NoteModelBuilder>(NoteModelBuilder::class.java))
        }

        // Execute queries and return total inserted + updated
        insertQuery.execute()
        return updateQueries.asSequence().map { it.execute() }.sum() + new.count()
    }

    /**
     * TODO comments
     */
    fun getNotesForSite(site: SiteModel, @SelectQuery.Order order: Int): List<NoteModel> {
        return WellSql.select(NoteModelBuilder::class.java)
                .where()
                .equals(NoteModelTable.LOCAL_SITE_ID, site.id)
                .endWhere()
                .orderBy(NoteModelTable.TIMESTAMP, order)
                .asModel
                .map { it.build(formattableContentMapper) }
    }

    private fun NoteModel.toBuilder(site: SiteModel): NoteModelBuilder {
        return NoteModelBuilder(
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
                formattableBody = this.body?.let { formattableContentMapper.mapFormattableContentToJson(it) },
                formattableSubject = this.subject?.let { formattableContentMapper.mapFormattableContentToJson(it) },
                formattableMeta = this.meta?.let { formattableContentMapper.mapFormattableContentToJson(it) }
        )
    }

    @Table(name = "NoteModel")
    data class NoteModelBuilder(
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
        constructor() : this(-1, 0L, 0, 0L, NoteModel.Kind.STORE_ORDER.toString())
        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = this.mId

        fun build(formattableContentMapper: FormattableContentMapper): NoteModel {
            val subkind: NoteModel.Subkind? = subtype?.let { NoteModel.Subkind.valueOf(it) }
            val body: FormattableContent? = formattableBody?.let {
                formattableContentMapper.mapToFormattableContent(it)
            }
            val subject: FormattableContent? = formattableSubject?.let {
                formattableContentMapper.mapToFormattableContent(it)
            }
            val meta: FormattableContent? = formattableMeta?.let {
                formattableContentMapper.mapToFormattableContent(it)
            }
            return NoteModel(
                    mId,
                    remoteNoteId,
                    localSiteId,
                    noteHash,
                    NoteModel.Kind.valueOf(type),
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
