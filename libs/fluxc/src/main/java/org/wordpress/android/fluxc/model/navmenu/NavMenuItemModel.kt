package org.wordpress.android.fluxc.model.navmenu

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

/**
 * Represents an item within a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menu-items endpoint.
 */
@Table
data class NavMenuItemModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId: Int = 0
    @Column var remoteItemId: Long = 0
    @Column var menuId: Long = 0
    @Column var title: String = ""
    @Column var url: String = ""
    // Type of menu item: "custom", "post_type", "taxonomy", "post_type_archive"
    @Column var type: String = ""
    // Specific object type: "post", "page", "category", "tag", or custom taxonomy/post type
    @Column var objectType: String = ""
    // ID of the linked object (post, page, category, etc.)
    @Column var objectId: Long = 0
    // Parent menu item ID for hierarchical menus
    @Column var parentId: Long = 0
    // Order position within the menu
    @Column var menuOrder: Int = 0
    // Link target: "_blank", "_self", etc.
    @Column var target: String = ""
    // CSS classes (JSON array stored as string)
    @Column var classes: String = ""
    // Item description
    @Column var description: String = ""
    // Attribute title (tooltip)
    @Column var attrTitle: String = ""

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NavMenuItemModel) return false

        return id == other.id &&
            localSiteId == other.localSiteId &&
            remoteItemId == other.remoteItemId &&
            menuId == other.menuId &&
            title == other.title &&
            url == other.url &&
            type == other.type &&
            objectType == other.objectType &&
            objectId == other.objectId &&
            parentId == other.parentId &&
            menuOrder == other.menuOrder &&
            target == other.target &&
            classes == other.classes &&
            description == other.description &&
            attrTitle == other.attrTitle
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + localSiteId
        result = 31 * result + remoteItemId.hashCode()
        result = 31 * result + menuId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + objectType.hashCode()
        result = 31 * result + objectId.hashCode()
        result = 31 * result + parentId.hashCode()
        result = 31 * result + menuOrder
        result = 31 * result + target.hashCode()
        result = 31 * result + classes.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + attrTitle.hashCode()
        return result
    }

    companion object {
        const val TYPE_CUSTOM = "custom"
        const val TYPE_POST_TYPE = "post_type"
        const val TYPE_TAXONOMY = "taxonomy"
        const val TYPE_POST_TYPE_ARCHIVE = "post_type_archive"

        const val OBJECT_TYPE_POST = "post"
        const val OBJECT_TYPE_PAGE = "page"
        const val OBJECT_TYPE_CATEGORY = "category"
        const val OBJECT_TYPE_TAG = "post_tag"
    }
}
