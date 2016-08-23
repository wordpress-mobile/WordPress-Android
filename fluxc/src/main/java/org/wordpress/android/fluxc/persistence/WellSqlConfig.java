package org.wordpress.android.fluxc.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yarolegovich.wellsql.DefaultWellConfig;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.TableClass;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.HTTPAuthModel;

import java.util.Map;

public class WellSqlConfig extends DefaultWellConfig {
    public WellSqlConfig(Context context) {
        super(context);
    }

    private static Class[] TABLES = {
            AccountModel.class,
            SiteModel.class,
            MediaModel.class,
            PostFormatModel.class,
            HTTPAuthModel.class
    };

    @Override
    public int getDbVersion() {
        return 1;
    }

    @Override
    public String getDbName() {
        return "wp-fluxc";
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
            db.execSQL("DROP TABLE IF EXISTS " + table.getTableName());
            db.execSQL(table.createStatement());
        }
    }
}
