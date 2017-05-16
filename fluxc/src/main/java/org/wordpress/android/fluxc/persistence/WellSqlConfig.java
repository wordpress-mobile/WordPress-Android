package org.wordpress.android.fluxc.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.yarolegovich.wellsql.DefaultWellConfig;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.TableClass;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

import org.wordpress.android.fluxc.model.AccountModel;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TaxonomyModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.network.HTTPAuthModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.Map;

public class WellSqlConfig extends DefaultWellConfig {
    public WellSqlConfig(Context context) {
        super(context);
    }

    private static final Class[] TABLES = {
            AccountModel.class,
            SiteModel.class,
            MediaModel.class,
            PostFormatModel.class,
            PostModel.class,
            CommentModel.class,
            TaxonomyModel.class,
            TermModel.class,
            HTTPAuthModel.class
    };

    @Override
    public int getDbVersion() {
        return 9;
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

    @SuppressWarnings({"FallThrough"})
    @Override
    public void onUpgrade(SQLiteDatabase db, WellTableManager helper, int oldVersion, int newVersion) {
        AppLog.d(T.DB, "Upgrading database from version " + oldVersion + " to " + newVersion);

        db.beginTransaction();
        switch (oldVersion) {
            case 1:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add ICON_URL text;");
                oldVersion++;
            case 2:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add FRAME_NONCE text;");
                oldVersion++;
            case 3:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table AccountModel add EMAIL_VERIFIED boolean;");
                oldVersion++;
            case 4:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add ORIGIN integer;");
                oldVersion++;
            case 5:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add HAS_FREE_PLAN boolean;");
                oldVersion++;
            case 6:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add UNMAPPED_URL text;");
                oldVersion++;
            case 7:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table MediaModel add LOCAL_POST_ID integer;");
                oldVersion++;
            case 8:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table MediaModel add FILE_NAME_MEDIUM_SIZE text;");
                db.execSQL("alter table MediaModel add FILE_NAME_MEDIUM_LARGE_SIZE text;");
                db.execSQL("alter table MediaModel add FILE_NAME_LARGE_SIZE text;");
                oldVersion++;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
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
