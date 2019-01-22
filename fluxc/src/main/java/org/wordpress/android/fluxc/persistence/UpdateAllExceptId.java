package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

class UpdateAllExceptId<T> implements InsertMapper<T> {
    private final SQLiteMapper<T> mMapper;

    UpdateAllExceptId(Class<T> clazz) {
        mMapper = WellSql.mapperFor(clazz);
    }

    @Override
    public ContentValues toCv(T item) {
        ContentValues cv = mMapper.toCv(item);
        cv.remove("_id");
        return cv;
    }
}
