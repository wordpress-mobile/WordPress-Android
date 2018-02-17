package org.wordpress.android.modules;

import android.app.Application;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule;
import org.wordpress.android.fluxc.module.ReleaseToolsModule;
import org.wordpress.android.login.di.LoginFragmentModule;
import org.wordpress.android.login.di.LoginServiceModule;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.AddQuickPressShortcutActivity;
import org.wordpress.android.ui.DeepLinkingIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverFragment;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.LoginActivity;
import org.wordpress.android.ui.accounts.LoginEpilogueActivity;
import org.wordpress.android.ui.accounts.NewBlogFragment;
import org.wordpress.android.ui.accounts.SignInDialogFragment;
import org.wordpress.android.ui.accounts.SiteCreationActivity;
import org.wordpress.android.ui.accounts.login.LoginEpilogueFragment;
import org.wordpress.android.ui.accounts.signup.SignupEmailFragment;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueFragment;
import org.wordpress.android.ui.accounts.signup.SignupGoogleFragment;
import org.wordpress.android.ui.accounts.signup.SignupMagicLinkFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationCategoryFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainAdapter;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainLoaderFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationService;
import org.wordpress.android.ui.accounts.signup.SiteCreationSiteDetailsFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeAdapter;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeLoaderFragment;
import org.wordpress.android.ui.accounts.signup.UsernameChangerFullScreenDialogFragment;
import org.wordpress.android.ui.comments.CommentAdapter;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.comments.CommentsDetailActivity;
import org.wordpress.android.ui.comments.CommentsListFragment;
import org.wordpress.android.ui.comments.EditCommentActivity;
import org.wordpress.android.ui.main.MeFragment;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaEditFragment;
import org.wordpress.android.ui.media.MediaGridFragment;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.ui.media.MediaPreviewFragment;
import org.wordpress.android.ui.media.MediaSettingsActivity;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.notifications.NotificationsDetailActivity;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.people.PeopleInviteFragment;
import org.wordpress.android.ui.people.PeopleListFragment;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.people.PersonDetailFragment;
import org.wordpress.android.ui.people.RoleChangeDialogFragment;
import org.wordpress.android.ui.people.RoleSelectDialogFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.plugins.PluginBrowserActivity;
import org.wordpress.android.ui.plugins.PluginDetailActivity;
import org.wordpress.android.ui.plugins.PluginListFragment;
import org.wordpress.android.ui.posts.AddCategoryFragment;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.EditPostSettingsFragment;
import org.wordpress.android.ui.posts.PostPreviewActivity;
import org.wordpress.android.ui.posts.PostPreviewFragment;
import org.wordpress.android.ui.posts.PostSettingsTagsActivity;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.posts.SelectCategoriesActivity;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.prefs.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.MyProfileFragment;
import org.wordpress.android.ui.prefs.ReleaseNotesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsTagDetailFragment;
import org.wordpress.android.ui.prefs.SiteSettingsTagListActivity;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment;
import org.wordpress.android.ui.publicize.PublicizeButtonPrefsFragment;
import org.wordpress.android.ui.publicize.PublicizeDetailFragment;
import org.wordpress.android.ui.publicize.PublicizeListActivity;
import org.wordpress.android.ui.publicize.PublicizeListFragment;
import org.wordpress.android.ui.publicize.PublicizeWebViewFragment;
import org.wordpress.android.ui.reader.ReaderCommentListActivity;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity;
import org.wordpress.android.ui.stats.StatsDeeplinkActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureAdapter;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.themes.ThemeBrowserFragment;
import org.wordpress.android.ui.uploads.MediaUploadHandler;
import org.wordpress.android.ui.uploads.PostUploadHandler;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.util.HtmlToSpannedConverter;
import org.wordpress.android.util.WPWebViewClient;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
@Component(modules = {
        ApplicationModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseOkHttpClientModule.class,
        ReleaseNetworkModule.class,
        LegacyModule.class,
        ReleaseToolsModule.class,
        AndroidSupportInjectionModule.class,
        ViewModelModule.class,
        // Login flow library
        LoginAnalyticsModule.class,
        LoginFragmentModule.class,
        LoginServiceModule.class
})
public interface AppComponent extends AndroidInjector<WordPress> {
    @Override
    void inject(WordPress instance);

    void inject(WPMainActivity object);
    void inject(NewBlogFragment object);
    void inject(SignInDialogFragment object);
    void inject(SignupEmailFragment object);
    void inject(SignupMagicLinkFragment object);
    void inject(SiteCreationService object);

    void inject(UploadService object);
    void inject(MediaUploadHandler object);
    void inject(PostUploadHandler object);

    void inject(LoginActivity object);
    void inject(LoginEpilogueActivity object);
    void inject(LoginEpilogueFragment object);
    void inject(SignupEpilogueFragment object);
    void inject(UsernameChangerFullScreenDialogFragment object);

