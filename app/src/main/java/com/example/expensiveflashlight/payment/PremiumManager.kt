package com.example.expensiveflashlight.payment

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PremiumManager(context: Context) {

    private val prefs = context.getSharedPreferences("premium_prefs", Context.MODE_PRIVATE)

    private companion object {
        const val KEY_IS_PREMIUM = "is_premium"
        const val KEY_PREMIUM_EXPIRY = "premium_expiry"
        const val KEY_LAST_PAYMENT_ID = "last_payment_id"

        /** Premium access lasts 30 days from the moment of activation. */
        val PREMIUM_DURATION_MS: Long = TimeUnit.DAYS.toMillis(30)

        /** Date format used for the human-readable expiry string. */
        val DATE_FORMAT = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }

    private val _isPremium = MutableStateFlow(checkPremiumStatus())
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    /**
     * Activates premium for 30 days and persists the given [paymentId].
     */
    fun activatePremium(paymentId: String) {
        val expiryMillis = System.currentTimeMillis() + PREMIUM_DURATION_MS
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, true)
            .putLong(KEY_PREMIUM_EXPIRY, expiryMillis)
            .putString(KEY_LAST_PAYMENT_ID, paymentId)
            .apply()

        _isPremium.value = true
    }

    /**
     * Returns `true` if the user has an active, non-expired premium subscription.
     * Also automatically deactivates premium if it has expired.
     */
    fun checkPremiumStatus(): Boolean {
        val isPremiumStored = prefs.getBoolean(KEY_IS_PREMIUM, false)
        if (!isPremiumStored) return false

        val expiry = prefs.getLong(KEY_PREMIUM_EXPIRY, 0L)
        if (System.currentTimeMillis() >= expiry) {
            // Premium has expired — clear it silently.
            deactivatePremium()
            return false
        }
        return true
    }

    /**
     * Clears all premium-related data from SharedPreferences.
     */
    fun deactivatePremium() {
        prefs.edit()
            .putBoolean(KEY_IS_PREMIUM, false)
            .remove(KEY_PREMIUM_EXPIRY)
            .remove(KEY_LAST_PAYMENT_ID)
            .apply()

        _isPremium.value = false
    }

    /**
     * Returns a formatted expiry date string (e.g. "05 Jul 2026, 11:52 PM") or `null`
     * if the user is not premium.
     */
    fun getPremiumExpiryDate(): String? {
        val expiry = prefs.getLong(KEY_PREMIUM_EXPIRY, 0L)
        if (expiry == 0L) return null
        return DATE_FORMAT.format(Date(expiry))
    }

    /**
     * Returns the Razorpay payment ID that was stored when premium was last activated,
     * or `null` if none exists.
     */
    fun getLastPaymentId(): String? {
        return prefs.getString(KEY_LAST_PAYMENT_ID, null)
    }
}
