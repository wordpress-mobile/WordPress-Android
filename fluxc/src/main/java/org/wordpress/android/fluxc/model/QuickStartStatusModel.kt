package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
class QuickStartStatusModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var siteId: Long = 0
    @Column var isCompleted: Boolean = false
        @JvmName("setIsCompleted")
        set
    @Column var isNotificationReceived: Boolean = false
        @JvmName("setIsNotificationReceived")
        set

    override fun getId(): Int {
        return this.id
    }

    override fun setId(id: Int) {
        this.id = id
    }
}
