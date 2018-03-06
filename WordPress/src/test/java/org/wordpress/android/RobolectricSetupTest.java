package org.wordpress.android;

import android.os.Build;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApplication.class, sdk = Build.VERSION_CODES.JELLY_BEAN)
public class RobolectricSetupTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void init() {
        // nothing special to initialise
    }

    @After
    public void teardown() {
        // nothing special to clean up
    }

    @Test
    public void appNameTest() {
        // this test does nothing fancy but it helps make sure the Robolectric setup is working OK.
        // If running this via AndroidStudio, make sure the run configuration's working directory is set to $MODULE_DIR$
        // and the VM options to `-ea`
        Assert.assertEquals("WordPress for Android", RuntimeEnvironment.application.getString(R.string.app_title));
    }
}
