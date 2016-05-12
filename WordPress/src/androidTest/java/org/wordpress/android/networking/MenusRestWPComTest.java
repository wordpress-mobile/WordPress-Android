package org.wordpress.android.networking;

import android.content.Context;
import android.os.Handler;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import junit.framework.Assert;

import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.networking.menus.MenusRestWPCom;
import org.wordpress.android.networking.menus.MenusRestWPCom.MenusListener;
import org.wordpress.android.ui.accounts.helpers.LoginAbstract.Callback;
import org.wordpress.android.ui.accounts.helpers.UpdateBlogListTask.GenericUpdateBlogListTask;
import org.wordpress.android.util.CoreEvents;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.test.BuildConfig.*;

public class MenusRestWPComTest extends InstrumentationTestCase {
    private MenusRestWPCom mTestRest;
    private Context mTargetContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTargetContext = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "test_");
        WordPress.WordPressComSignOut(mTargetContext);
        TestUtils.resetEventBus();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        EventBus.getDefault().unregister(this);
        mBlogListHandler = null;
        mLatch = null;
    }

    public void testCreateGoodMenu() throws InterruptedException {
        final String testName = "MenusTest-" + System.currentTimeMillis();
        final boolean[] success = { false };
        final MenuModel goodMenu = getMenu(0, testName);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) {
                                Assert.assertTrue(statusCode == HttpURLConnection.HTTP_OK);
                                success[0] = menu != null && testName.equals(menu.name);
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                        });
                        Assert.assertTrue(mTestRest.createMenu(goodMenu));
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testCreateNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertFalse(mTestRest.createMenu(null));
    }

    public void testCreateBadMenu() {
        final MenuModel badMenu = getMenu(0, null);
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertFalse(mTestRest.createMenu(badMenu));
        badMenu.name = "";
        Assert.assertFalse(mTestRest.createMenu(badMenu));
    }

    public void testUpdateGoodMenu() throws InterruptedException {
        final boolean[] success = { false };
        final String testName = "MenusTest-" + System.currentTimeMillis();
        final MenuModel goodMenu = getMenu(Long.valueOf(TEST_WPCOM_KNOWN_MENU_ID), testName);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) {
                                Assert.assertTrue(statusCode == HttpURLConnection.HTTP_OK);
                                success[0] = menu != null && testName.equals(menu.name);
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                        });
                        Assert.assertTrue(mTestRest.updateMenu(goodMenu));
                    }
                });
            }
        };
        mLatch.await(100, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testUpdateBadMenu() throws InterruptedException {
        final boolean[] success = { false };
        final String testName = "MenusTest-" + System.currentTimeMillis();
        final MenuModel badMenu = getMenu(Long.valueOf(TEST_WPCOM_BAD_MENU_ID), testName);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) {
                                success[0] = menu == null;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                        });
                        Assert.assertTrue(mTestRest.updateMenu(badMenu));
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testUpdateNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertFalse(mTestRest.updateMenu(null));
    }

    public void testFetchAllMenus() throws InterruptedException {
        final boolean[] success = { false };
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) {
                                if (menus != null && menus.size() > 0) success[0] = true;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                        });
                        mTestRest.fetchAllMenus();
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testFetchGoodMenu() throws InterruptedException {
        final boolean[] success = { false };
        final long expectedId = Long.valueOf(TEST_WPCOM_KNOWN_MENU_ID);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menu) {
                                Assert.assertTrue(statusCode == HttpURLConnection.HTTP_OK);
                                success[0] = menu != null && menu.size() > 0 && menu.get(0).menuId == expectedId;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                        });
                        mTestRest.fetchMenu(expectedId);
                    }
                });
            }
        };
        mLatch.await(60, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testFetchBadMenu() throws InterruptedException {
        final boolean[] success = { false };
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menu) {
                                success[0] = menu == null;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                        });
                        mTestRest.fetchMenu(Long.valueOf(TEST_WPCOM_BAD_MENU_ID));
                    }
                });
            }
        };
        mLatch.await(60, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testDeleteGoodMenu() throws InterruptedException {
        final boolean[] success = { false };
        final String testName = "MenusTest-" + System.currentTimeMillis();
        final MenuModel goodMenu = getMenu(0, testName);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) {
                                Assert.assertTrue(statusCode == HttpURLConnection.HTTP_OK);
                                Assert.assertEquals(testName, menu.name);
                                Assert.assertTrue(mTestRest.deleteMenu(menu));
                            }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) {
                                success[0] = deleted;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
                        });
                        Assert.assertTrue(mTestRest.createMenu(goodMenu));
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testDeleteBadMenu() throws InterruptedException {
        final boolean[] success = { false };
        final String testName = "MenusTest-" + System.currentTimeMillis();
        final MenuModel badMenu = getMenu(Long.valueOf(TEST_WPCOM_BAD_MENU_ID), testName);
        TestUtils.loginWPCom(TEST_WPCOM_USERNAME_TEST1, TEST_WPCOM_PASSWORD_TEST1, loginAndUpdateBlogs);
        mLatch = new CountDownLatch(1);
        mBlogListHandler = new OnBlogListChanged() {
            @Override
            public void eventRecieved(CoreEvents.BlogListChanged event) {
                Handler mainLooperHandler = new Handler(mTargetContext.getMainLooper());
                mainLooperHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mTestRest = new MenusRestWPCom(new MenusListener() {
                            @Override public long getSiteId() {
                                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
                            }
                            @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) {
                                success[0] = !deleted;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
                        });
                        Assert.assertTrue(mTestRest.deleteMenu(badMenu));
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testDeleteNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertFalse(mTestRest.deleteMenu(null));
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.BlogListChanged event) {
        if (mBlogListHandler != null) mBlogListHandler.eventRecieved(event);
    }

    private MenuModel getMenu(long id, String name) {
        MenuModel menu = new MenuModel();
        menu.menuId = id;
        menu.name = name;
        return menu;
    }

    private Callback loginAndUpdateBlogs = new Callback() {
        @Override
        public void onSuccess() {
            GenericUpdateBlogListTask getBlogs = new GenericUpdateBlogListTask(mTargetContext);
            getBlogs.execute();
        }

        @Override
        public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate) {
            countDown();
            Assert.fail();
        }
    };

    private interface OnBlogListChanged {
        void eventRecieved(CoreEvents.BlogListChanged event);
    }
    private OnBlogListChanged mBlogListHandler;

    private CountDownLatch mLatch;
    private void countDown() {
        if (mLatch != null) mLatch.countDown();
    }

    private final MenusListener EMPTY_DELEGATE = new MenusListener() {
        @Override public long getSiteId() { return -1; }
        @Override public Context getContext() { return null; }
        @Override public void onMenusReceived(int statusCode, List<MenuModel> menus) { countDown(); }
        @Override public void onMenuCreated(int statusCode, MenuModel menu) { countDown(); }
        @Override public void onMenuDeleted(int statusCode, MenuModel menu, boolean deleted) { countDown(); }
        @Override public void onMenuUpdated(int statusCode, MenuModel menu) { countDown(); }
    };
}
