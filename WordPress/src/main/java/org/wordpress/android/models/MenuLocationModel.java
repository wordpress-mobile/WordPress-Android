package org.wordpress.android.models;

import org.wordpress.android.util.StringUtils;

/**
 * Menu Locations are defined by a site's Theme and may contain, at most, a single Menu.
 *
 * Each Menu Location within a Theme is defined by a unique string ({@code name}). However, since
 * users may have access to multiple sites that may have unique Menu Locations with the same name,
 * each Menu Location must be keyed by a combination of {@code siteId} and {@code name}. This
 * guarantees a unique primary key for each Menu Location because a site can only have one Theme.
 */

public class MenuLocationModel implements NameInterface {
    public static final String LOCATION_DEFAULT = "default";
    public static final String LOCATION_EMPTY   = "empty";

    //
    // Primary key attributes (cannot be null)
    //
    /** Remote ID of the site this Menu Location is associated with */
    public long siteId;
    /** Name of this Menu Location */
    public String name;

    //
    // Optional attributes (may be null)
    //
    /** Descriptive text for this Menu Location */
    public String details;
    /** Visual state the Menu should be displayed */
    public String defaultState;

    //
    // Derived attributes
    //
    /** ID of the Menu contained in this Menu Location */
    public long menuId;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuLocationModel)) return false;

        MenuLocationModel otherLocation = (MenuLocationModel) other;
        return siteId == otherLocation.siteId &&
                StringUtils.equals(name, otherLocation.name) &&
                StringUtils.equals(details, otherLocation.details) &&
                StringUtils.equals(defaultState, otherLocation.defaultState) &&
                menuId == otherLocation.menuId;
    }

    @Override
    public String getName() {
        return details;
    }
}
