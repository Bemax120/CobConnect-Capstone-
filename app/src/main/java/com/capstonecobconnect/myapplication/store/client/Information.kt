package com.capstonecobconnect.myapplication.store.client

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Information : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var tvPlaceOrder: TextView
    private lateinit var tvAddress: TextView
    private lateinit var rvItems: RecyclerView
    private lateinit var tvPriceDetails: TextView
    private lateinit var rgPayment: RadioGroup
    private lateinit var stripePay: RadioButton
    private lateinit var gcashPay: RadioButton
    private lateinit var codPay: RadioButton
    private lateinit var tvTotal: TextView
    private lateinit var btnPayNow: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()


        tvPlaceOrder = findViewById(R.id.tv_place_order)
        tvAddress = findViewById(R.id.tv_address)
        rvItems = findViewById(R.id.rv_items)
        tvPriceDetails = findViewById(R.id.tv_price_details)
        rgPayment = findViewById(R.id.rg_payment)
        stripePay = findViewById(R.id.stripe_pay)
        gcashPay = findViewById(R.id.gcash_pay)
        codPay = findViewById(R.id.cod_pay)
        tvTotal = findViewById(R.id.tv_total)
        btnPayNow = findViewById(R.id.btn_pay_now)


        btnPayNow.setOnClickListener {
            val selectedPaymentId = rgPayment.checkedRadioButtonId
            if (selectedPaymentId == -1) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
            } else {
                val selectedPaymentMethod = findViewById<RadioButton>(selectedPaymentId).text
                Toast.makeText(this, "Processing payment via $selectedPaymentMethod", Toast.LENGTH_SHORT).show()

            }
        }
    }
}
