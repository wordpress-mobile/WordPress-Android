package org.wordpress.android.modules;

import com.automattic.android.tracks.crashlogging.CrashLogging;

import org.wordpress.android.push.GCMMessageService;
import org.wordpress.android.push.GCMRegistrationIntentService;
import org.wordpress.android.push.NotificationsProcessingService;
import org.wordpress.android.ui.AddQuickPressShortcutActivity;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.JetpackConnectionResultActivity;
import org.wordpress.android.ui.JetpackRemoteInstallFragment;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.ShareIntentReceiverFragment;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.about.UnifiedAboutActivity;
import org.wordpress.android.ui.accounts.PostSignupInterstitialActivity;
import org.wordpress.android.ui.accounts.SignupEpilogueActivity;
import org.wordpress.android.ui.accounts.signup.SignupEpilogueFragment;
import org.wordpress.android.ui.activitylog.detail.ActivityLogDetailFragment;
import org.wordpress.android.ui.activitylog.list.ActivityLogListFragment;
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterFragment;
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment;
import org.wordpress.android.ui.bloggingreminders.BloggingReminderBottomSheetFragment;
import org.wordpress.android.ui.bloggingreminders.BloggingReminderTimePicker;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentsDetailActivity;
import org.wordpress.android.ui.comments.EditCommentActivity;
import org.wordpress.android.ui.comments.unified.EditCancelDialogFragment;
import org.wordpress.android.ui.comments.unified.UnifiedCommentDetailsFragment;
import org.wordpress.android.ui.comments.unified.UnifiedCommentListAdapter;
import org.wordpress.android.ui.comments.unified.UnifiedCommentListFragment;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsActivity;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsDetailsActivity;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditFragment;
import org.wordpress.android.ui.debug.cookies.DebugCookiesFragment;
import org.wordpress.android.ui.domains.DomainRegistrationActivity;
import org.wordpress.android.ui.domains.DomainRegistrationDetailsFragment;
import org.wordpress.android.ui.domains.DomainRegistrationResultFragment;
import org.wordpress.android.ui.domains.DomainSuggestionsFragment;
import org.wordpress.android.ui.domains.DomainsDashboardFragment;
import org.wordpress.android.ui.engagement.EngagedPeopleListActivity;
import org.wordpress.android.ui.engagement.EngagedPeopleListFragment;
import org.wordpress.android.ui.engagement.UserProfileBottomSheetFragment;
import org.wordpress.android.ui.history.HistoryAdapter;
import org.wordpress.android.ui.history.HistoryDetailContainerFragment;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadActivity;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadFragment;
import org.wordpress.android.ui.jetpack.restore.RestoreActivity;
import org.wordpress.android.ui.jetpack.restore.RestoreFragment;
import org.wordpress.android.ui.jetpack.scan.ScanFragment;
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsFragment;
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryFragment;
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListFragment;
import org.wordpress.android.ui.layoutpicker.LayoutPreviewFragment;
import org.wordpress.android.ui.layoutpicker.LayoutsAdapter;
import org.wordpress.android.ui.main.AddContentAdapter;
import org.wordpress.android.ui.main.MainBottomSheetFragment;
import org.wordpress.android.ui.main.MeFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.main.SitePickerAdapter;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.media.MediaGridAdapter;
import org.wordpress.android.ui.media.MediaGridFragment;
import org.wordpress.android.ui.media.MediaPreviewActivity;
import org.wordpress.android.ui.media.MediaPreviewFragment;
import org.wordpress.android.ui.media.MediaSettingsActivity;
import org.wordpress.android.ui.media.services.MediaDeleteService;
import org.wordpress.android.ui.mediapicker.MediaPickerActivity;
import org.wordpress.android.ui.mediapicker.MediaPickerFragment;
import org.wordpress.android.ui.mlp.ModalLayoutPickerFragment;
import org.wordpress.android.ui.mysite.MySiteFragment;
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuFragment;
import org.wordpress.android.ui.mysite.tabs.MySiteTabFragment;
import org.wordpress.android.ui.notifications.DismissNotificationReceiver;
import org.wordpress.android.ui.notifications.NotificationsDetailActivity;
import org.wordpress.android.ui.notifications.NotificationsDetailListFragment;
import org.wordpress.android.ui.notifications.NotificationsListFragmentPage;
import org.wordpress.android.ui.notifications.adapters.NotesAdapter;
import org.wordpress.android.ui.notifications.receivers.NotificationsPendingDraftsReceiver;
import org.wordpress.android.ui.pages.PageListFragment;
import org.wordpress.android.ui.pages.PageParentFragment;
import org.wordpress.android.ui.pages.PageParentSearchFragment;
import org.wordpress.android.ui.pages.PagesActivity;
import org.wordpress.android.ui.pages.PagesFragment;
import org.wordpress.android.ui.pages.SearchListFragment;
import org.wordpress.android.ui.people.PeopleInviteDialogFragment;
import org.wordpress.android.ui.people.PeopleInviteFragment;
import org.wordpress.android.ui.people.PeopleListFragment;
import org.wordpress.android.ui.people.PersonDetailFragment;
import org.wordpress.android.ui.people.RoleChangeDialogFragment;
import org.wordpress.android.ui.people.RoleSelectDialogFragment;
import org.wordpress.android.ui.photopicker.PhotoPickerActivity;
import org.wordpress.android.ui.photopicker.PhotoPickerFragment;
import org.wordpress.android.ui.plans.PlanDetailsFragment;
import org.wordpress.android.ui.plans.PlansActivity;
import org.wordpress.android.ui.plans.PlansListAdapter;
import org.wordpress.android.ui.plans.PlansListFragment;
import org.wordpress.android.ui.plugins.PluginBrowserActivity;
import org.wordpress.android.ui.plugins.PluginDetailActivity;
import org.wordpress.android.ui.plugins.PluginListFragment;
import org.wordpress.android.ui.posts.AddCategoryFragment;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.EditPostPublishSettingsFragment;
import org.wordpress.android.ui.posts.EditPostSettingsFragment;
import org.wordpress.android.ui.posts.HistoryListFragment;
import org.wordpress.android.ui.posts.PostDatePickerDialogFragment;
import org.wordpress.android.ui.posts.PostListCreateMenuFragment;
import org.wordpress.android.ui.posts.PostListFragment;
import org.wordpress.android.ui.posts.PostNotificationScheduleTimeDialogFragment;
import org.wordpress.android.ui.posts.PostSettingsListDialogFragment;
import org.wordpress.android.ui.posts.PostSettingsTagsFragment;
import org.wordpress.android.ui.posts.PostTimePickerDialogFragment;
import org.wordpress.android.ui.posts.PostsListActivity;
import org.wordpress.android.ui.posts.PrepublishingAddCategoryFragment;
import org.wordpress.android.ui.posts.PrepublishingBottomSheetFragment;
import org.wordpress.android.ui.posts.PrepublishingCategoriesFragment;
import org.wordpress.android.ui.posts.PrepublishingHomeAdapter;
import org.wordpress.android.ui.posts.PrepublishingHomeFragment;
import org.wordpress.android.ui.posts.PrepublishingTagsFragment;
import org.wordpress.android.ui.posts.PublishNotificationReceiver;
import org.wordpress.android.ui.posts.QuickStartPromptDialogFragment;
import org.wordpress.android.ui.posts.SelectCategoriesActivity;
import org.wordpress.android.ui.posts.adapters.AuthorSelectionAdapter;
import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsFragment;
import org.wordpress.android.ui.posts.services.AztecVideoLoader;
import org.wordpress.android.ui.prefs.AppSettingsActivity;
import org.wordpress.android.ui.prefs.AppSettingsFragment;
import org.wordpress.android.ui.prefs.BlogPreferencesActivity;
import org.wordpress.android.ui.prefs.MyProfileActivity;
import org.wordpress.android.ui.prefs.MyProfileFragment;
import org.wordpress.android.ui.prefs.ReleaseNotesActivity;
import org.wordpress.android.ui.prefs.SiteSettingsFragment;
import org.wordpress.android.ui.prefs.SiteSettingsInterface;
import org.wordpress.android.ui.prefs.SiteSettingsTagDetailFragment;
import org.wordpress.android.ui.prefs.SiteSettingsTagListActivity;
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsFragment;
import org.wordpress.android.ui.prefs.categories.detail.CategoryDetailFragment;
import org.wordpress.android.ui.prefs.categories.list.CategoriesListFragment;
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDialog;
import org.wordpress.android.ui.prefs.language.LocalePickerBottomSheet;
import org.wordpress.android.ui.prefs.notifications.NotificationsSettingsFragment;
import org.wordpress.android.ui.prefs.timezone.SiteSettingsTimezoneBottomSheet;
import org.wordpress.android.ui.publicize.PublicizeAccountChooserListAdapter;
import org.wordpress.android.ui.publicize.PublicizeButtonPrefsFragment;
import org.wordpress.android.ui.publicize.PublicizeDetailFragment;
import org.wordpress.android.ui.publicize.PublicizeListFragment;
import org.wordpress.android.ui.publicize.PublicizeWebViewFragment;
import org.wordpress.android.ui.publicize.adapters.PublicizeConnectionAdapter;
import org.wordpress.android.ui.publicize.adapters.PublicizeServiceAdapter;
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment;
import org.wordpress.android.ui.reader.CommentNotificationsBottomSheetFragment;
import org.wordpress.android.ui.reader.ReaderBlogFragment;
import org.wordpress.android.ui.reader.ReaderCommentListActivity;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.ReaderPostListActivity;
import org.wordpress.android.ui.reader.ReaderPostListFragment;
import org.wordpress.android.ui.reader.ReaderPostPagerActivity;
import org.wordpress.android.ui.reader.ReaderSearchActivity;
import org.wordpress.android.ui.reader.ReaderSubsActivity;
import org.wordpress.android.ui.reader.SubfilterBottomSheetFragment;
import org.wordpress.android.ui.reader.adapters.CommentSnippetAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderBlogAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderCommentAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderTagAdapter;
import org.wordpress.android.ui.reader.adapters.ReaderUserAdapter;
import org.wordpress.android.ui.reader.discover.ReaderDiscoverFragment;
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment;
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverJobService;
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic;
import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverService;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.views.ReaderCommentsPostHeaderView;
import org.wordpress.android.ui.reader.views.ReaderExpandableTagsView;
import org.wordpress.android.ui.reader.views.ReaderLikingUsersView;
import org.wordpress.android.ui.reader.views.ReaderPostDetailHeaderView;
import org.wordpress.android.ui.reader.views.ReaderSimplePostContainerView;
import org.wordpress.android.ui.reader.views.ReaderSiteHeaderView;
import org.wordpress.android.ui.reader.views.ReaderSiteSearchResultView;
import org.wordpress.android.ui.reader.views.ReaderTagHeaderView;
import org.wordpress.android.ui.reader.views.ReaderWebView;
import org.wordpress.android.ui.sitecreation.services.SiteCreationService;
import org.wordpress.android.ui.sitecreation.theme.DesignPreviewFragment;
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity;
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetBlockListProviderFactory;
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.AllTimeWidgetListProvider;
import org.wordpress.android.ui.stats.refresh.lists.widget.alltime.StatsAllTimeWidget;
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.StatsMinifiedWidget;
import org.wordpress.android.ui.stats.refresh.lists.widget.today.StatsTodayWidget;
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetBlockListProviderFactory;
import org.wordpress.android.ui.stats.refresh.lists.widget.today.TodayWidgetListProvider;
import org.wordpress.android.ui.stats.refresh.lists.widget.views.StatsViewsWidget;
import org.wordpress.android.ui.stats.refresh.lists.widget.views.ViewsWidgetListProvider;
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.StatsWeekWidget;
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekViewsWidgetListProvider;
import org.wordpress.android.ui.stats.refresh.lists.widget.weeks.WeekWidgetBlockListProviderFactory;
import org.wordpress.android.ui.stockmedia.StockMediaPickerActivity;
import org.wordpress.android.ui.stories.StoryComposerActivity;
import org.wordpress.android.ui.stories.intro.StoriesIntroDialogFragment;
import org.wordpress.android.ui.suggestion.SuggestionActivity;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.themes.ThemeBrowserFragment;
import org.wordpress.android.ui.uploads.MediaUploadHandler;
import org.wordpress.android.ui.uploads.MediaUploadReadyProcessor;
import org.wordpress.android.ui.uploads.PostUploadHandler;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementDialogFragment;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementListAdapter;
import org.wordpress.android.util.WPWebViewClient;
import org.wordpress.android.util.image.getters.WPCustomImageGetter;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@EntryPoint
public interface AppComponent {
    void inject(WPMainActivity object);

