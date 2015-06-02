package org.wordpress.android.ui.main;

import android.test.InstrumentationTestCase;
import android.view.Menu;
import android.view.MenuItem;

import static org.mockito.Mockito.mock;

public class SitePickerSearchViewTest extends InstrumentationTestCase {
    private SitePickerSearchView mSitePickerSearchView;
    private SitePickerActivity mMockSitePickerActivity;
    private Menu mMockMenu;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mSitePickerSearchView = new SitePickerSearchView(getInstrumentation().getContext());
        mMockSitePickerActivity = mock(SitePickerActivity.class);
        mMockMenu = mock(Menu.class);

    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        mSitePickerSearchView = null;
        mMockSitePickerActivity = null;
        mMockMenu = null;
    }

    public void testConstructorSetsInputMethodManager() {
        assertNotNull(mSitePickerSearchView.getInputMethodManager());
    }

    public void testConfigureSetsIconifiedByDefaultToFalse() {
        mSitePickerSearchView.configure(mMockSitePickerActivity, mMockMenu);

        assertFalse(mSitePickerSearchView.isIconfiedByDefault());
    }

    public void testGetInputMethodManagerReturnsNotNull() {
        assertNotNull(mSitePickerSearchView.getInputMethodManager());
    }
}
