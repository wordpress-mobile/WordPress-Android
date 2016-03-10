package org.wordpress.android.stores.release;

import android.content.Context;
import android.test.InstrumentationTestCase;

import com.yarolegovich.wellsql.WellSql;

import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.persistence.WellSqlConfig;

public class ReleaseStack_Base extends InstrumentationTestCase {
    Context mAppContext;
    ReleaseStack_AppComponent mReleaseStackAppComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Needed for Mockito
        System.setProperty("dexmaker.dexcache", getInstrumentation().getTargetContext().getCacheDir().getPath());
        mAppContext = getInstrumentation().getTargetContext().getApplicationContext();

        mReleaseStackAppComponent = DaggerReleaseStack_AppComponent.builder()
                .appContextModule(new AppContextModule(mAppContext))
                .build();
        WellSqlConfig config = new WellSqlConfig(mAppContext);
        WellSql.init(config);
        config.reset();
    }
}