    void inject(UploadService object);

    void inject(MediaUploadHandler object);

    void inject(PostUploadHandler object);

    void inject(SignupEpilogueActivity object);

    void inject(SignupEpilogueFragment object);

    void inject(PostSignupInterstitialActivity object);

    void inject(JetpackConnectionResultActivity object);

    void inject(StatsConnectJetpackActivity object);

    void inject(GCMMessageService object);

    void inject(GCMRegistrationIntentService object);

    void inject(ShareIntentReceiverActivity object);

    void inject(ShareIntentReceiverFragment object);

    void inject(AddQuickPressShortcutActivity object);

    void inject(CommentDetailFragment object);

    void inject(CommentFullScreenDialogFragment object);

    void inject(EditCommentActivity object);

    void inject(CommentsDetailActivity object);

    void inject(MeFragment object);

    void inject(MyProfileActivity object);

    void inject(MyProfileFragment object);

    void inject(AccountSettingsFragment object);

    void inject(SitePickerActivity object);

    void inject(SitePickerAdapter object);

    void inject(SiteSettingsFragment object);

    void inject(SiteSettingsInterface object);

    void inject(BlogPreferencesActivity object);

    void inject(AppSettingsFragment object);

    void inject(PeopleListFragment object);

    void inject(PersonDetailFragment object);

