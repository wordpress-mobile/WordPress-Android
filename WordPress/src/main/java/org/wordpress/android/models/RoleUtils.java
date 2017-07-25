package org.wordpress.android.models;

import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.util.StringUtils;

import java.util.List;

public class RoleUtils {
    public static String getDisplayName(String userRole, List<RoleModel> siteUserRoles) {
        for (RoleModel roleModel : siteUserRoles) {
            if (roleModel.getName().equalsIgnoreCase(userRole)) {
                return roleModel.getDisplayName();
            }
        }
        return StringUtils.capitalize(userRole);
    }
}
