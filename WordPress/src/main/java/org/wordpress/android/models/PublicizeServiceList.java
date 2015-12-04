package org.wordpress.android.models;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class PublicizeServiceList extends ArrayList<PublicizeService> {

    private int indexOfService(PublicizeService service) {
        if (service == null) return -1;

        for (int i = 0; i < this.size(); i++) {
            if (service.getId().equalsIgnoreCase(this.get(i).getId())) {
                return i;
            }
        }

        return -1;
    }

    public boolean isSameAs(PublicizeServiceList otherList) {
        if (otherList == null || otherList.size() != this.size()) {
            return false;
        }

        for (PublicizeService otherService: otherList) {
            int i = this.indexOfService(otherService);
            if (i == -1) {
                return false;
            } else if (!otherService.isSameAs(this.get(i))) {
                return false;
            }
        }

        return true;
    }

    /*
     * passed JSON is the response from /meta/external-services?type=publicize
         "services": {
                "facebook": {
                    "ID": "facebook",
                    "label": "Facebook",
                    "type": "publicize",
                    "description": "Publish your posts to your Facebook timeline or page.",
                    "genericon": {
                        "class": "facebook-alt",
                        "unicode": "\\f203"
                    },
                    "icon": "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-fb-2x.png",
                    "connect_URL": "https://public-api.wordpress.com/connect/?action=request&kr_nonce=b2c86a0cdb&nonce=94557d1529&for=connect&service=facebook&kr_blog_nonce=5e399375f1&magic=keyring&blog=52451191",
                    "multiple_external_user_ID_support": true,
                    "jetpack_support": true,
                    "jetpack_module_required": "publicize"
                },
            ...
     */
    public static PublicizeServiceList fromJson(JSONObject json) {
        PublicizeServiceList serviceList = new PublicizeServiceList();
        if (json == null) return serviceList;

        JSONObject jsonServiceList = json.optJSONObject("services");
        if (jsonServiceList == null) return serviceList;

        Iterator<String> it = jsonServiceList.keys();
        while (it.hasNext()) {
            String serviceName = it.next();
            JSONObject jsonService = jsonServiceList.optJSONObject(serviceName);

            PublicizeService service = new PublicizeService();
            service.setId(jsonService.optString("ID"));
            service.setLabel(jsonService.optString("label"));
            service.setDescription(jsonService.optString("description"));
            service.setIconUrl(jsonService.optString("icon"));
            service.setConnectUrl(jsonService.optString("connect_URL"));

            service.setIsJetpackSupported(jsonService.optBoolean("jetpack_support"));
            service.setIsMultiExternalUserIdSupported(jsonService.optBoolean("multiple_external_user_ID_support"));

            JSONObject jsonGenericon = jsonService.optJSONObject("genericon");
            if (jsonGenericon != null) {
                service.setGenericon(jsonGenericon.optString("unicode"));
            }
                    serviceList.add(service);
        }

        return serviceList;
    }
}
