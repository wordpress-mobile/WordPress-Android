package org.wordpress.android.stores.example;

import android.app.Application;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.persistence.WellSqlConfig;

public class ExampleApp extends Application {
    private AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        component = DaggerAppComponent.builder()
                .appContextModule(new AppContextModule(getApplicationContext()))
                .build();
        component().inject(this);
        WellSql.init(new WellSqlConfig(getApplicationContext()));
    }

    public AppComponent component() {
        return component;
    }
}
