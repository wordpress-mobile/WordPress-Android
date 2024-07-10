package org.wordpress.android.ui.accounts.login

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.analytics.AnalyticsTracker.Stat.LOGIN_PROLOGUE_VIEWED
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import kotlin.math.PI

/**
 * This factor is used to convert the raw values emitted from device sensor to an appropriate scale for the consuming
 * composables. The range for pitch values is -π/2 to π/2, representing an upright pose and an upside-down pose,
 * respectively. E.g. Setting this factor to 0.2 / (π/2) will scale this output range to -0.2 to 0.2. The resulting
 * product is interpreted in units proportional to the repeating child composable's height per second, i.e. at a
 * velocity of 0.1, it will take 10 seconds for the looping animation to repeat. When applied, the product can also be
 * thought of as a frequency value in Hz.
 */
private const val VELOCITY_FACTOR = (0.2 / (PI / 2)).toFloat()

/**
 * This is the default pitch provided for devices lacking support for the sensors used. This is consumed by the model
 * as a value in radians, but is specified here as -30 degrees for convenience. This represents a device pose that is
 * slightly upright from flat, approximating a typical usage pattern, and will ensure that the text is animated at
 * an appropriate velocity when sensors are unavailable.
 */
private const val DEFAULT_PITCH = (-30 * PI / 180).toFloat()

@HiltViewModel
class LoginPrologueRevampedViewModel @Inject constructor(
    private val unifiedLoginTracker: UnifiedLoginTracker,
    analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    sensorManager: SensorManager,
) : ViewModel() {
    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = floatArrayOf(0f, DEFAULT_PITCH, 0f)
    private var position = 0f

    init {
        analyticsTrackerWrapper.track(stat = LOGIN_PROLOGUE_VIEWED)
        unifiedLoginTracker.track(flow = Flow.PROLOGUE, step = Step.PROLOGUE)
    }

    /**
     * This function updates the physics model for the interactive animation by applying the elapsed time (in seconds)
     * to update the velocity and position.
     *
     *  * Velocity is calculated as proportional to the pitch angle
     *  * Position is constrained so that it always falls between 0 and 1, and represents the relative vertical offset
     *  in terms of the height of the repeated child composable.
     *
     *  @param elapsed the elapsed time (in seconds) since the last frame
     */
    fun updateForFrame(elapsed: Float) {
        orientationAngles.let { (_, pitch) ->
            val velocity = pitch * VELOCITY_FACTOR

            // Update the position, modulo 1 (ensuring a value greater or equal to 0, and less than 1)
            position = ((position + elapsed * velocity) % 1 + 1) % 1

            _positionData.postValue(position)
        }
    }

    fun onWpComLoginClicked() {
        unifiedLoginTracker.trackClick(Click.CONTINUE_WITH_WORDPRESS_COM)
    }

    fun onSiteAddressLoginClicked() {
        unifiedLoginTracker.trackClick(Click.LOGIN_WITH_SITE_ADDRESS)
    }

    /**
     * This LiveData responds to orientation data to calculate the pitch of the device. This is then used to update the
     * velocity and position for each frame.
     */
    private val _positionData = object : MutableLiveData<Float>(), SensorEventListener {
        override fun onActive() {
            super.onActive()
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        override fun onInactive() {
            super.onInactive()
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> event.values.copyInto(accelerometerData)
                Sensor.TYPE_MAGNETIC_FIELD -> event.values.copyInto(magnetometerData)
            }
            // Update the orientation angles when sensor data is updated
            SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    val positionData: LiveData<Float> = _positionData
}
