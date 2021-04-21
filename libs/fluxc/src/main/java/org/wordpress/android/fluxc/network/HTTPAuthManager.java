package org.wordpress.android.fluxc.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.fluxc.persistence.HTTPAuthSqlUtils;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

public class HTTPAuthManager {
    @Inject public HTTPAuthManager() {}

    /**
     * Get an HTTPAuthModel containing username and password for the url parameter
     * TODO: Use an in memory model (or caching) - because this SQL query is executed every time a request is sent
     *
     * @param url to test
     * @return null if url is not matching any known HTTP auth credentials
     */
    @Nullable
    public HTTPAuthModel getHTTPAuthModel(String url) {
        List<HTTPAuthModel> authModels = WellSql.select(HTTPAuthModel.class).getAsModel();
        if (authModels.isEmpty()) {
            return null;
        }
        for (HTTPAuthModel authModel : authModels) {
            if (url.startsWith(authModel.getRootUrl())) {
                return authModel;
            }

            // Also compare against the stored URL with the ending 'xmlrpc.php' (or other name) stripped
            String xmlrpcStripped = authModel.getRootUrl().replaceFirst("/[^/]*?.php$", "");
            if (url.startsWith(xmlrpcStripped)) {
                return authModel;
            }
        }
        return null;
    }

    public void addHTTPAuthCredentials(@NonNull String username, @NonNull String password,
                                       @NonNull String url, @Nullable String realm) {
        HTTPAuthModel httpAuthModel = new HTTPAuthModel();
        httpAuthModel.setUsername(username);
        httpAuthModel.setPassword(password);
        httpAuthModel.setRootUrl(normalizeURL(url));
        httpAuthModel.setRealm(realm);
        // Replace old username / password / realm - URL used as key
        HTTPAuthSqlUtils.insertOrUpdateModel(httpAuthModel);
    }

    private String normalizeURL(String url) {
        try {
            URI uri = URI.create(url);
            return uri.normalize().toString();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }
}
