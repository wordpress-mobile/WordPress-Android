package org.wordpress.android.fluxc.utils;

import java.util.Map;

public class NetworkUtils {
    public static String addParamsToUrl(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()){
            if (stringBuilder.length() == 0){
                stringBuilder.append('?');
            } else {
                stringBuilder.append('&');
            }
            stringBuilder.append(entry.getKey()).append('=').append(entry.getValue());
        }

        return url + stringBuilder.toString();
    }
}
