package org.wordpress.android;

import android.content.Context;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import org.wordpress.android.mocks.RestClientFactoryTest;
import org.wordpress.android.util.AppLog;

public class DefaultMocksInstrumentationTestCase extends InstrumentationTestCase {
    protected Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        FactoryUtils.initWithTestFactories();

        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        TestUtils.clearApplicationState(mTargetContext);

        // Init contexts
        RestClientFactoryTest.sContext = getInstrumentation().getContext();
        AppLog.v(AppLog.T.TESTS, "Contexts set");

        // Set mode to Customizable
        RestClientFactoryTest.sMode = RestClientFactoryTest.Mode.CUSTOMIZABLE;
        AppLog.v(AppLog.T.TESTS, "Modes set to customizable");

        // Set default variant
        RestClientFactoryTest.setPrefixAllInstances("default");
    }

    @Override
    protected void tearDown() throws Exception {
        FactoryUtils.clearFactories();
        super.tearDown();
    }
}
