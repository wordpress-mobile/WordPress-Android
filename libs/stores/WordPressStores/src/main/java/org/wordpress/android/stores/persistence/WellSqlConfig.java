package org.wordpress.android.stores.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yarolegovich.wellsql.DefaultWellConfig;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.TableClass;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

import org.wordpress.android.stores.model.AccountModel;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.network.HTTPAuthModel;

import java.util.Map;

public class WellSqlConfig extends DefaultWellConfig {
    public WellSqlConfig(Context context) {
        super(context);
    }

    private static Class[] TABLES = {
            AccountModel.class,
            SiteModel.class,
            HTTPAuthModel.class
    };

    @Override
    public int getDbVersion() {
        return 1;
    }

    @Override
    public String getDbName() {
        return "wp-stores";
    }

    @Override
    public void onCreate(SQLiteDatabase db, WellTableManager helper) {
        for (Class table : TABLES) {
            helper.createTable(table);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, WellTableManager helper, int newVersion, int oldVersion) {
        // drop+create or migrate
    }

    @Override
    protected Map<Class<?>, SQLiteMapper<?>> registerMappers() {
        return super.registerMappers();
    }

    /**
     * Drop and create all tables
     */
    public void reset() {
        SQLiteDatabase db = WellSql.giveMeWritableDb();
        for (Class clazz : TABLES) {
            TableClass table = getTable(clazz);
            db.execSQL("DROP TABLE " + table.getTableName());
            db.execSQL(table.createStatement());
        }
    }
}
