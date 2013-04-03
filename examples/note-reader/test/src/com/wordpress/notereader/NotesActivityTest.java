package com.wordpress.notereader;

import android.test.ActivityInstrumentationTestCase2;

/**
 * This is a simple framework for a test of an Application.  See
 * {@link android.test.ApplicationTestCase ApplicationTestCase} for more information on
 * how to write and extend Application tests.
 * <p/>
 * To run this test, you can type:
 * adb shell am instrument -w \
 * -e class com.wordpress.notereader.NotesActivityTest \
 * com.wordpress.notereader.tests/android.test.InstrumentationTestRunner
 */
public class NotesActivityTest extends ActivityInstrumentationTestCase2<NotesActivity> {

    public NotesActivityTest() {
        super("com.wordpress.notereader", NotesActivity.class);
    }
    
    public void testFirst(){
        NotesActivity mActivity = getActivity();
        assertTrue("Time to test", false);
    }
    
}
