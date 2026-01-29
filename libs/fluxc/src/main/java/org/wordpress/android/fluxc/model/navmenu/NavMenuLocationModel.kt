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
data class NavMenuLocationModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavMenuLocationModel) return false

        return id == other.id &&
            localSiteId == other.localSiteId &&
            name == other.name &&
            description == other.description &&
            menuId == other.menuId
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + localSiteId
        result = 31 * result + name.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + menuId.hashCode()
        return result
    }
}
