package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

class UpdateAllExceptId<T> implements InsertMapper<T> {
    private final SQLiteMapper<T> mMapper;
    private final String[] mAdditionalColumnsToSkip;

    UpdateAllExceptId(Class<T> clazz) {
        this(clazz, new String[0]);
    }

    /**
     * @param additionalColumnsToSkip columns (beyond the primary key) that should be left untouched by the
     *                                resulting UPDATE. Use this when a column has a dedicated writer and must
     *                                not be overwritten by full-row updates built from partial in-memory models.
     */
    UpdateAllExceptId(Class<T> clazz, String... additionalColumnsToSkip) {
        mMapper = WellSql.mapperFor(clazz);
        mAdditionalColumnsToSkip = additionalColumnsToSkip;
    }

    @Override
    public ContentValues toCv(T item) {
        ContentValues cv = mMapper.toCv(item);
        cv.remove("_id");
        for (String column : mAdditionalColumnsToSkip) {
            cv.remove(column);
        }
        return cv;
    }
}
