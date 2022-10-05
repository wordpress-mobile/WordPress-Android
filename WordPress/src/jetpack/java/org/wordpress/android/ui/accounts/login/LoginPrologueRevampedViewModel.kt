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
import kotlin.math.PI

// This factor is used to convert the raw values emitted from device sensor to an appropriate scale for the consuming
// composables.
private const val VELOCITY_FACTOR = (0.2 / (PI / 2)).toFloat()

// Default pitch provided for devices lacking support for the sensors used
private const val DEFAULT_PITCH = (-30 * PI / 180).toFloat()

@HiltViewModel
class LoginPrologueRevampedViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
) : ViewModel() {
    private val accelerometerData = FloatArray(3)
    private val magnetometerData = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = floatArrayOf(0f, DEFAULT_PITCH, 0f)
    private var velocity = 0f
    private var position = 0f

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
            velocity = pitch * VELOCITY_FACTOR
        }
        // Update the position, modulo 1 (ensuring a value greater or equal to 0, and less than 1)
        position = ((position + elapsed * velocity) % 1 + 1) % 1

        _positionData.postValue(position)
    }

    /** This LiveData responds to accelerometer data from the y-axis of the device and emits updated position data. */
    private val _positionData = object : MutableLiveData<Float>(), SensorEventListener {
        private val sensorManager
            get() = appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        override fun onActive() {
            super.onActive()
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
                sensorManager.registerListener( this, it, SensorManager.SENSOR_DELAY_GAME)
            }
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also {
                sensorManager.registerListener( this, it, SensorManager.SENSOR_DELAY_GAME)
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
