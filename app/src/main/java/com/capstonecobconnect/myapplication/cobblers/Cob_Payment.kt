package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.capstonecobconnect.myapplication.R

class Cob_Payment : AppCompatActivity() {

    private var totalPaymentAmount: Int = 0
    private var paymentConfirmed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cob_payment)

        // Set the initial total payment amount
        totalPaymentAmount = 0 // Set to the initial value you want, e.g., 50

        // Pass the total payment amount to the Stripe checkout URL in cents
        val stripeCheckoutUrl = "https://buy.stripe.com/test_28ofZac8OfAS3fy288?amount=${totalPaymentAmount * 100}"

        // Load Stripe checkout page in WebView
        loadStripeCheckout(stripeCheckoutUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadStripeCheckout(url: String) {
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    if (url.contains("success_url")) {
                        // Payment was successful
                        paymentConfirmed = true
                        savePaymentStatusAndAmount()
                        navigateToHomeFragment(totalPaymentAmount)
                        return true
                    } else if (url.contains("cancel_url")) {
                        // Payment was canceled
                        paymentConfirmed = false
                        savePaymentStatusAndAmount()
                        navigateToHomeFragment(totalPaymentAmount)

                        return true
                    }
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // You can handle page finished event here if needed
            }
        }

        webView.loadUrl(url)
    }

    private fun savePaymentStatusAndAmount() {
        val sharedPreferences = getSharedPreferences("payment_info", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("totalPaymentAmount", totalPaymentAmount)
        editor.putBoolean("paymentConfirmed", paymentConfirmed)
        editor.apply()
    }

    private fun navigateToHomeFragment(totalPaymentAmount: Int) {
        val intent = Intent(this, Cobblers_Home_Fragment::class.java).apply {
            putExtra("navigateTo", "cobblers_Home_fragment")
            putExtra("paymentConfirmed", paymentConfirmed)
            putExtra("totalPaymentAmount", totalPaymentAmount)
        }
        startActivity(intent)
        finish()
    }
}
