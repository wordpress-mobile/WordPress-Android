package org.wordpress.android.networking;

import android.content.Context;
import android.os.Handler;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import junit.framework.Assert;

import org.wordpress.android.TestUtils;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.networking.menus.MenusRestWPCom;
import org.wordpress.android.networking.menus.MenusRestWPCom.MenusListener;
import org.wordpress.android.ui.accounts.helpers.LoginAbstract.Callback;
import org.wordpress.android.ui.accounts.helpers.UpdateBlogListTask.GenericUpdateBlogListTask;
import org.wordpress.android.util.CoreEvents;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.test.BuildConfig.*;

public class MenusRestWPComTest extends InstrumentationTestCase {
    private MenusRestWPCom mTestRest;
    private Context mTargetContext;
    private int mTestRequest;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mTestRequest = -1;
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
                            @Override public void onMenuCreated(int requestId, MenuModel menu) {
                                success[0] = requestId == mTestRequest && menu != null && testName.equals(menu.name);
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.createMenu(goodMenu);
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testCreateNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertEquals(-1, mTestRest.createMenu(null));
    }

    public void testCreateBadMenu() {
        final MenuModel badMenu = getMenu(0, null);
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertEquals(-1, mTestRest.createMenu(badMenu));
        badMenu.name = "";
        Assert.assertEquals(-1, mTestRest.createMenu(badMenu));
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
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) {
                                success[0] = requestId == mTestRequest && menu != null && testName.equals(menu.name);
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.updateMenu(goodMenu);
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
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) {
                                success[0] = requestId == mTestRequest && menu == null;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.updateMenu(badMenu);
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testUpdateNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertEquals(-1, mTestRest.updateMenu(null));
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
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) {
                                success[0] = requestId == mTestRequest && menus != null && menus.size() > 0;
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.fetchAllMenus();
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
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) {
                                success[0] = requestId == mTestRequest && menus != null && menus.size() > 0 && menus.get(0).menuId == expectedId;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.fetchMenu(expectedId);
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
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) {
                                success[0] = requestId == mTestRequest && menus.isEmpty();
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        mTestRequest = mTestRest.fetchMenu(Long.valueOf(TEST_WPCOM_BAD_MENU_ID));
                    }
                });
            }
        };
        mLatch.await(60, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testFetchMenuId0() throws InterruptedException {
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
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) {
                                success[0] = requestId == mTestRequest && error == MenusRestWPCom.REST_ERROR.RESERVED_ID_ERROR;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                        });
                        mTestRequest = mTestRest.fetchMenu(0L);
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
                            @Override public void onMenuCreated(int requestId, MenuModel menu) {
                                Assert.assertTrue(requestId == mTestRequest);
                                Assert.assertEquals(testName, menu.name);
                                Assert.assertTrue((mTestRequest = mTestRest.deleteMenu(menu)) != -1);
                            }
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) {
                                success[0] = requestId == mTestRequest && deleted;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        Assert.assertTrue((mTestRequest = mTestRest.createMenu(goodMenu)) != -1);
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
                            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) {
                                success[0] = requestId == mTestRequest && !deleted;
                                countDown();
                            }
                            @Override public Context getContext() { return mTargetContext; }
                            @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
                            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
                            @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
                        });
                        Assert.assertTrue((mTestRequest = mTestRest.deleteMenu(badMenu)) != -1);
                    }
                });
            }
        };
        mLatch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(success[0]);
    }

    public void testDeleteNullMenu() {
        mTestRest = new MenusRestWPCom(EMPTY_DELEGATE);
        Assert.assertEquals(-1, mTestRest.deleteMenu(null));
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
        @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) { countDown(); }
        @Override public void onMenuCreated(int requestId, MenuModel menu) { countDown(); }
        @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) { countDown(); }
        @Override public void onMenuUpdated(int requestId, MenuModel menu) { countDown(); }
        @Override public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) { countDown(); }
    };
}