    void inject(RoleChangeDialogFragment object);

    void inject(PeopleInviteFragment object);

    void inject(RoleSelectDialogFragment object);

    void inject(PeopleInviteDialogFragment object);

    void inject(PlansActivity object);

    void inject(MediaBrowserActivity object);

    void inject(MediaGridFragment object);

    void inject(MediaPreviewActivity object);

    void inject(MediaPreviewFragment object);

    void inject(MediaSettingsActivity object);

    void inject(PhotoPickerActivity object);

    void inject(StockMediaPickerActivity object);

    void inject(SiteSettingsTagListActivity object);

    void inject(SiteSettingsTagDetailFragment object);

    void inject(PublicizeWebViewFragment object);

    void inject(PublicizeDetailFragment object);

    void inject(PublicizeListFragment object);

    void inject(PublicizeButtonPrefsFragment object);

    void inject(EditPostActivity object);

    void inject(EditPostSettingsFragment object);

    void inject(PostSettingsListDialogFragment object);

    void inject(PostsListActivity object);

    void inject(PagesActivity object);

    void inject(AuthorSelectionAdapter object);

    void inject(PostListFragment object);

    void inject(HistoryListFragment object);

    void inject(HistoryAdapter object);

    void inject(HistoryDetailContainerFragment object);

