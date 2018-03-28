package org.wordpress.android.fluxc.model.activity

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import java.util.Date

data class ActivityLogModel(val activityID: String,
                            val summary: String,
                            val text: String,
                            val name: String?,
                            val type: String?,
                            val gridicon: String?,
                            val status: String?,
                            val rewindable: Boolean?,
                            val rewindID: String?,
                            val published: Date,
                            val isDiscarded: Boolean?,
                            val actor: ActivityActor? = null) {
    enum class Status(value: String) {
        ERROR("error"), SUCCESS("success"), WARNING("warning");
    }

    data class ActivityActor(val displayName: String?,
                             val type: String?,
                             val wpcomUserID: Long?,
                             val avatarURL: String?,
                             val role: String?) {
        val isJetpack = { type == "Application" && displayName == "Jetpack" }
    }

    @Table(name = "ActivityLogModel")
    data class Builder(@PrimaryKey
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
                       @Column var published: Date = Date(),
                       @Column var discarded: Boolean? = null,
                       @Column var displayName: String? = null,
                       @Column var actorType: String? = null,
                       @Column var wpcomUserID: Long? = null,
                       @Column var avatarURL: String? = null,
                       @Column var role: String? = null) : Identifiable {
        constructor() : this(-1, 0, 0, "", "", "")

        override fun setId(id: Int) {
            mId = id
        }

        override fun getId() = mId

        fun build(): ActivityLogModel {
            var actor: ActivityActor? = null
            if (actorType != null || displayName != null || wpcomUserID != null || avatarURL != null || role != null) {
                actor = ActivityActor(displayName, type, wpcomUserID, avatarURL, role)
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
                    published,
                    discarded,
                    actor)
        }
    }
}