    void inject(SignupGoogleFragment object);

    void inject(SiteCreationActivity object);
    void inject(SiteCreationCategoryFragment object);
    void inject(SiteCreationThemeFragment object);
    void inject(SiteCreationThemeLoaderFragment object);
    void inject(SiteCreationThemeAdapter object);
    void inject(SiteCreationSiteDetailsFragment object);
    void inject(SiteCreationDomainFragment object);
    void inject(SiteCreationDomainLoaderFragment object);
    void inject(SiteCreationDomainAdapter object);

    void inject(StatsWidgetConfigureActivity object);
    void inject(StatsWidgetConfigureAdapter object);
    void inject(StatsActivity object);
    void inject(StatsDeeplinkActivity object);
    void inject(StatsConnectJetpackActivity object);
    void inject(StatsAbstractFragment object);
    void inject(StatsService object);
    void inject(StatsWidgetProvider object);

    void inject(GCMMessageService object);
    void inject(GCMRegistrationIntentService object);
    void inject(DeepLinkingIntentReceiverActivity object);
    void inject(ShareIntentReceiverActivity object);
    void inject(ShareIntentReceiverFragment object);
    void inject(AddQuickPressShortcutActivity object);

    void inject(HelpActivity object);

    void inject(CommentDetailFragment object);
    void inject(EditCommentActivity object);
    void inject(CommentAdapter object);
    void inject(CommentsListFragment object);
    void inject(CommentsActivity object);
    void inject(CommentsDetailActivity object);

    void inject(MeFragment object);
    void inject(MyProfileActivity object);
    void inject(MyProfileFragment object);
    void inject(AccountSettingsFragment object);
    void inject(MySiteFragment object);
    void inject(SitePickerActivity object);
    void inject(SitePickerAdapter object);
    void inject(SiteSettingsFragment object);
    void inject(SiteSettingsInterface object);
    void inject(BlogPreferencesActivity object);
    void inject(AppSettingsFragment object);
    void inject(PeopleManagementActivity object);
    void inject(PeopleListFragment object);
    void inject(PersonDetailFragment object);
    void inject(RoleChangeDialogFragment object);
    void inject(PeopleInviteFragment object);
    void inject(RoleSelectDialogFragment object);
    void inject(PlansActivity object);
    void inject(MediaBrowserActivity object);
    void inject(MediaGridFragment object);
    void inject(MediaEditFragment object);
    void inject(MediaPreviewActivity object);
    void inject(MediaPreviewFragment object);
    void inject(MediaSettingsActivity object);
    void inject(PhotoPickerActivity object);

    void inject(SiteSettingsTagListActivity object);
    void inject(SiteSettingsTagDetailFragment object);

    void inject(PublicizeListActivity object);
    void inject(PublicizeWebViewFragment object);
    void inject(PublicizeDetailFragment object);
    void inject(PublicizeListFragment object);
    void inject(PublicizeButtonPrefsFragment object);

    void inject(EditPostActivity object);
    void inject(EditPostSettingsFragment object);
    void inject(PostSettingsTagsActivity object);

    void inject(PostPreviewActivity object);
    void inject(PostPreviewFragment object);

    void inject(PostsListActivity object);
    void inject(PostsListFragment object);
    void inject(PostsListAdapter object);

    void inject(NotificationsListFragment object);
    void inject(NotificationsSettingsFragment object);
    void inject(NotificationsDetailActivity object);
    void inject(NotificationsProcessingService object);
    void inject(NotificationsPendingDraftsReceiver object);

    void inject(ReaderCommentListActivity object);
    void inject(ReaderUpdateService object);
    void inject(ReaderPostDetailFragment object);
    void inject(ReaderPostListFragment object);
    void inject(ReaderCommentAdapter object);
    void inject(ReaderPostAdapter object);
    void inject(ReaderSiteHeaderView object);
    void inject(ReaderTagHeaderView object);
    void inject(ReaderLikingUsersView object);
    void inject(ReaderWebView object);
    void inject(ReaderPostPagerActivity object);

    void inject(ReleaseNotesActivity object);
    void inject(WPWebViewActivity object);
    void inject(WPWebViewClient object);

    void inject(ThemeBrowserActivity object);
    void inject(ThemeBrowserFragment object);

    void inject(MediaDeleteService object);

    void inject(SelectCategoriesActivity object);
    void inject(AddCategoryFragment object);

    void inject(HtmlToSpannedConverter object);

    void inject(PluginBrowserActivity object);
    void inject(PluginListFragment object);
    void inject(PluginDetailActivity object);

    void inject(WordPressGlideModule object);

    // Allows us to inject the application without having to instantiate any modules, and provides the Application
    // in the app graph
    @Component.Builder
    interface Builder {
        @BindsInstance
        AppComponent.Builder application(Application application);

        AppComponent build();
    }
}
