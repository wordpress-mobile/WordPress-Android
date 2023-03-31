package org.wordpress.android.modules;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import com.tenor.android.core.network.ApiClient;
import com.tenor.android.core.network.ApiService;
import com.tenor.android.core.network.IApiClient;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStepsProvider;
import org.wordpress.android.ui.jetpack.restore.RestoreStep;
import org.wordpress.android.ui.jetpack.restore.RestoreStepsProvider;
import org.wordpress.android.ui.mediapicker.loader.TenorGifClient;
import org.wordpress.android.ui.sitecreation.SiteCreationStep;
import org.wordpress.android.ui.sitecreation.SiteCreationStepsProvider;
import org.wordpress.android.util.wizard.WizardManager;
import org.wordpress.android.viewmodel.helpers.ConnectionStatus;
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module(includes = AndroidInjectionModule.class)
public abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    abstract Context bindContext(Application application);

    @Provides
    public static SharedPreferences provideSharedPrefs(@ApplicationContext Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

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
