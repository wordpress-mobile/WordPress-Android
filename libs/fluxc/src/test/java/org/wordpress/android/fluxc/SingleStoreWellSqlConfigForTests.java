package org.wordpress.android.fluxc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.TableClass;

import org.wordpress.android.fluxc.persistence.WellSqlConfig;

import java.util.ArrayList;
import java.util.List;

public class SingleStoreWellSqlConfigForTests extends WellSqlConfig {
    private List<Class<? extends Identifiable>> mStoreClassList;

    public SingleStoreWellSqlConfigForTests(Context context, Class<? extends Identifiable> token) {
        super(context);
        mStoreClassList = new ArrayList<>();
        mStoreClassList.add(token);
    }

    public SingleStoreWellSqlConfigForTests(Context context, Class<? extends Identifiable> token,
                                            @AddOn String... addOns) {
        super(context, addOns);
        mStoreClassList = new ArrayList<>();
        mStoreClassList.add(token);
    }

    public SingleStoreWellSqlConfigForTests(Context context, List<Class<? extends Identifiable>> tokens,
                                            @AddOn String... addOns) {
        super(context, addOns);
        mStoreClassList = tokens;
    }

    @Override
    public String getDbName() {
        return "test-fluxc";
    }

    @Override
    public void onCreate(SQLiteDatabase db, WellTableManager helper) {
        for (Class<? extends Identifiable> clazz : mStoreClassList) {
            helper.createTable(clazz);
        }
    }

    /**
     * Drop and create all tables
     */
    @Override
    public void reset() {
        SQLiteDatabase db = WellSql.giveMeWritableDb();
        for (Class<? extends Identifiable> clazz : mStoreClassList) {
            TableClass table = getTable(clazz);
            db.execSQL("DROP TABLE " + table.getTableName());
            db.execSQL(table.createStatement());
        }
    }
}
