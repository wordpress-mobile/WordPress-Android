package org.wordpress.android.modules;

import org.wordpress.android.GCMMessageService;
import org.wordpress.android.GCMRegistrationIntentService;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.WPDelayedHurlStack;
import org.wordpress.android.stores.module.AppContextModule;
import org.wordpress.android.stores.module.ReleaseBaseModule;
import org.wordpress.android.stores.module.ReleaseNetworkModule;
import org.wordpress.android.stores.module.ReleaseStoreModule;
import org.wordpress.android.ui.DeepLinkingIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.NewBlogFragment;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.main.MeFragment;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.notifications.NotificationsDetailActivity;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.prefs.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment;
import org.wordpress.android.ui.reader.ReaderCommentListActivity;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.views.ReaderBlogInfoView;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderTagInfoView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureActivity;
import org.wordpress.android.util.HelpshiftHelper;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class
})
public interface AppComponent {
    void inject(WordPress application);
    void inject(WPMainActivity object);
    void inject(SignInFragment object);
    void inject(NewBlogFragment object);

    void inject(StatsWidgetConfigureActivity object);
    void inject(StatsActivity object);
    void inject(StatsAbstractFragment object);

    void inject(GCMMessageService object);
    void inject(GCMRegistrationIntentService object);
    void inject(DeepLinkingIntentReceiverActivity object);
    void inject(ShareIntentReceiverActivity object);

    void inject(HelpshiftHelper object);

    void inject(CommentDetailFragment object);

    void inject(MeFragment object);
    void inject(AccountSettingsFragment object);
    void inject(MySiteFragment object);
    void inject(SitePickerActivity object);
    void inject(SitePickerAdapter object);
    void inject(SiteSettingsFragment object);
    void inject(BlogPreferencesActivity object);
    void inject(AppSettingsFragment object);
    void inject(PeopleManagementActivity object);
    void inject(PlansActivity object);

    void inject(EditPostActivity object);

    void inject(NotificationsListFragment object);
    void inject(NotificationsSettingsFragment object);
    void inject(NotificationsDetailActivity object);

    void inject(ReaderCommentListActivity object);
    void inject(ReaderUpdateService object);
    void inject(ReaderPostDetailFragment object);
    void inject(ReaderPostListFragment object);
    void inject(ReaderCommentAdapter object);
    void inject(ReaderPostAdapter object);
    void inject(ReaderBlogInfoView object);
    void inject(ReaderTagInfoView object);
    void inject(ReaderLikingUsersView object);
    void inject(ReaderWebView object);

    void inject(WPWebViewActivity object);

    // WPDelayedHurlStack will burn in hell as soon as we have all the XMLRPC based stores ready.
    void inject(WPDelayedHurlStack object);
}
