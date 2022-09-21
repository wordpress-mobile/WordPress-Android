package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.math.exp
import kotlin.math.ln

// This factor is used to convert the raw values emitted from device sensor to an appropriate scale for the consuming
// composables.
private const val ACCELERATION_FACTOR = -0.1f

// The maximum velocity (in either direction)
private const val MAXIMUM_VELOCITY = 0.3f

// The velocity decay factor (i.e. 1/4th velocity after 1 second)
private val VELOCITY_DECAY = -ln(4f)

// An additional acceleration applied to make the text scroll when the device is flat on a table
private const val DRIFT = -0.05f

@HiltViewModel
class LoginPrologueRevampedViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private var acceleration = 0f
    private var velocity = 0f
    var position = 0f

    /**
     * This function updates the physics model for the interactive animation by applying the elapsed time (in seconds)
     * to update the velocity and position.
     *
     *  * Velocity is constrained so that it does not fall below -MAXIMUM_VELOCITY and does not exceed MAXIMUM_VELOCITY.
     *  * Position is constrained so that it always falls between 0 and 1, and represents the relative vertical offset
     *  in terms of the height of the repeated child composable.
     *
     *  @param elapsed the elapsed time (in seconds) since the last frame
     */
    fun updateForFrame(elapsed: Float) {
        // Update the velocity, (decayed and clamped to the maximum)
        velocity = (velocity * exp(elapsed * VELOCITY_DECAY) + elapsed * acceleration)
                .coerceIn(-MAXIMUM_VELOCITY, MAXIMUM_VELOCITY)
        // Update the position, modulo 1 (ensuring a value greater or equal to 0, and less than 1)
        position = ((position + elapsed * velocity) % 1 + 1) % 1

        _positionData.postValue(position)
    }

    /** This LiveData responds to accelerometer data from the y-axis of the device and emits updated position data. */
    private val _positionData = object : MutableLiveData<Float>(), SensorEventListener {
        private val sensorManager
            get() = appContext.getSystemService(Context.SENSOR_SERVICE)
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
                acceleration = it.values[1] * ACCELERATION_FACTOR + DRIFT
                postValue(position)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    val positionData: LiveData<Float> = _positionData
}
