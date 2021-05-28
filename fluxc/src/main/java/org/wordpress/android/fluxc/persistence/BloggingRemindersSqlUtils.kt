package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.ScanStateTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BloggingRemindersSqlUtils @Inject constructor() {
    fun replaceBloggingReminder(bloggingReminders: BloggingReminders) {
        WellSql.delete(BloggingReminders::class.java)
                .where()
                .equals(ScanStateTable.LOCAL_SITE_ID, bloggingReminders.localSiteId)
                .endWhere()
                .execute()
        WellSql.insert(bloggingReminders).execute()
    }

    fun getBloggingReminders(): List<BloggingReminders> {
        return WellSql.select(BloggingReminders::class.java).asModel
    }

    @Table(name = "BloggingReminders")
    data class BloggingReminders(
        @PrimaryKey
        @Column private var id: Int = -1,
        @Column var localSiteId: Int,
        @Column var monday: Boolean = false,
        @Column var tuesday: Boolean = false,
        @Column var wednesday: Boolean = false,
        @Column var thursday: Boolean = false,
        @Column var friday: Boolean = false,
        @Column var saturday: Boolean = false,
        @Column var sunday: Boolean = false
    ) : Identifiable {
        constructor() : this(-1, 0, false, false, false, false, false, false, false)

        override fun setId(id: Int) {
            this.id = id
        }

        override fun getId() = id
    }
}
