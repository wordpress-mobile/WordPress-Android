package org.wordpress.android.modules;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.ui.JetpackRemoteInstallViewModel;
import org.wordpress.android.ui.accounts.LoginEpilogueViewModel;
import org.wordpress.android.ui.accounts.LoginViewModel;
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel;
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingViewModel;
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel;
import org.wordpress.android.ui.comments.unified.UnifiedCommentActivityViewModel;
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel;
import org.wordpress.android.ui.comments.unified.UnifiedCommentsEditViewModel;
import org.wordpress.android.ui.debug.DebugSettingsViewModel;
import org.wordpress.android.ui.debug.cookies.DebugCookiesViewModel;
import org.wordpress.android.ui.domains.DomainRegistrationDetailsViewModel;
import org.wordpress.android.ui.domains.DomainRegistrationMainViewModel;
import org.wordpress.android.ui.domains.DomainSuggestionsViewModel;
import org.wordpress.android.ui.domains.DomainsDashboardViewModel;
import org.wordpress.android.ui.engagement.EngagedPeopleListViewModel;
import org.wordpress.android.ui.engagement.UserProfileViewModel;
import org.wordpress.android.ui.featureintroduction.FeatureIntroductionViewModel;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel;
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel;
import org.wordpress.android.ui.jetpack.scan.ScanViewModel;
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel;
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryListViewModel;
import org.wordpress.android.ui.jetpack.scan.history.ScanHistoryViewModel;
import org.wordpress.android.ui.mediapicker.MediaPickerViewModel;
import org.wordpress.android.ui.mysite.MySiteViewModel;
import org.wordpress.android.ui.mysite.dynamiccards.DynamicCardMenuViewModel;
import org.wordpress.android.ui.people.PeopleInviteViewModel;
import org.wordpress.android.ui.photopicker.PhotoPickerViewModel;
import org.wordpress.android.ui.plans.PlansViewModel;
import org.wordpress.android.ui.posts.BasicDialogViewModel;
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel;
import org.wordpress.android.ui.posts.EditorBloggingPromptsViewModel;
import org.wordpress.android.ui.posts.PostListMainViewModel;
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel;
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel;
import org.wordpress.android.ui.posts.PrepublishingHomeViewModel;
import org.wordpress.android.ui.posts.PrepublishingTagsViewModel;
import org.wordpress.android.ui.posts.PrepublishingViewModel;
import org.wordpress.android.ui.posts.editor.StorePostViewModel;
import org.wordpress.android.ui.posts.prepublishing.PrepublishingPublishSettingsViewModel;
import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel;
import org.wordpress.android.ui.prefs.categories.detail.CategoryDetailViewModel;
import org.wordpress.android.ui.prefs.categories.list.CategoriesListViewModel;
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsViewModel;
import org.wordpress.android.ui.prefs.language.LocalePickerViewModel;
import org.wordpress.android.ui.prefs.timezone.SiteSettingsTimezoneViewModel;
import org.wordpress.android.ui.reader.ReaderCommentListViewModel;
import org.wordpress.android.ui.reader.discover.ReaderDiscoverViewModel;
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel;
import org.wordpress.android.ui.reader.subfilter.SubFilterViewModel;
import org.wordpress.android.ui.reader.viewmodels.ConversationNotificationsViewModel;
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel;
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel;
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel;
import org.wordpress.android.ui.stats.refresh.lists.DaysListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.InsightsDetailListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.InsightsListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.MonthsListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.TotalCommentsDetailListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.TotalFollowersDetailListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.TotalLikesDetailListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.WeeksListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.YearsListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.detail.DetailListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailViewModel;
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementViewModel;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsColorSelectionViewModel;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsDataTypeSelectionViewModel;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsSiteSelectionViewModel;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureViewModel;
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.StatsMinifiedWidgetConfigureViewModel;
import org.wordpress.android.ui.stories.StoryComposerViewModel;
import org.wordpress.android.ui.stories.intro.StoriesIntroViewModel;
import org.wordpress.android.ui.suggestion.SuggestionViewModel;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel;
import org.wordpress.android.viewmodel.ViewModelFactory;
import org.wordpress.android.viewmodel.ViewModelKey;
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel;
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel;
import org.wordpress.android.viewmodel.history.HistoryViewModel;
import org.wordpress.android.viewmodel.main.SitePickerViewModel;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel;
import org.wordpress.android.viewmodel.mlp.ModalLayoutPickerViewModel;
import org.wordpress.android.viewmodel.pages.PageListViewModel;
import org.wordpress.android.viewmodel.pages.PageParentSearchViewModel;
import org.wordpress.android.viewmodel.pages.PageParentViewModel;
import org.wordpress.android.viewmodel.pages.PagesViewModel;
import org.wordpress.android.viewmodel.pages.SearchListViewModel;
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.posts.PostListCreateMenuViewModel;
import org.wordpress.android.viewmodel.posts.PostListViewModel;
import org.wordpress.android.viewmodel.quickstart.QuickStartViewModel;
import org.wordpress.android.viewmodel.storage.StorageUtilsViewModel;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dagger.multibindings.IntoMap;

