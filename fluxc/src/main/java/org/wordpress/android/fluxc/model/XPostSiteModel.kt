package org.wordpress.android.fluxc.model

import com.google.gson.annotations.SerializedName
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table

@Table(name = "XPostSites")
@RawConstraints("UNIQUE (BLOG_ID) ON CONFLICT REPLACE")
data class XPostSiteModel(
    @PrimaryKey @Column private var id: Int = 0,
    @SerializedName("blog_id") @Column var blogId: Int = 0,
    @Column var title: String = "",
    @SerializedName("siteurl") @Column var siteUrl: String = "",
    @Column var subdomain: String = "",
    @Column var blavatar: String = ""
) : Identifiable {
    override fun setId(id: Int) {
        this.id = id
    }

    override fun getId(): Int = id
}
