package org.wordpress.android.ui.main;

import android.test.InstrumentationTestCase;
import android.view.MenuItem;

import static org.mockito.Mockito.mock;

public class SitePickerSearchViewTest extends InstrumentationTestCase {
    private SitePickerSearchView mSitePickerSearchView;
    private SitePickerActivity mMockSitePickerActivity;
    private MenuItem mMockMenuItem;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSitePickerSearchView = new SitePickerSearchView(getInstrumentation().getContext());
        mMockSitePickerActivity = mock(SitePickerActivity.class);
        mMockMenuItem = mock(MenuItem.class);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSitePickerSearchView = null;
        mMockSitePickerActivity = null;
        mMockMenuItem = null;
    }

    public void testConstructorSetsInputMethodManager() {
        assertNotNull(mSitePickerSearchView.getInputMethodManager());
    }

    public void testConfigureSetsIconifiedByDefaultToFalse() {
        mSitePickerSearchView.configure(mMockSitePickerActivity, mMockMenuItem);

        assertFalse(mSitePickerSearchView.isIconfiedByDefault());
    }

    public void testGetInputMethodManagerReturnsNotNull() {
        assertNotNull(mSitePickerSearchView.getInputMethodManager());
    }
}
