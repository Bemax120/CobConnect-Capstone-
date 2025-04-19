package com.capstonecobconnect.myapplication.customers.activity

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.capstonecobconnect.myapplication.R

class HistoryAdapter(
    context: Activity,
    private val cobblers: MutableList<HistoryClass>,
    private val userId: String,
    private val firestore: FirebaseFirestore
) : ArrayAdapter<HistoryClass>(context, 0, cobblers) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val cobbler = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.custom_layout_history, parent, false)

        val nameTextView = view.findViewById<TextView>(R.id.historyName)
        val totalPriceTextView = view.findViewById<TextView>(R.id.historyPrice)
        val dateTextView = view.findViewById<TextView>(R.id.historyDate)
        val statusTextView = view.findViewById<TextView>(R.id.status)
        val profileImageView = view.findViewById<ImageView>(R.id.historyImage)

        // Set date
        val formattedDate = cobbler?.dateShed?.substringBefore(" ")?.replace("-", "/") ?: ""
        dateTextView.text = "Date: $formattedDate"

        // ✅ Set total price
        totalPriceTextView.text = "₱${String.format("%.2f", cobbler?.totalPrice ?: 0.0)}"

        //Set status
        statusTextView.text = "Status: ${cobbler?.status}"

        // Fetch cobbler name and image
        cobbler?.cobblerId?.let {
            fetchCustomerDetailsRealTime(it, nameTextView, profileImageView)
        }

        return view
    }


    private fun fetchCustomerDetailsRealTime(customerId: String, nameTextView: TextView, profileImageView: ImageView) {
        if (customerId.isEmpty()) {
            nameTextView.text = "Waiting"
            profileImageView.setImageResource(R.drawable.cob_img)
            return
        }

        val docRef = firestore.collection("cobblers").document(customerId)
        docRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val fname = snapshot.getString("fName")
                val lname = snapshot.getString("lName")
                if (fname != null && lname != null) {
                    nameTextView.text = "$fname $lname"

                    val profileImage = snapshot.getString("profileImg") ?: ""
                    if (profileImage.isNotEmpty()) {
                        Glide.with(context).load(profileImage).circleCrop().into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.cob_img)
                    }
                } else {
                    deleteCustomerData(customerId, nameTextView, profileImageView)
                }
            } else {
                deleteCustomerData(customerId, nameTextView, profileImageView)
            }
        }
    }

    private fun deleteCustomerData(customerId: String, nameTextView: TextView, profileImageView: ImageView) {
        nameTextView.text = "Waiting"
        profileImageView.setImageResource(R.drawable.cob_img)

        firestore.collection("cobblers").document(customerId)
            .delete()
    }
}
