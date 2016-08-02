package org.wordpress.android.fluxc.persistence;

import android.content.ContentValues;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

public class UpdateAllExceptId<T> implements InsertMapper<T> {
    @Override
    public ContentValues toCv(T item) {
        SQLiteMapper<T> mapper = WellSql.mapperFor((Class<T>) item.getClass());
        ContentValues cv = mapper.toCv(item);
        cv.remove("_id");
        return cv;
    }
}
