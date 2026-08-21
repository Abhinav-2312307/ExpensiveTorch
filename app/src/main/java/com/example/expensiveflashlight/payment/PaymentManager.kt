package com.example.expensiveflashlight.payment

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.razorpay.Checkout
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object PaymentManager {

    /**
     * Replace this with your own Razorpay Key ID.
     */
    private const val RAZORPAY_KEY_ID = "rzp_test_REPLACE_WITH_YOUR_KEY"
    
    /**
     * Update this to your deployed Vercel backend URL
     * Example: "https://expensive-flashlight-backend.vercel.app"
     */
    private const val BACKEND_URL = "https://expensive-flashlight-api.vercel.app"

    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun initializeRazorpay(context: Context) {
        Checkout.preload(context.applicationContext)
    }

    /**
     * Calls our backend to create a Razorpay Order, then opens the Checkout.
     */
    fun startPayPerUsePayment(
        activity: Activity,
        amountPaise: Int,
        durationSeconds: String
    ) {
        val jsonBody = JSONObject().apply {
            put("amount", amountPaise)
            put("receipt", "rcpt_pu_${System.currentTimeMillis()}")
        }

        val request = Request.Builder()
            .url("$BACKEND_URL/api/create-order")
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PaymentManager", "Failed to create order", e)
                runOnUiThread {
                    Toast.makeText(activity, "Failed to connect to backend", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread {
                        Toast.makeText(activity, "Error from backend", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                try {
                    val orderJson = JSONObject(responseBody)
                    val orderId = orderJson.getString("id")

                    runOnUiThread {
                        openRazorpayCheckout(activity, amountPaise, "Flashlight for $durationSeconds", orderId)
                    }
                } catch (e: Exception) {
                    Log.e("PaymentManager", "Error parsing order JSON", e)
                }
            }
        })
    }

    fun startPremiumPayment(activity: Activity) {
        val amountPaise = 50000 // ₹500
        val jsonBody = JSONObject().apply {
            put("amount", amountPaise)
            put("receipt", "rcpt_prem_${System.currentTimeMillis()}")
        }

        val request = Request.Builder()
            .url("$BACKEND_URL/api/create-order")
            .post(jsonBody.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(activity, "Failed to connect to backend", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody == null) {
                    runOnUiThread { Toast.makeText(activity, "Error from backend", Toast.LENGTH_SHORT).show() }
                    return
                }
                try {
                    val orderJson = JSONObject(responseBody)
                    val orderId = orderJson.getString("id")
                    runOnUiThread {
                        openRazorpayCheckout(activity, amountPaise, "Premium Monthly Subscription", orderId)
                    }
                } catch (e: Exception) {}
            }
        })
    }
    
    fun startPremiumSubscription(activity: Activity, subscriptionId: String) {
        // Implementation remains unchanged for existing subscriptions
        val checkout = Checkout()
        checkout.setKeyID(RAZORPAY_KEY_ID)

        val options = JSONObject().apply {
            put("name", "Expensive Flashlight")
            put("description", "Premium Monthly Subscription")
            put("subscription_id", subscriptionId)
            put("recurring", 1)
            put("currency", "INR")
            put("theme", JSONObject().put("color", "#D4AF37"))
        }

        checkout.open(activity, options)
    }

    private fun openRazorpayCheckout(activity: Activity, amountPaise: Int, description: String, orderId: String) {
        val checkout = Checkout()
        checkout.setKeyID(RAZORPAY_KEY_ID)

        val options = JSONObject().apply {
            put("name", "Expensive Flashlight")
            put("description", description)
            put("currency", "INR")
            put("amount", amountPaise)
            put("order_id", orderId) // MUST pass the order_id here
            put("theme", JSONObject().put("color", "#D4AF37"))
            put("prefill", JSONObject().apply {
                put("email", "")
                put("contact", "")
            })
        }

        checkout.open(activity, options)
    }

    private fun runOnUiThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }
}
