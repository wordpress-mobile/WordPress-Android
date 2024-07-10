package org.wordpress.android.modules;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.tenor.android.core.network.ApiClient;
import com.tenor.android.core.network.ApiService;
import com.tenor.android.core.network.IApiClient;

import org.wordpress.android.BuildConfig;
import org.wordpress.android.inappupdate.IInAppUpdateManager;
import org.wordpress.android.inappupdate.InAppUpdateAnalyticsTracker;
import org.wordpress.android.inappupdate.InAppUpdateManagerImpl;
import org.wordpress.android.inappupdate.InAppUpdateManagerNoop;
import org.wordpress.android.ui.ActivityNavigator;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStep;
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadStepsProvider;
import org.wordpress.android.ui.jetpack.restore.RestoreStep;
import org.wordpress.android.ui.jetpack.restore.RestoreStepsProvider;
import org.wordpress.android.ui.mediapicker.loader.TenorGifClient;
import org.wordpress.android.ui.sitecreation.SiteCreationStep;
import org.wordpress.android.ui.sitecreation.SiteCreationStepsProvider;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.audio.AudioRecorder;
import org.wordpress.android.util.audio.IAudioRecorder;
import org.wordpress.android.util.audio.RecordingStrategy;
import org.wordpress.android.util.audio.RecordingStrategy.VoiceToContentRecordingStrategy;
import org.wordpress.android.util.audio.VoiceToContentStrategy;
import org.wordpress.android.util.config.InAppUpdatesFeatureConfig;
import org.wordpress.android.util.config.RemoteConfigWrapper;
import org.wordpress.android.util.wizard.WizardManager;
import org.wordpress.android.viewmodel.helpers.ConnectionStatus;
import org.wordpress.android.viewmodel.helpers.ConnectionStatusLiveData;

import javax.inject.Named;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import kotlinx.coroutines.CoroutineScope;
import static org.wordpress.android.modules.ThreadModuleKt.APPLICATION_SCOPE;

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

    @Provides
    public static AppUpdateManager provideAppUpdateManager(@ApplicationContext Context context) {
        return AppUpdateManagerFactory.create(context);
    }

    @Provides
    public static IInAppUpdateManager provideInAppUpdateManager(
            @ApplicationContext Context context,
            @Named(APPLICATION_SCOPE) CoroutineScope appScope,
            AppUpdateManager appUpdateManager,
            RemoteConfigWrapper remoteConfigWrapper,
            BuildConfigWrapper buildConfigWrapper,
            InAppUpdatesFeatureConfig inAppUpdatesFeatureConfig,
            InAppUpdateAnalyticsTracker inAppUpdateAnalyticsTracker
    ) {
        // Check if in-app updates feature is enabled
        return inAppUpdatesFeatureConfig.isEnabled()
                ? new InAppUpdateManagerImpl(
                context,
                appScope,
                appUpdateManager,
                remoteConfigWrapper,
                buildConfigWrapper,
                inAppUpdateAnalyticsTracker,
                System::currentTimeMillis
        )
                : new InAppUpdateManagerNoop();
    }

    @Provides
    public static ActivityNavigator provideActivityNavigator(@ApplicationContext Context context) {
        return new ActivityNavigator();
    }

    @Provides
    public static SensorManager provideSensorManager(@ApplicationContext Context context) {
        return (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    @VoiceToContentStrategy
    @Provides
    public static IAudioRecorder provideAudioRecorder(
            @ApplicationContext Context context,
            @VoiceToContentStrategy RecordingStrategy recordingStrategy
    ) {
        return new AudioRecorder(context, recordingStrategy);
    }

    @VoiceToContentStrategy
    @Provides
    public static RecordingStrategy provideVoiceToContentRecordingStrategy() {
        return new VoiceToContentRecordingStrategy();
    }
}
