package org.wordpress.android.fluxc.network.rest.wpcom.site;

import java.util.List;

public class UserRoleWPComRestResponse {
    public class UserRolesResponse {
        public List<UserRoleWPComRestResponse> roles;
    }
    public String name;
    public String display_name;
}
