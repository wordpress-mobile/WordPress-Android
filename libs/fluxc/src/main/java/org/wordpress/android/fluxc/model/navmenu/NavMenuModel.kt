package org.wordpress.android.fluxc.model.navmenu

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

/**
 * Represents a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menus endpoint.
 */
@Table
class NavMenuModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId: Int = 0
    @Column var remoteMenuId: Long = 0
    @Column var name: String = ""
    @Column var slug: String = ""
    @Column var description: String = ""
    // JSON array of location slugs where this menu is assigned
    @Column var locations: String = ""
    // Whether to automatically add new top-level pages to this menu
    @Column var autoAdd: Boolean = false

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
