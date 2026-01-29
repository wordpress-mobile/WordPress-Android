package org.wordpress.android.fluxc.model.navmenu

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

/**
 * Represents a theme menu location where menus can be assigned.
 * Maps to the WP REST API /wp/v2/menu-locations endpoint.
 */
@Table
class NavMenuLocationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId: Int = 0
    // The location slug (e.g., "primary", "footer", "social")
    @Column var name: String = ""
    // Human-readable description of the location
    @Column var description: String = ""
    // The ID of the menu assigned to this location (0 if no menu assigned)
    @Column var menuId: Long = 0

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }
}
