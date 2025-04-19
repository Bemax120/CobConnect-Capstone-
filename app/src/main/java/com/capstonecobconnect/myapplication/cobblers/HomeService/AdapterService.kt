package com.capstonecobconnect.myapplication.cobblers.HomeService

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.cobblers.Booking_Page
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdapterService(
    private val context: Context,
    private val customers: List<ServiceClass>,
    private val userId: String

) : RecyclerView.Adapter<AdapterService.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val listeners = mutableMapOf<String, ListenerRegistration>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameSRCob)
        val addressTextView: TextView = view.findViewById(R.id.addressSRCob)
        val dateShed: TextView = view.findViewById(R.id.dateShedCob)
        val phoneTextView: TextView = view.findViewById(R.id.phoneSRCob)
        val accept: TextView = view.findViewById(R.id.btn_acceptSR)
        val statusCob: TextView = view.findViewById(R.id.statusCob)
        val serviceRecyclerView: RecyclerView = view.findViewById(R.id.problemSRCob)
        val amount: TextView = view.findViewById(R.id.amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_layout_cobblerhomeservice, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = customers.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val customer = customers[position]

        holder.nameTextView.text = customer.name
        holder.addressTextView.text = customer.address
        holder.dateShed.text = customer.dateShed
        holder.phoneTextView.text = customer.phone

        holder.serviceRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        loadServiceItems(customer.documentId, holder.serviceRecyclerView, holder.amount)


        listeners[customer.documentId]?.remove()
        listeners[customer.documentId] = db.collection("Request").document(customer.documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Failed to listen for updates.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val status = snapshot?.getString("status") ?: "Pending"
                holder.statusCob.text = status

                when (status) {
                    "Pending" -> {
                        holder.statusCob.setTextColor(Color.BLUE)
                        holder.accept.isEnabled = true
                    }
                    "Accepted" -> {
                        holder.statusCob.setTextColor(Color.GREEN)
                        holder.accept.isEnabled = false
                        holder.accept.setBackgroundResource(R.drawable.round_button_corner)
                    }
                    else -> {
                        holder.statusCob.setTextColor(Color.BLACK)
                        holder.accept.isEnabled = true
                    }
                }
            }

        holder.accept.setOnClickListener {
            handleAcceptButtonClick(customer)
        }
    }

    private fun loadServiceItems(documentId: String, recyclerView: RecyclerView, amountTextView: TextView) {
        db.collection("Request").document(documentId).collection("SelectedServices")
            .get()
            .addOnSuccessListener { documents ->
                val serviceItems = documents.mapNotNull { doc ->
                    val quantity = doc.getLong("quantity")?.toInt() ?: 0
                    val totalPrice = doc.getDouble("totalPrice") ?: 0.0

                    val serviceItems = mutableListOf<ServiceItem>()
                    var totalRequestPrice = 0.0

                    for (doc in documents) {
                        val quantity = doc.getLong("quantity")?.toInt() ?: 0
                        val totalPrice = doc.getDouble("totalPrice") ?: 0.0
                        val servicesString = doc.getString("services") ?: ""
                        val services = servicesString.split(",").map { it.trim() }

                        totalRequestPrice += totalPrice
                    }

                    amountTextView.text = "₱%.2f".format(totalRequestPrice)


                    // 🔥 Fix starts here
                    val servicesString = doc.getString("services") ?: ""
                    val services = servicesString.split(",").map { it.trim() }

                    ServiceItem(quantity, totalPrice, services)
                }
                recyclerView.adapter = ServiceItemAdapter(serviceItems)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to load service items.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun handleAcceptButtonClick(serviceClass: ServiceClass) {
        db.collection("Request").document(serviceClass.documentId)
            .update("status", "Accepted")
            .addOnSuccessListener {
                redirectToBookingPage(serviceClass.geoCoding, serviceClass.address, serviceClass.documentId)
            }
            .addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(context, "Failed to update status.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun redirectToBookingPage(geoCoding: String?, address: String?, documentId: String) {
        val intent = Intent(context, Booking_Page::class.java).apply {
            putExtra("userId", userId)
            putExtra("geoCoding", geoCoding)
            putExtra("address", address)
            putExtra("documentId", documentId)
        }
        context.startActivity(intent)
    }

    fun clearListeners() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}

