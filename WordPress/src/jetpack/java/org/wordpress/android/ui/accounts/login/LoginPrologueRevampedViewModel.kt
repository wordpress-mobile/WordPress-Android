package org.wordpress.android.ui.accounts.login

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.ScopedAndroidViewModel
import javax.inject.Inject
import javax.inject.Named

// This factor is used to convert the raw values emitted from device sensor to an appropriate scale for the consuming
// composables.
private const val ACCERLERATION_FACTOR = -0.1f

class LoginPrologueRevampedViewModel @Inject constructor(
    application: Application,
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
): ScopedAndroidViewModel(application, mainDispatcher) {
    /** This LiveData emits scaled acceleration data from the y-axis of the device. */
    val accelerometerData = object: AccelerometerLiveData() {
        private val sensorManager get() = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE)
                as SensorManager
        override fun onActive() {
            super.onActive()
            val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        override fun onInactive() {
            super.onInactive()
            sensorManager.unregisterListener(this)
        }
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                postValue(it.values[1] * ACCERLERATION_FACTOR)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    abstract class AccelerometerLiveData: LiveData<Float>(), SensorEventListener {
        /** This dummy companion implementation is useful for passing to preview composables. */
        companion object: AccelerometerLiveData() {
            override fun onSensorChanged(event: SensorEvent?) = Unit
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
    }
}
