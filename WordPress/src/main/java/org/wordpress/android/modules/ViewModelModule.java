package org.wordpress.android.modules;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.ui.JetpackRemoteInstallViewModel;
import org.wordpress.android.ui.domains.DomainRegistrationMainViewModel;
import org.wordpress.android.ui.plans.PlansViewModel;
import org.wordpress.android.ui.posts.EditPostPublishSettingsViewModel;
import org.wordpress.android.ui.posts.PostListMainViewModel;
import org.wordpress.android.ui.posts.editor.StorePostViewModel;
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsViewModel;
import org.wordpress.android.ui.reader.ReaderCommentListViewModel;
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel;
import org.wordpress.android.ui.reader.viewmodels.SubfilterPageViewModel;
import org.wordpress.android.ui.sitecreation.SiteCreationMainVM;
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel;
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel;
import org.wordpress.android.ui.sitecreation.segments.SiteCreationSegmentsViewModel;
import org.wordpress.android.ui.stats.refresh.StatsViewModel;
import org.wordpress.android.ui.stats.refresh.lists.DaysListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.InsightsListViewModel;
import org.wordpress.android.ui.stats.refresh.lists.MonthsListViewModel;
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
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementViewModel;
import org.wordpress.android.viewmodel.ViewModelFactory;
import org.wordpress.android.viewmodel.ViewModelKey;
import org.wordpress.android.viewmodel.accounts.PostSignupInterstitialViewModel;
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel;
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel;
import org.wordpress.android.viewmodel.domains.DomainRegistrationDetailsViewModel;
import org.wordpress.android.viewmodel.domains.DomainSuggestionsViewModel;
import org.wordpress.android.viewmodel.gif.GifPickerViewModel;
import org.wordpress.android.viewmodel.history.HistoryViewModel;
import org.wordpress.android.viewmodel.main.SitePickerViewModel;
import org.wordpress.android.viewmodel.main.WPMainActivityViewModel;
import org.wordpress.android.viewmodel.pages.PageListViewModel;
import org.wordpress.android.viewmodel.pages.PageParentSearchViewModel;
import org.wordpress.android.viewmodel.pages.PageParentViewModel;
import org.wordpress.android.viewmodel.pages.PagesViewModel;
import org.wordpress.android.viewmodel.pages.SearchListViewModel;
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.posts.PostListViewModel;
import org.wordpress.android.viewmodel.quickstart.QuickStartViewModel;
import org.wordpress.android.viewmodel.wpwebview.WPWebViewViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

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
    @ViewModelKey(ActivityLogDetailViewModel.class)
    abstract ViewModel activityLogDetailViewModel(ActivityLogDetailViewModel viewModel);

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
    @ViewModelKey(DetailListViewModel.class)
    abstract ViewModel detailListViewModel(DetailListViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(StatsViewModel.class)
    abstract ViewModel statsViewModel(StatsViewModel viewModel);

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
    @ViewModelKey(SiteCreationSegmentsViewModel.class)
    abstract ViewModel siteCreationSegmentsViewModel(SiteCreationSegmentsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SiteCreationDomainsViewModel.class)
    abstract ViewModel siteCreationDomainsViewModel(SiteCreationDomainsViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SiteCreationMainVM.class)
    abstract ViewModel siteCreationMainVM(SiteCreationMainVM viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(SitePreviewViewModel.class)
    abstract ViewModel newSitePreviewViewModel(SitePreviewViewModel viewModel);

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
    @ViewModelKey(GifPickerViewModel.class)
    abstract ViewModel gifPickerViewModel(GifPickerViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PlansViewModel.class)
    abstract ViewModel plansViewModel(PlansViewModel viewModel);

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
    @ViewModelKey(HomepageSettingsViewModel.class)
    abstract ViewModel homepageSettingsDialogViewModel(HomepageSettingsViewModel viewModel);

    @Binds
    abstract ViewModelProvider.Factory provideViewModelFactory(ViewModelFactory viewModelFactory);
}
