package org.wordpress.android.fluxc.model.plugin;

import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.annotation.Column;
import com.yarolegovich.wellsql.core.annotation.PrimaryKey;
import com.yarolegovich.wellsql.core.annotation.Table;

@Table
public class PluginDirectoryModel implements Identifiable {
    @PrimaryKey @Column private int mId;
    @Column private String mSlug;
    @Column private String mDirectoryType;
    @Column private int mPage;

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public void setId(int id) {
        mId = id;
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

    public String getDirectoryType() {
        return mDirectoryType;
    }

    public void setDirectoryType(String directoryType) {
        mDirectoryType = directoryType;
    }

    public int getPage() {
        return mPage;
    }

    public void setPage(int page) {
        mPage = page;
    }
}
