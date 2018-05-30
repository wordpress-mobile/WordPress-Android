package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import java.io.Serializable

@Table
class QuickStartModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable, Serializable {
    @Column var siteId: Long = 0
    @Column var taskName: String? = null
    @Column var isDone: Boolean = false
        @JvmName("setIsDone")
        set
    @Column var isShown: Boolean = false
        @JvmName("setIsShown")
        set

    override fun getId(): Int {
        return this.id
    }

    override fun setId(id: Int) {
        this.id = id
    }
}