@InstallIn(SingletonComponent.class)
@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(PluginBrowserViewModel.class)
    abstract ViewModel pluginBrowserViewModel(PluginBrowserViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ActivityLogViewModel.class)
    abstract ViewModel activityLogViewModel(ActivityLogViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PagesViewModel.class)
    abstract ViewModel pagesViewModel(PagesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SearchListViewModel.class)
    abstract ViewModel searchListViewModel(SearchListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PageListViewModel.class)
    abstract ViewModel pageListViewModel(PageListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PageParentViewModel.class)
    abstract ViewModel pageParentViewModel(PageParentViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ReaderPostListViewModel.class)
    abstract ViewModel readerPostListViewModel(ReaderPostListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SubFilterViewModel.class)
    abstract ViewModel readerSubFilterViewModel(SubFilterViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SubfilterPageViewModel.class)
    abstract ViewModel subfilterPageViewModel(SubfilterPageViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(JetpackRemoteInstallViewModel.class)
    abstract ViewModel jetpackRemoteInstallViewModel(JetpackRemoteInstallViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(QuickStartViewModel.class)
    abstract ViewModel quickStartViewModel(QuickStartViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(InsightsListViewModel.class)
    abstract ViewModel insightsTabViewModel(InsightsListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DaysListViewModel.class)
    abstract ViewModel daysTabViewModel(DaysListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(WeeksListViewModel.class)
    abstract ViewModel weeksTabViewModel(WeeksListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(MonthsListViewModel.class)
    abstract ViewModel monthsTabViewModel(MonthsListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(YearsListViewModel.class)
    abstract ViewModel yearsTabViewModel(YearsListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsDetailViewModel.class)
    abstract ViewModel statsDetailViewModel(StatsDetailViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(InsightsDetailListViewModel.class)
    abstract ViewModel insightsDetailListViewModel(InsightsDetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DetailListViewModel.class)
    abstract ViewModel detailListViewModel(DetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsWidgetConfigureViewModel.class)
    abstract ViewModel statsViewsWidgetViewModel(StatsWidgetConfigureViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsSiteSelectionViewModel.class)
    abstract ViewModel statsSiteSelectionViewModel(StatsSiteSelectionViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsDataTypeSelectionViewModel.class)
    abstract ViewModel statsDataTypeSelectionViewModel(StatsDataTypeSelectionViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsMinifiedWidgetConfigureViewModel.class)
    abstract ViewModel statsMinifiedWidgetViewModel(StatsMinifiedWidgetConfigureViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsColorSelectionViewModel.class)
    abstract ViewModel statsColorSelectionViewModel(StatsColorSelectionViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(InsightsManagementViewModel.class)
    abstract ViewModel insightsManagementViewModel(InsightsManagementViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(HistoryViewModel.class)
    abstract ViewModel historyViewModel(HistoryViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PostListViewModel.class)
    abstract ViewModel postListViewModel(PostListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PostListMainViewModel.class)
    abstract ViewModel postListMainViewModel(PostListMainViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PlansViewModel.class)
    abstract ViewModel plansViewModel(PlansViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DomainsDashboardViewModel.class)
    abstract ViewModel domainsDashboardViewModel(DomainsDashboardViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DomainSuggestionsViewModel.class)
    abstract ViewModel domainSuggestionsViewModel(DomainSuggestionsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(WPWebViewViewModel.class)
    abstract ViewModel wpWebViewViewModel(WPWebViewViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DomainRegistrationDetailsViewModel.class)
    abstract ViewModel domainRegistrationDetailsViewModel(DomainRegistrationDetailsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DomainRegistrationMainViewModel.class)
    abstract ViewModel domainRegistrationMainViewModel(DomainRegistrationMainViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StorePostViewModel.class)
    abstract ViewModel storePostViewModel(StorePostViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(EditPostPublishSettingsViewModel.class)
    abstract ViewModel editPostPublishedSettingsViewModel(EditPostPublishSettingsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ReaderCommentListViewModel.class)
    abstract ViewModel readerCommentListViewModel(ReaderCommentListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(WPMainActivityViewModel.class)
    abstract ViewModel wpMainActivityViewModel(WPMainActivityViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ModalLayoutPickerViewModel.class)
    abstract ViewModel mlpViewModel(ModalLayoutPickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PostSignupInterstitialViewModel.class)
    abstract ViewModel postSignupInterstitialViewModel(PostSignupInterstitialViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PageParentSearchViewModel.class)
    abstract ViewModel pageParentSearchViewModel(PageParentSearchViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(FeatureAnnouncementViewModel.class)
    abstract ViewModel featureAnnouncementViewModel(FeatureAnnouncementViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SitePickerViewModel.class)
    abstract ViewModel sitePickerViewModel(SitePickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ReaderViewModel.class)
    abstract ViewModel readerParentPostListViewModel(ReaderViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ReaderDiscoverViewModel.class)
    abstract ViewModel readerDiscoverViewModel(ReaderDiscoverViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ReaderInterestsViewModel.class)
    abstract ViewModel readerInterestsViewModel(ReaderInterestsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(HomepageSettingsViewModel.class)
    abstract ViewModel homepageSettingsDialogViewModel(HomepageSettingsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingViewModel.class)
    abstract ViewModel prepublishingViewModel(PrepublishingViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingHomeViewModel.class)
    abstract ViewModel prepublishingOptionsViewModel(PrepublishingHomeViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingTagsViewModel.class)
    abstract ViewModel prepublishingTagsViewModel(PrepublishingTagsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingPublishSettingsViewModel.class)
    abstract ViewModel prepublishingPublishSettingsViewModel(PrepublishingPublishSettingsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PostListCreateMenuViewModel.class)
    abstract ViewModel postListCreateMenuViewModel(PostListCreateMenuViewModel postListCreateMenuViewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StoryComposerViewModel.class)
    abstract ViewModel storyComposerViewModel(StoryComposerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StoriesIntroViewModel.class)
    abstract ViewModel storiesIntroViewModel(StoriesIntroViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PhotoPickerViewModel.class)
    abstract ViewModel photoPickerViewModel(PhotoPickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(MediaPickerViewModel.class)
    abstract ViewModel mediaPickerViewModel(MediaPickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DebugSettingsViewModel.class)
    abstract ViewModel debugSettingsViewModel(DebugSettingsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingCategoriesViewModel.class)
    abstract ViewModel prepublishingCategoriesViewModel(PrepublishingCategoriesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PrepublishingAddCategoryViewModel.class)
    abstract ViewModel prepublishingAddCategoryViewModel(PrepublishingAddCategoryViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SuggestionViewModel.class)
    abstract ViewModel suggestionViewModel(SuggestionViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ActivityLogTypeFilterViewModel.class)
    abstract ViewModel activityLogTypeFilterViewModel(ActivityLogTypeFilterViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ScanViewModel.class)
    abstract ViewModel scanViewModel(ScanViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ScanHistoryViewModel.class)
    abstract ViewModel scanHistoryViewModel(ScanHistoryViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ScanHistoryListViewModel.class)
    abstract ViewModel scanHistoryListViewModel(ScanHistoryListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ThreatDetailsViewModel.class)
    abstract ViewModel threatDetailsViewModel(ThreatDetailsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(MySiteViewModel.class)
    abstract ViewModel mySiteViewModel(MySiteViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(BasicDialogViewModel.class)
    abstract ViewModel basicDialogViewModel(BasicDialogViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(BackupDownloadViewModel.class)
    abstract ViewModel backupDownloadViewModel(BackupDownloadViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(RestoreViewModel.class)
    abstract ViewModel restoreViewModel(RestoreViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DynamicCardMenuViewModel.class)
    abstract ViewModel dynamicCardMenuViewModel(DynamicCardMenuViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PeopleInviteViewModel.class)
    abstract ViewModel peopleInviteViewModel(PeopleInviteViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(EngagedPeopleListViewModel.class)
    abstract ViewModel engagedPeopleListViewModel(EngagedPeopleListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UserProfileViewModel.class)
    abstract ViewModel userProfileViewModel(UserProfileViewModel viewModel);

    @Binds
    abstract ViewModelProvider.Factory provideViewModelFactory(ViewModelFactory viewModelFactory);

    @Binds
    @IntoMap
    @ViewModelKey(SiteSettingsTimezoneViewModel.class)
    abstract ViewModel siteSettingsTimezoneViewModel(SiteSettingsTimezoneViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(LoginEpilogueViewModel.class)
    abstract ViewModel loginEpilogueViewModel(LoginEpilogueViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(LoginViewModel.class)
    abstract ViewModel loginViewModel(LoginViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StorageUtilsViewModel.class)
    abstract ViewModel storageUtilsViewModel(StorageUtilsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UnifiedCommentListViewModel.class)
    abstract ViewModel unifiedCommentListViewModel(UnifiedCommentListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(BloggingRemindersViewModel.class)
    abstract ViewModel bloggingRemindersViewModel(BloggingRemindersViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(LocalePickerViewModel.class)
    abstract ViewModel localePickerViewModel(LocalePickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UnifiedCommentActivityViewModel.class)
    abstract ViewModel unifiedCommentActivityViewModel(UnifiedCommentActivityViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(UnifiedCommentsEditViewModel.class)
    abstract ViewModel unifiedCommentsEditViewModel(UnifiedCommentsEditViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(DebugCookiesViewModel.class)
    abstract ViewModel debugCookiesViewModel(DebugCookiesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ConversationNotificationsViewModel.class)
    abstract ViewModel conversationNotificationsViewModel(ConversationNotificationsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(BloggingPromptsOnboardingViewModel.class)
    abstract ViewModel bloggingPromptsOnboardingViewModel(BloggingPromptsOnboardingViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(EditorBloggingPromptsViewModel.class)
    abstract ViewModel editorBloggingPromptsViewModel(EditorBloggingPromptsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(TotalLikesDetailListViewModel.class)
    abstract ViewModel totalLikesDetailListViewModel(TotalLikesDetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(FeatureIntroductionViewModel.class)
    abstract ViewModel featureIntroductionViewModel(FeatureIntroductionViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(TotalCommentsDetailListViewModel.class)
    abstract ViewModel totalCommentsDetailListViewModel(TotalCommentsDetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(TotalFollowersDetailListViewModel.class)
    abstract ViewModel totalFollowersDetailListViewModel(TotalFollowersDetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(AccountSettingsViewModel.class)
    abstract ViewModel accountSettingsViewModel(AccountSettingsViewModel viewModel);
}
