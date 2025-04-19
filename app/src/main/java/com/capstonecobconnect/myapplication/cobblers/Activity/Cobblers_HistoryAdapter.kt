package com.capstonecobconnect.myapplication.cobblers.Activity

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class Cobblers_HistoryAdapter(
    context: Activity,
    private val customers: MutableList<Cobblers_HistoryClass>,
    private val userId: String,
    private val firestore: FirebaseFirestore
) : ArrayAdapter<Cobblers_HistoryClass>(context, 0, customers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val customer = getItem(position)
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.custom_layout_history, parent, false)

        val nameTextView = view.findViewById<TextView>(R.id.historyName)
        val totalPriceTextView = view.findViewById<TextView>(R.id.historyPrice)
        val dateTextView = view.findViewById<TextView>(R.id.historyDate)
        val statusTextView = view.findViewById<TextView>(R.id.status)
        val profileImageView = view.findViewById<ImageView>(R.id.historyImage)

        // Check for null customer
        if (customer == null) {
            Log.w("HistoryAdapter", "Customer at position $position is null")
            nameTextView.text = "Unknown"
            totalPriceTextView.text = "₱0.00"
            dateTextView.text = "Date: N/A"
            statusTextView.text = "Status: N/A"
            profileImageView.setImageResource(R.drawable.default_picture)
            return view
        }

        // Format date
        val formattedDate = customer.dateShed?.substringBefore(" ")?.replace("-", "/") ?: "N/A"
        dateTextView.text = "Date: $formattedDate"

        // Set total price
        val price = customer.totalPrice ?: 0.0
        totalPriceTextView.text = "₱${String.format("%.2f", price)}"

        // Set status
        statusTextView.text = "Status: ${customer.status ?: "N/A"}"

        // Fetch name and image using userId
        if (!customer.userId.isNullOrEmpty()) {
            fetchCustomerDetailsRealTime(customer.userId, nameTextView, profileImageView)
        } else {
            Log.w("HistoryAdapter", "User ID is null or empty for customer at position $position")
            nameTextView.text = "Unknown"
            profileImageView.setImageResource(R.drawable.default_picture)
        }

        return view
    }

    private fun fetchCustomerDetailsRealTime(
        customerId: String,
        nameTextView: TextView,
        profileImageView: ImageView
    ) {
        val docRef = firestore.collection("customers").document(customerId)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("HistoryAdapter", "Error fetching customer details", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val fname = snapshot.getString("fName") ?: ""
                val lname = snapshot.getString("lName") ?: ""
                nameTextView.text = "$fname $lname".trim()

                val profileImage = snapshot.getString("profileImg") ?: ""
                if (profileImage.isNotEmpty()) {
                    Glide.with(context)
                        .load(profileImage)
                        .circleCrop()
                        .into(profileImageView)
                } else {
                    profileImageView.setImageResource(R.drawable.default_picture)
                }

                Log.d("HistoryAdapter", "Loaded customer: $fname $lname")
            } else {
                Log.w("HistoryAdapter", "Customer document not found for ID: $customerId")
                nameTextView.text = "Unknown"
                profileImageView.setImageResource(R.drawable.default_picture)
            }
        }
    }
}
