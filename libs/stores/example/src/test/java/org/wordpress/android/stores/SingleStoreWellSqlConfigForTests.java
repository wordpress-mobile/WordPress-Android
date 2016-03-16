package org.wordpress.android.stores;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.TableClass;

import org.wordpress.android.stores.persistence.WellSqlConfig;

public class SingleStoreWellSqlConfigForTests extends WellSqlConfig {
    private Class<? extends Identifiable> mStoreClass;

    public SingleStoreWellSqlConfigForTests(Context context, Class<? extends Identifiable> token) {
        super(context);
        mStoreClass = token;
    }

    @Override
    public String getDbName() {
        return "test-stores";
    }

    @Override
    public void onCreate(SQLiteDatabase db, WellTableManager helper) {
        helper.createTable(mStoreClass);
    }

    /**
     * Drop and create all tables
     */
    public void reset() {
        SQLiteDatabase db = WellSql.giveMeWritableDb();
        TableClass table = getTable(mStoreClass);
        db.execSQL("DROP TABLE " + table.getTableName());
        db.execSQL(table.createStatement());
    }
}
