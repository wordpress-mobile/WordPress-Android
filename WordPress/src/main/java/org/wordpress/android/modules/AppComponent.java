package org.wordpress.android.modules;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.module.AppContextModule;
import org.wordpress.android.fluxc.module.ReleaseBaseModule;
import org.wordpress.android.fluxc.module.ReleaseNetworkModule;
import org.wordpress.android.fluxc.module.ReleaseStoreModule;
import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.AddQuickPressShortcutActivity;
import org.wordpress.android.ui.DeepLinkingIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.NewBlogFragment;
import org.wordpress.android.ui.accounts.NewUserFragment;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.accounts.SignInDialogFragment;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.ui.accounts.login.MagicLinkRequestFragment;
import org.wordpress.android.ui.comments.CommentAdapter;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.comments.CommentsListFragment;
import org.wordpress.android.ui.comments.EditCommentActivity;
import org.wordpress.android.ui.main.MeFragment;
import org.wordpress.android.ui.main.MySiteFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.notifications.NotificationsDetailActivity;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.notifications.NotificationsListFragment;
import org.wordpress.android.ui.notifications.services.NotificationsPendingDraftsService;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.people.PeopleInviteFragment;
import org.wordpress.android.ui.people.PeopleManagementActivity;
import org.wordpress.android.ui.people.PersonDetailFragment;
import org.wordpress.android.ui.plans.PlanUpdateService;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.posts.AddCategoryActivity;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.EditPostSettingsFragment;
import org.wordpress.android.ui.posts.PostPreviewActivity;
import org.wordpress.android.ui.posts.PostPreviewFragment;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.posts.PostsListFragment;
import org.wordpress.android.ui.posts.SelectCategoriesActivity;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.ui.posts.services.PostMediaService;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.ui.prefs.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.DotComSiteSettings;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.MyProfileFragment;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment;
import org.wordpress.android.ui.reader.ReaderCommentListActivity;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.reader.ReaderSubsActivity;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.services.ReaderCommentService;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.ui.reader.services.ReaderSearchService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderPostDetailHeaderView;
import org.wordpress.android.ui.reader.views.ReaderSimplePostView;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.stats.FollowHelper;
import org.wordpress.android.ui.stats.ReferrerSpamHelper;
import org.wordpress.android.ui.stats.StatsAbstractFragment;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.stats.StatsSingleItemDetailsActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureActivity;
import org.wordpress.android.ui.stats.StatsWidgetConfigureAdapter;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.suggestion.service.SuggestionService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.ui.themes.ThemeWebActivity;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.widgets.WPNetworkImageView;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        AppContextModule.class,
        AppSecretsModule.class,
        ReleaseBaseModule.class,
        ReleaseNetworkModule.class,
        ReleaseStoreModule.class,
        LegacyRestClientModule.class
})
public interface AppComponent {
    void inject(WordPress application);
    void inject(WPMainActivity object);
    void inject(SignInActivity object);
    void inject(SignInFragment object);
    void inject(NewBlogFragment object);
    void inject(SignInDialogFragment object);
    void inject(NewUserFragment object);
    void inject(MagicLinkRequestFragment object);

    void inject(StatsWidgetConfigureActivity object);
    void inject(StatsWidgetConfigureAdapter object);
    void inject(StatsActivity object);
    void inject(StatsAbstractFragment object);
    void inject(StatsService object);
    void inject(StatsWidgetProvider object);
    void inject(StatsSingleItemDetailsActivity object);
    void inject(ReferrerSpamHelper object);
    void inject(FollowHelper object);

    void inject(GCMMessageService object);
    void inject(GCMRegistrationIntentService object);
    void inject(DeepLinkingIntentReceiverActivity object);
    void inject(ShareIntentReceiverActivity object);
    void inject(AddQuickPressShortcutActivity object);

    void inject(HelpActivity object);

    void inject(CommentDetailFragment object);
    void inject(EditCommentActivity object);
    void inject(CommentAdapter object);
    void inject(CommentsListFragment object);
    void inject(CommentsActivity object);

    void inject(MeFragment object);
    void inject(MyProfileActivity object);
    void inject(MyProfileFragment object);
    void inject(AccountSettingsFragment object);
    void inject(MySiteFragment object);
    void inject(SitePickerActivity object);
    void inject(SitePickerAdapter object);
    void inject(SiteSettingsFragment object);
    void inject(BlogPreferencesActivity object);
    void inject(AppSettingsFragment object);
    void inject(PeopleManagementActivity object);
    void inject(PeopleInviteFragment object);
    void inject(PersonDetailFragment object);
    void inject(PlansActivity object);
    void inject(PlanUpdateService object);
    void inject(DotComSiteSettings object);

    void inject(EditPostActivity object);
    void inject(EditPostSettingsFragment object);

    void inject(PostPreviewActivity object);
    void inject(PostPreviewFragment object);

    void inject(PostsListActivity object);
    void inject(PostsListFragment object);
    void inject(PostsListAdapter object);

    void inject(NotificationsListFragment object);
    void inject(NotificationsSettingsFragment object);
    void inject(NotificationsDetailActivity object);
    void inject(NotificationsProcessingService object);
    void inject(NotificationsUpdateService object);
    void inject(NotificationsDetailListFragment object);

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
    void inject(ReaderBlogActions object);
    void inject(ReaderPostService object);
    void inject(ReaderSimplePostView object);
    void inject(ReaderSubsActivity object);
    void inject(ReaderCommentService object);
    void inject(ReaderSearchService object);
    void inject(ReaderPostDetailHeaderView object);
    void inject(ReaderTagAdapter object);

    void inject(WPWebViewActivity object);
    void inject(WPWebViewClient object);
    void inject(PostUploadService object);

    void inject(ThemeWebActivity object);
    void inject(ThemeBrowserActivity object);

    void inject(MediaDeleteService object);
    void inject(PostMediaService object);
    void inject(NotificationsPendingDraftsService object);

    void inject(SelectCategoriesActivity object);
    void inject(AddCategoryActivity object);
    void inject(SuggestionService object);

    void inject(WPNetworkImageView object);
}
