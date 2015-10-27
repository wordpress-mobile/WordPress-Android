package org.wordpress.android.stores.persistence;

import android.content.ContentValues;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.mapper.InsertMapper;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

import org.wordpress.android.util.AppLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class UpdateAllExceptId<T> implements InsertMapper<T> {
    @Override
    public ContentValues toCv(T item) {
        try {
            // Reflection equivalent for: SQLiteMapper<T> mapper = WellSql.mapperFor((Class<T>) item.getClass());
            // This will be useless once the new wellsql version (1.0.6) is deployed
            Method mapperFor = WellSql.class.getDeclaredMethod("mapperFor", Class.class);
            mapperFor.setAccessible(true); // if security settings allow this
            SQLiteMapper<T> mapper = (SQLiteMapper<T>) mapperFor.invoke(null, (Class<T>) item.getClass());
            ContentValues cv = mapper.toCv(item);
            cv.remove("_id");
            return cv;
        } catch (IllegalAccessException e) {
            AppLog.e(AppLog.T.API, "can't invoke WellSql.mapperFor", e);
            return null;
        } catch (InvocationTargetException e) {
            AppLog.e(AppLog.T.API, "can't invoke WellSql.mapperFor", e);
            return null;
        } catch (NoSuchMethodException e) {
            AppLog.e(AppLog.T.API, "can't invoke WellSql.mapperFor", e);
            return null;
        }
    }
}
