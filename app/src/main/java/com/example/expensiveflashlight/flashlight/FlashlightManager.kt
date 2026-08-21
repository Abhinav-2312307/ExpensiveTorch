package com.example.expensiveflashlight.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FlashlightManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val cameraId: String? = findCameraWithFlash()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isOn = MutableStateFlow(false)
    val isOn: StateFlow<Boolean> = _isOn.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private var countdownJob: Job? = null
    private var specialModeJob: Job? = null

    // --- Torch helpers ---

    private fun findCameraWithFlash(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun setTorch(on: Boolean) {
        val id = cameraId ?: return
        try {
            cameraManager.setTorchMode(id, on)
            _isOn.value = on
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Public API ---

    /**
     * Turns the flashlight on indefinitely (for premium users).
     */
    fun turnOn() {
        cancelCountdown()
        stopSpecialMode()
        setTorch(true)
    }

    /**
     * Turns the flashlight on for the given number of [seconds], then automatically turns off.
     * The [remainingSeconds] StateFlow is updated every second.
     */
    fun turnOnForDuration(seconds: Int) {
        cancelCountdown()
        stopSpecialMode()
        setTorch(true)
        _remainingSeconds.value = seconds

        countdownJob = scope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _remainingSeconds.value = remaining
            }
            if (isActive) {
                setTorch(false)
                _remainingSeconds.value = 0
            }
        }
    }

    /**
     * Turns the flashlight off and cancels any running countdown timer or special mode.
     */
    fun turnOff() {
        cancelCountdown()
        stopSpecialMode()
        setTorch(false)
        _remainingSeconds.value = 0
    }

    /**
     * Starts a rapid strobe effect — the flashlight toggles on/off every 100 ms.
     * Any previous special mode or countdown is cancelled first.
     */
    fun startStrobe() {
        cancelCountdown()
        stopSpecialMode()
        _remainingSeconds.value = 0

        specialModeJob = scope.launch {
            while (isActive) {
                setTorch(true)
                delay(100L)
                setTorch(false)
                delay(100L)
            }
        }
    }

    /**
     * Starts an SOS pattern (Morse code: ... --- ...) that repeats until stopped.
     *
     * Timing conventions (ITU-R M.1677-1):
     *  • Dot  = 200 ms on
     *  • Dash = 600 ms on
     *  • Intra-character gap = 200 ms off
     *  • Inter-character gap = 600 ms off
     *  • Word gap (between repetitions) = 1400 ms off
     */
    fun startSOS() {
        cancelCountdown()
        stopSpecialMode()
        _remainingSeconds.value = 0

        specialModeJob = scope.launch {
            while (isActive) {
                // S: ...
                repeat(3) {
                    setTorch(true)
                    delay(200L)
                    setTorch(false)
                    delay(200L)
                }
                delay(400L) // extra gap to reach 600 ms inter-character

                // O: ---
                repeat(3) {
                    setTorch(true)
                    delay(600L)
                    setTorch(false)
                    delay(200L)
                }
                delay(400L)

                // S: ...
                repeat(3) {
                    setTorch(true)
                    delay(200L)
                    setTorch(false)
                    delay(200L)
                }

                // Word gap before repeating
                delay(1400L)
            }
        }
    }

    /**
     * Stops any running strobe or SOS pattern and turns the torch off.
     */
    fun stopSpecialMode() {
        specialModeJob?.cancel()
        specialModeJob = null
        // Ensure the torch is left in the off state after a special mode ends.
        try {
            cameraId?.let { cameraManager.setTorchMode(it, false) }
        } catch (_: Exception) { }
        _isOn.value = false
    }

    /**
     * Releases all resources (cancels the coroutine scope).
     * Call this from your ViewModel's onCleared() or Activity's onDestroy().
     */
    fun cleanup() {
        turnOff()
        scope.cancel()
    }

    // --- Internal helpers ---

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }
}
