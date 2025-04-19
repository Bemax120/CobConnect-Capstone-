package com.capstonecobconnect.myapplication.wallet

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class PaymentMethodDialog : DialogFragment() {

    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var customerId: String? = null
    private var ephemeralKey: String? = null

    private val backendUrl = "https://cobconnect-backend.onrender.com/create-payment-intent"
    private var paymentAmount: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater: LayoutInflater = layoutInflater
        val view = inflater.inflate(R.layout.payment_method, null)

        val stripeMethod = view.findViewById<LinearLayout>(R.id.stripe_method)
        val gcashMethod = view.findViewById<LinearLayout>(R.id.gcash_method)
        val codMethod = view.findViewById<LinearLayout>(R.id.cod_method)

        gcashMethod.isEnabled = false
        codMethod.isEnabled = false
        gcashMethod.alpha = 0.5f
        codMethod.alpha = 0.5f

        // Retrieve userId from arguments
        val userId = arguments?.getString("USER_ID")

        PaymentConfiguration.init(
            requireContext(),
            "pk_test_51PLSnDK060l68PJv1saDTRAelYSfTjjn19pWvDklC0zy4GmB3izLhyPohyTxY38jKAyexuS7lRVJliagOfu5KtCt00KuehOjLz"
        )
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        stripeMethod.setOnClickListener {
            showAmountDialog(userId)
        }

        builder.setView(view)
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    private fun showAmountDialog(userId: String?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Enter Dollar Amount")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            val amountEntered = input.text.toString().toDoubleOrNull()
            if (amountEntered != null && amountEntered > 0) {
                paymentAmount = (amountEntered * 100).toInt() // Convert dollar to cents
                fetchPaymentIntent()
            } else {
                Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun fetchPaymentIntent() {
        val client = OkHttpClient()
        val requestBody = """
            {"amount": $paymentAmount}
        """.trimIndent().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(backendUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("PAYMENT_ERROR", "Error fetching payment details: ${e.message}")
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Failed to fetch payment details",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { responseBodyString ->
                    response.close()
                    try {
                        val jsonObject = JSONObject(responseBodyString)
                        clientSecret = jsonObject.getString("clientSecret")
                        customerId = jsonObject.getString("customerId")
                        ephemeralKey = jsonObject.getString("ephemeralKey")

                        requireActivity().runOnUiThread {
                            presentPaymentSheet()
                        }
                    } catch (e: JSONException) {
                        Log.e("JSON_ERROR", "Invalid JSON Response: $responseBodyString", e)
                        requireActivity().runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Error parsing payment details",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        })
    }

    private fun presentPaymentSheet() {
        if (clientSecret == null || customerId == null || ephemeralKey == null) {
            Toast.makeText(requireContext(), "Payment details are missing", Toast.LENGTH_LONG)
                .show()
            return
        }

        paymentSheet.presentWithPaymentIntent(
            clientSecret!!,
            PaymentSheet.Configuration(
                merchantDisplayName = "CobConnect",
                customer = PaymentSheet.CustomerConfiguration(
                    id = customerId!!,
                    ephemeralKeySecret = ephemeralKey!!
                )
            )
        )
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        requireActivity().runOnUiThread {
            when (paymentSheetResult) {
                is PaymentSheetResult.Completed -> {
                    Toast.makeText(requireContext(), "✅ Payment successful!", Toast.LENGTH_LONG).show()

                    val userId = arguments?.getString("USER_ID")
                    if (userId != null) {
                        val transactionAmount = paymentAmount / 100.0

                        // Record transaction and update wallet balance
                        recordTransaction(userId, transactionAmount)
                        updateWalletBalance(userId, transactionAmount)

                        // Update UI with new balance
                        updateUI(userId)
                    } else {
                        Log.e("Wallet", "User ID is null, cannot update balance")
                    }

                    dismiss()
                }

                is PaymentSheetResult.Canceled -> {
                    Toast.makeText(requireContext(), "⚠️ Payment was canceled!", Toast.LENGTH_LONG).show()
                }

                is PaymentSheetResult.Failed -> {
                    Toast.makeText(
                        requireContext(),
                        "❌ Payment failed: ${paymentSheetResult.error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun recordTransaction(userId: String, amount: Double) {
        val walletRef = FirebaseFirestore.getInstance().collection("wallet").document(userId)

        // Create a reference to the transactions collection
        val transactionRef = walletRef.collection("transactions").document()

        // Use a unique transactionId (e.g., use the document ID or combine userId + timestamp)
        val transactionId = transactionRef.id // Or generate a unique ID if needed

        val transactionData = hashMapOf(
            "transactionId" to transactionId,
            "userId" to userId,
            "type" to "Cash-In",
            "amount" to amount,
            "timestamp" to System.currentTimeMillis(),
            "status" to "Success"
        )

        // Check if the transaction already exists before adding a new one
        walletRef.collection("transactions")
            .whereEqualTo("transactionId", transactionId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    // Transaction doesn't exist, add it
                    transactionRef.set(transactionData)
                        .addOnSuccessListener {
                            Log.d("Transaction", "Transaction recorded successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Transaction", "Failed to record transaction", e)
                        }
                } else {
                    // Transaction already exists, log and skip
                    Log.d("Transaction", "Transaction already exists, skipping")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Transaction", "Error checking existing transactions", e)
            }
    }


    private fun updateWalletBalance(userId: String, amount: Double) {
        val walletRef = FirebaseFirestore.getInstance().collection("wallet").document(userId)

        walletRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentBalance = document.getDouble("balance") ?: 0.00
                val newBalance = currentBalance + amount

                walletRef.update("balance", newBalance)
                    .addOnSuccessListener {
                        Log.d("Wallet", "Wallet balance updated: $newBalance")
                    }
                    .addOnFailureListener { e ->
                        Log.e("Wallet", "Error updating wallet balance", e)
                    }
            } else {
                Log.e("Wallet", "Wallet not found for user: $userId")
            }
        }.addOnFailureListener { e ->
            Log.e("Wallet", "Error fetching wallet document", e)
        }
    }

    private fun updateUI(userId: String?) {
        if (userId != null) {
            val walletRef = FirebaseFirestore.getInstance().collection("wallet").document(userId)
            walletRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val newBalance = document.getDouble("balance") ?: 0.0
                    // Update your UI components (e.g., TextView showing balance)
                    // Example: textViewBalance.text = "Balance: $newBalance"
                }
            }
        }
    }
}


