package com.capstonecobconnect.myapplication.cobblers.HomeService

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class Cobblers_Home_Service : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterService
    private val customers = mutableListOf<ServiceClass>()
    private var userId: String? = null
    private var requestListener: ListenerRegistration? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobblers_home_service)

        userId = intent.getStringExtra("USER_ID")

        recyclerView = findViewById(R.id.listViewCob)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapterService(this, customers, userId ?: "")
        recyclerView.adapter = adapter

        listenForRequestsUpdates()
    }

    private fun listenForRequestsUpdates() {
        requestListener = db.collection("Request")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                customers.clear()

                snapshots?.documents?.forEach { document ->
                    val customerDocumentId = document.getString("userId") ?: return@forEach
                    val address = document.getString("address") ?: ""
                    val problem = document.getString("issue") ?: ""
                    val dateShed = document.getString("dateShed") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val status = document.getString("status") ?: ""
                    val geoCoding = document.getString("geolocation") ?: ""
                    val latitude = document.getDouble("latitude")
                    val longitude = document.getDouble("longitude")
                    val documentId = document.id

                    fetchCustomerDetails(
                        customerDocumentId,
                        address,
                        problem,
                        dateShed,
                        phone,
                        documentId,
                        status,
                        geoCoding,
                        latitude,
                        longitude
                    )
                }

                if (snapshots == null || snapshots.isEmpty) {
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun fetchCustomerDetails(
        customerDocumentId: String,
        address: String,
        problem: String,
        dateShed: String,
        phone: String,
        documentId: String,
        status: String,
        geoCoding: String,
        latitude: Double?,
        longitude: Double?
    ) {
        db.collection("customers").document(customerDocumentId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val fName = document.getString("fName") ?: ""
                    val lName = document.getString("lName") ?: ""
                    val name = "$fName $lName"

                    val serviceClass = ServiceClass(
                        name,
                        address,
                        problem,
                        dateShed,
                        phone,
                        customerDocumentId,
                        documentId,
                        status,
                        geoCoding,
                        latitude,
                        longitude
                    )
                    customers.add(serviceClass)
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { it.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        requestListener?.remove()
        adapter.clearListeners()
    }
}