    void inject(NotificationsListFragmentPage object);

    void inject(NotificationsSettingsFragment object);

    void inject(NotificationsDetailActivity object);

    void inject(NotificationsProcessingService object);

    void inject(NotificationsPendingDraftsReceiver object);

    void inject(NotificationsDetailListFragment object);

    void inject(ReaderCommentListActivity object);

    void inject(ReaderSubsActivity object);

    void inject(ReaderUpdateLogic object);

    void inject(ReaderPostDetailFragment object);

    void inject(ReaderPostListFragment object);

    void inject(ReaderCommentAdapter object);

    void inject(ReaderPostAdapter object);

    void inject(ReaderTagAdapter object);

    void inject(PlansListFragment object);

    void inject(ReaderSiteHeaderView object);

    void inject(ReaderSiteSearchResultView object);

    void inject(ReaderTagHeaderView object);

    void inject(ReaderPostDetailHeaderView object);

    void inject(ReaderExpandableTagsView object);

    void inject(ReaderLikingUsersView object);

    void inject(ReaderWebView object);

    void inject(ReaderSimplePostContainerView object);

    void inject(ReaderPostPagerActivity object);

    void inject(ReaderPostListActivity object);

    void inject(ReaderBlogFragment object);

    void inject(ReaderBlogAdapter object);

    void inject(ReaderCommentsPostHeaderView object);

    void inject(ReleaseNotesActivity object);

    void inject(WPWebViewActivity object);

    void inject(WPWebViewClient object);

    void inject(NotesAdapter object);

    void inject(ThemeBrowserFragment object);

    void inject(MediaDeleteService object);

    void inject(SelectCategoriesActivity object);

    void inject(ReaderUserAdapter object);

    void inject(AddCategoryFragment object);

    void inject(PluginBrowserActivity object);

    void inject(ActivityLogListFragment object);

    void inject(ActivityLogDetailFragment object);

    void inject(ScanFragment object);

    void inject(ScanHistoryFragment object);

    void inject(ScanHistoryListFragment object);

    void inject(ThreatDetailsFragment object);

    void inject(PluginListFragment object);

    void inject(PluginDetailActivity object);

    void inject(SuggestionAdapter object);

    void inject(WordPressGlideModule object);

    void inject(QuickStartFullScreenDialogFragment object);

    void inject(MediaGridAdapter object);

    void inject(PagesFragment object);

    void inject(PageListFragment object);

    void inject(SearchListFragment object);

    void inject(PageParentFragment object);

    void inject(WPCustomImageGetter object);

    void inject(PublicizeAccountChooserListAdapter object);

    void inject(PublicizeConnectionAdapter object);

    void inject(PublicizeServiceAdapter object);

    void inject(JetpackRemoteInstallFragment jetpackRemoteInstallFragment);

    void inject(PlansListAdapter object);

