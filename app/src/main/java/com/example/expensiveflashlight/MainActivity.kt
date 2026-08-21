package com.example.expensiveflashlight

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.example.expensiveflashlight.flashlight.FlashlightManager
import com.example.expensiveflashlight.payment.PaymentManager
import com.example.expensiveflashlight.payment.PremiumManager
import com.example.expensiveflashlight.theme.ExpensiveFlashlightTheme
import com.example.expensiveflashlight.ui.components.FlashlightMode
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultListener {

    private lateinit var flashlightManager: FlashlightManager
    private lateinit var premiumManager: PremiumManager

    // Reactive state for Compose
    private var isFlashlightOn by mutableStateOf(false)
    private var remainingSeconds by mutableIntStateOf(0)
    private var isPremium by mutableStateOf(false)
    private var expiryDate by mutableStateOf<String?>(null)
    private var lastPaymentId by mutableStateOf<String?>(null)
    private var currentMode by mutableStateOf(FlashlightMode.MAX)

    // Track pending payment for pay-per-use
    private var pendingDurationSeconds: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        flashlightManager = FlashlightManager(this)
        premiumManager = PremiumManager(this)

        // Preload Razorpay
        PaymentManager.initializeRazorpay(this)

        // Collect state flows
        lifecycleScope.launch {
            flashlightManager.isOn.collectLatest { isOn ->
                isFlashlightOn = isOn
            }
        }
        lifecycleScope.launch {
            flashlightManager.remainingSeconds.collectLatest { seconds ->
                remainingSeconds = seconds
            }
        }
        lifecycleScope.launch {
            premiumManager.isPremium.collectLatest { premium ->
                isPremium = premium
                expiryDate = premiumManager.getPremiumExpiryDate()
                lastPaymentId = premiumManager.getLastPaymentId()
            }
        }

        enableEdgeToEdge()
        setContent {
            ExpensiveFlashlightTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0A0A0A)
                ) {
                    MainNavigation(
                        isPremium = isPremium,
                        isFlashlightOn = isFlashlightOn,
                        remainingSeconds = remainingSeconds,
                        expiryDate = expiryDate,
                        lastPaymentId = lastPaymentId,
                        onToggleFlashlight = { toggleFlashlight() },
                        onPayForDuration = { amountPaise, durationSeconds ->
                            startPayPerUse(amountPaise, durationSeconds)
                        },
                        onModeSelected = { mode ->
                            handleModeChange(mode)
                        },
                        onSubscribePremium = { startPremiumPayment() }
                    )
                }
            }
        }
    }

    private fun toggleFlashlight() {
        if (isFlashlightOn) {
            flashlightManager.turnOff()
        } else {
            if (isPremium) {
                flashlightManager.turnOn()
            }
            // Non-premium users go through payment flow via the UI
        }
    }

    private fun handleModeChange(mode: FlashlightMode) {
        currentMode = mode
        when (mode) {
            FlashlightMode.MAX -> {
                flashlightManager.stopSpecialMode()
                if (isFlashlightOn || isPremium) {
                    flashlightManager.turnOn()
                }
            }
            FlashlightMode.MEDIUM -> {
                flashlightManager.stopSpecialMode()
                if (isFlashlightOn || isPremium) {
                    flashlightManager.turnOn()
                }
            }
            FlashlightMode.STROBE -> {
                if (isPremium) {
                    flashlightManager.startStrobe()
                }
            }
            FlashlightMode.SOS -> {
                if (isPremium) {
                    flashlightManager.startSOS()
                }
            }
        }
    }

    private fun startPayPerUse(amountPaise: Int, durationSeconds: Int) {
        pendingDurationSeconds = durationSeconds
        val durationLabel = when {
            durationSeconds < 60 -> "${durationSeconds}s"
            durationSeconds == 60 -> "1 min"
            else -> "${durationSeconds / 60} min"
        }
        PaymentManager.startPayPerUsePayment(this, amountPaise, durationLabel)
    }

    private fun startPremiumPayment() {
        pendingDurationSeconds = -1 // Marker for premium payment
        PaymentManager.startPremiumPayment(this)
    }

    // --- Razorpay Callbacks ---

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        val paymentId = razorpayPaymentID ?: "unknown"

        if (pendingDurationSeconds == -1) {
            // Premium subscription payment
            premiumManager.activatePremium(paymentId)
            expiryDate = premiumManager.getPremiumExpiryDate()
            lastPaymentId = paymentId
            Toast.makeText(this, "🎉 Premium activated! Enjoy unlimited flashlight.", Toast.LENGTH_LONG).show()
        } else {
            // Pay-per-use payment
            flashlightManager.turnOnForDuration(pendingDurationSeconds)
            Toast.makeText(this, "✅ Payment successful! Flashlight ON for ${pendingDurationSeconds}s", Toast.LENGTH_SHORT).show()
        }
        pendingDurationSeconds = 0
    }

    override fun onPaymentError(code: Int, response: String?) {
        pendingDurationSeconds = 0
        val message = when (code) {
            Checkout.PAYMENT_CANCELED -> "Payment cancelled"
            Checkout.NETWORK_ERROR -> "Network error. Please try again."
            else -> "Payment failed. Please try again."
        }
        Toast.makeText(this, "❌ $message", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        flashlightManager.cleanup()
    }
}
