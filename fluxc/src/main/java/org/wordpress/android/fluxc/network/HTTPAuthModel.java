package org.wordpress.android.fluxc.network;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.RawConstraints;
import com.yarolegovich.wellsql.core.annotation.Table;

@Table
@RawConstraints({"UNIQUE (ROOT_URL)"})
public class HTTPAuthModel implements Identifiable {
    @PrimaryKey
    @Column private int mId;
    @Column private String mRootUrl;
    @Column private String mRealm;
    @Column private String mUsername;
    @Column private String mPassword;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public HTTPAuthModel() {
    }

    public String getRealm() {
        return mRealm;
    }

    public void setRealm(String realm) {
        mRealm = realm;
    }

    public String getUsername() {
        return mUsername;
    }

    public void setUsername(String username) {
        mUsername = username;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public String getRootUrl() {
        return mRootUrl;
    }

    public void setRootUrl(String rootUrl) {
        mRootUrl = rootUrl;
    }
}
