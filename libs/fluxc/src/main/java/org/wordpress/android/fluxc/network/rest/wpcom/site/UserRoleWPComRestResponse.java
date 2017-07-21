package org.wordpress.android.fluxc.network.rest.wpcom.site;

import java.util.List;

class UserRoleWPComRestResponse {
    class UserRolesResponse {
        List<UserRoleWPComRestResponse> roles;
    }
    String name;
    String display_name;
}