    void inject(PlanDetailsFragment object);

    void inject(DomainsDashboardFragment object);

    void inject(DomainSuggestionsFragment object);

    void inject(DomainRegistrationDetailsFragment object);

    void inject(StatsViewsWidget object);

    void inject(StatsAllTimeWidget object);

    void inject(StatsTodayWidget object);

    void inject(StatsMinifiedWidget object);

    void inject(ViewsWidgetListProvider object);

    void inject(AllTimeWidgetListProvider object);

    void inject(AllTimeWidgetBlockListProviderFactory object);

    void inject(TodayWidgetListProvider object);

    void inject(TodayWidgetBlockListProviderFactory object);

    void inject(StatsListFragment object);

    void inject(DomainRegistrationActivity object);

    void inject(EditPostPublishSettingsFragment object);

    void inject(PostDatePickerDialogFragment object);

    void inject(PostTimePickerDialogFragment object);

    void inject(PostNotificationScheduleTimeDialogFragment object);

    void inject(PublishNotificationReceiver object);

    void inject(MainBottomSheetFragment object);

    void inject(ModalLayoutPickerFragment object);

    void inject(SubfilterBottomSheetFragment object);

    void inject(AddContentAdapter object);

    void inject(LayoutsAdapter object);

    void inject(PageParentSearchFragment object);

    void inject(PrepublishingBottomSheetFragment object);

    void inject(PrepublishingHomeFragment object);

    void inject(PrepublishingHomeAdapter object);

    void inject(PrepublishingTagsFragment object);

    void inject(PostSettingsTagsFragment object);

    void inject(PrepublishingPublishSettingsFragment object);

    void inject(AppSettingsActivity object);

    void inject(FeatureAnnouncementDialogFragment object);

    void inject(FeatureAnnouncementListAdapter object);

    void inject(StoryComposerActivity object);

    void inject(StoriesIntroDialogFragment object);

    void inject(ReaderDiscoverFragment object);

    void inject(ReaderSearchActivity object);

    void inject(ReaderInterestsFragment object);

    void inject(HomepageSettingsDialog object);

    void inject(CrashLogging object);

    void inject(AztecVideoLoader object);

    void inject(PhotoPickerFragment object);

    void inject(ReaderDiscoverLogic object);

    void inject(PostListCreateMenuFragment object);

    void inject(ReaderDiscoverJobService object);

    void inject(ReaderDiscoverService object);

    void inject(SuggestionActivity object);

    void inject(MediaPickerActivity object);

    void inject(MediaPickerFragment object);

    void inject(MediaUploadReadyProcessor object);

    void inject(PrepublishingCategoriesFragment object);

    void inject(PrepublishingAddCategoryFragment object);

    void inject(ActivityLogTypeFilterFragment object);

    void inject(MySiteFragment object);

    void inject(MySiteTabFragment object);

    void inject(BackupDownloadActivity object);

    void inject(RestoreActivity object);

    void inject(DynamicCardMenuFragment object);

    void inject(BackupDownloadFragment object);

    void inject(RestoreFragment object);

    void inject(EngagedPeopleListFragment object);

    void inject(SiteSettingsTimezoneBottomSheet object);

    void inject(UserProfileBottomSheetFragment object);

    void inject(EngagedPeopleListActivity object);

    void inject(UnifiedCommentsActivity object);

    void inject(UnifiedCommentListFragment object);

    void inject(UnifiedCommentListAdapter object);

    void inject(UnifiedCommentsEditFragment object);

    void inject(EditCancelDialogFragment object);

    void inject(BloggingReminderBottomSheetFragment object);

    void inject(LocalePickerBottomSheet object);

    void inject(CategoriesListFragment object);

    void inject(CategoryDetailFragment object);

    void inject(LayoutPreviewFragment object);

    void inject(DesignPreviewFragment object);

    void inject(QuickStartPromptDialogFragment object);

    void inject(BloggingReminderTimePicker object);

    void inject(DebugCookiesFragment object);

    void inject(DomainRegistrationResultFragment object);

    void inject(CommentNotificationsBottomSheetFragment object);

    void inject(UnifiedCommentsDetailsActivity object);

    void inject(UnifiedCommentDetailsFragment object);

    void inject(UnifiedAboutActivity object);

    void inject(CommentSnippetAdapter object);

    void inject(BloggingPromptsOnboardingDialogFragment object);

    void inject(DismissNotificationReceiver object);

    void inject(StatsWeekWidget object);

    void inject(WeekViewsWidgetListProvider object);

    void inject(WeekWidgetBlockListProviderFactory object);
}
