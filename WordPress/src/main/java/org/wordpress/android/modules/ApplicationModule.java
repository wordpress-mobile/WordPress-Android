package org.wordpress.android.modules;

import android.app.Application;
import android.content.Context;

import androidx.lifecycle.LiveData;

import com.tenor.android.core.network.ApiClient;
import com.tenor.android.core.network.ApiService;
import com.tenor.android.core.network.IApiClient;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.ui.CommentFullScreenDialogFragment;
import org.wordpress.android.ui.accounts.signup.SettingsUsernameChangerFragment;
import org.wordpress.android.ui.accounts.signup.UsernameChangerFullScreenDialogFragment;
import org.wordpress.android.ui.debug.DebugSettingsFragment;
import org.wordpress.android.ui.domains.DomainRegistrationDetailsFragment.CountryPickerDialogFragment;
import org.wordpress.android.ui.domains.DomainRegistrationDetailsFragment.StatePickerDialogFragment;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStepsProvider;
import org.wordpress.android.ui.jetpack.restore.RestoreStep;
import org.wordpress.android.ui.jetpack.restore.RestoreStepsProvider;
import org.wordpress.android.ui.mediapicker.loader.TenorGifClient;
import org.wordpress.android.ui.posts.BasicDialog;
import org.wordpress.android.ui.reader.ReaderPostWebViewCachingFragment;
import org.wordpress.android.ui.reader.subfilter.SubfilterPageFragment;
import org.wordpress.android.ui.sitecreation.SiteCreationStep;
import org.wordpress.android.ui.sitecreation.SiteCreationStepsProvider;
import org.wordpress.android.ui.stats.refresh.StatsViewAllFragment;
import org.wordpress.android.ui.stats.refresh.lists.StatsListFragment;
import org.wordpress.android.ui.stats.refresh.lists.detail.StatsDetailFragment;
import org.wordpress.android.ui.stats.refresh.lists.sections.insights.management.InsightsManagementFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetColorSelectionDialogFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetConfigureFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetDataTypeSelectionDialogFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.configuration.StatsWidgetSiteSelectionDialogFragment;
import org.wordpress.android.ui.stats.refresh.lists.widget.minified.StatsMinifiedWidgetConfigureFragment;
import org.wordpress.android.util.wizard.WizardManager;
import org.wordpress.android.viewmodel.helpers.ConnectionStatus;
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.android.ContributesAndroidInjector;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module(includes = AndroidInjectionModule.class)
public abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    abstract Context bindContext(Application application);

    @ContributesAndroidInjector
    abstract StatsListFragment contributeStatListFragment();

    @ContributesAndroidInjector
    abstract StatsViewAllFragment contributeStatsViewAllFragment();

    @ContributesAndroidInjector
    abstract InsightsManagementFragment contributeInsightsManagementFragment();

    @ContributesAndroidInjector
    abstract StatsDetailFragment contributeStatsDetailFragment();

    @ContributesAndroidInjector
    abstract CountryPickerDialogFragment contributeCountryPickerDialogFragment();

    @ContributesAndroidInjector
    abstract StatePickerDialogFragment contributeCStatePickerDialogFragment();

    @ContributesAndroidInjector
    abstract StatsWidgetConfigureFragment contributeStatsViewsWidgetConfigureFragment();

    @ContributesAndroidInjector
    abstract StatsWidgetSiteSelectionDialogFragment contributeSiteSelectionDialogFragment();

    @ContributesAndroidInjector
    abstract StatsWidgetColorSelectionDialogFragment contributeViewModeSelectionDialogFragment();

    @ContributesAndroidInjector
    abstract StatsMinifiedWidgetConfigureFragment contributeStatsMinifiedWidgetConfigureFragment();

    @ContributesAndroidInjector
    abstract StatsWidgetDataTypeSelectionDialogFragment contributeDataTypeSelectionDialogFragment();

    @ContributesAndroidInjector
    abstract CommentFullScreenDialogFragment contributecommentFullScreenDialogFragment();

    @ContributesAndroidInjector
    abstract UsernameChangerFullScreenDialogFragment contributeUsernameChangerFullScreenDialogFragment();

    @ContributesAndroidInjector
    abstract SettingsUsernameChangerFragment contributeSettingsUsernameChangerFragment();

    @ContributesAndroidInjector
    abstract ReaderPostWebViewCachingFragment contributeReaderPostWebViewCachingFragment();

    @ContributesAndroidInjector
    abstract SubfilterPageFragment contributeSubfilterPageFragment();

    @ContributesAndroidInjector
    abstract DebugSettingsFragment contributeDebugSettingsFragment();

    @ContributesAndroidInjector
    abstract BasicDialog contributeBasicDialog();

    @Provides
    public static WizardManager<SiteCreationStep> provideWizardManager(
            SiteCreationStepsProvider stepsProvider) {
        return new WizardManager<>(stepsProvider.getSteps());
    }

    @Provides
    static LiveData<ConnectionStatus> provideConnectionStatusLiveData(@ApplicationContext Context context) {
        return new ConnectionStatusLiveData.Factory(context).create();
    }

    @Provides
    static TenorGifClient provideTenorGifClient(@ApplicationContext Context context) {
        ApiService.IBuilder<IApiClient> builder = new ApiService.Builder<>(context, IApiClient.class);
        builder.apiKey(BuildConfig.TENOR_API_KEY);
        ApiClient.init(context, builder);
        return new TenorGifClient(context, ApiClient.getInstance(context));
    }

    @Provides
    public static WizardManager<BackupDownloadStep> provideBackupDownloadWizardManager(
            BackupDownloadStepsProvider stepsProvider) {
        return new WizardManager<>(stepsProvider.getSteps());
    }

    @Provides
    public static WizardManager<RestoreStep> provideRestoreWizardManager(
            RestoreStepsProvider stepsProvider) {
        return new WizardManager<>(stepsProvider.getSteps());
    }
}
