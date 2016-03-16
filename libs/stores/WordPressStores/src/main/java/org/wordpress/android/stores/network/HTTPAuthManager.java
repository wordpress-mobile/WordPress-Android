package org.wordpress.android.stores.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.wellsql.generated.HTTPAuthModelTable;
import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.persistence.HTTPAuthSqlUtils;

import java.net.URI;
import java.util.List;

public class HTTPAuthManager {
    public HTTPAuthManager() {}

    private String normalizeURL(String url) {
        try {
            URI uri = URI.create(url);
            return uri.normalize().toString();
        } catch (IllegalArgumentException e) {
            return url;
        }
    }

    /**
     * Get an HTTPAuthModel containing username and password for the url parameter
     * TODO: Use an in memory model (or caching) - because this SQL query is executed every time a request is sent
     *
     * @param url to test
     * @return null if url is not matching any known HTTP auth credentials
     */
    public @Nullable HTTPAuthModel getHTTPAuthModel(String url) {
        List<HTTPAuthModel> authModels = WellSql.selectUnique(HTTPAuthModel.class).where()
                .equals(HTTPAuthModelTable.ROOT_URL, normalizeURL(url))
                .endWhere().getAsModel();
        if (authModels.isEmpty()) {
            return null;
        }
        return authModels.get(0);
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
}
