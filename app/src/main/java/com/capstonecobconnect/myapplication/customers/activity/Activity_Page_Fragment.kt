package com.capstonecobconnect.myapplication.customers.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class Activity_Page_Fragment : Fragment() {

    private lateinit var historyListView: ListView
    private var adapter: HistoryAdapter? = null
    private val allActivities = mutableListOf<HistoryClass>()
    private val pendingActivities = mutableListOf<HistoryClass>()
    private val ongoingActivities = mutableListOf<HistoryClass>()
    private val historyActivities = mutableListOf<HistoryClass>()
    private lateinit var userId: String
    private val db = FirebaseFirestore.getInstance()
    private lateinit var pendingTab: TextView
    private lateinit var ongoingTab: TextView
    private lateinit var historyTab: TextView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var noDataTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_activity__page_, container, false)

        loadingSpinner = view.findViewById(R.id.loading_spinner)
        loadingSpinner.visibility = View.VISIBLE

        userId = arguments?.getString("USER_ID") ?: ""

        historyListView = view.findViewById(R.id.historyList)
        pendingTab = view.findViewById(R.id.pendingTab)
        ongoingTab = view.findViewById(R.id.ongoingTab)
        historyTab = view.findViewById(R.id.historyTab)
        noDataTextView = view.findViewById(R.id.noDataTextView) // Add this TextView in your XML

        // Tab click listeners
        pendingTab.setOnClickListener {
            selectPendingTab()
            showActivities(pendingActivities)
        }

        ongoingTab.setOnClickListener {
            selectOnGoingTab()
            showActivities(ongoingActivities)
        }

        historyTab.setOnClickListener {
            selectHistoryTab()
            showActivities(historyActivities)
        }

        retrieveDataFromFirestore()
        selectPendingTab() // Default tab

        return view
    }

    private fun selectPendingTab() {
        pendingTab.setBackgroundResource(R.drawable.tab_selected_background)
        ongoingTab.setBackgroundResource(R.drawable.tab_unselected_background)
        historyTab.setBackgroundResource(R.drawable.tab_unselected_background)



    }

    private fun selectOnGoingTab() {
        pendingTab.setBackgroundResource(R.drawable.tab_unselected_background)
        ongoingTab.setBackgroundResource(R.drawable.tab_selected_background)
        historyTab.setBackgroundResource(R.drawable.tab_unselected_background)
    }

    private fun selectHistoryTab() {
        pendingTab.setBackgroundResource(R.drawable.tab_unselected_background)
        ongoingTab.setBackgroundResource(R.drawable.tab_unselected_background)
        historyTab.setBackgroundResource(R.drawable.tab_selected_background)
    }

    private fun retrieveDataFromFirestore() {
        db.collection("customers").document(userId).collection("activityHistory")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    loadingSpinner.visibility = View.GONE
                    showNoDataMessage("Failed to load activities: ${error.message}")
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    allActivities.clear()
                    pendingActivities.clear()
                    ongoingActivities.clear()
                    historyActivities.clear()

                    for (document in querySnapshot.documents) {
                        val address = document.getString("address") ?: ""
                        val cobblerId = document.getString("cobblerId") ?: ""
                        val dateShed = document.getString("dateShed") ?: ""
                        val status = document.getString("status") ?: ""
                        var totalRequestPrice = 0.0

                        val selectedServices = document.get("selectedServices") as? List<Map<String, Any>>
                        selectedServices?.forEach { service ->
                            val price = service["totalPrice"] as? Number
                            totalRequestPrice += price?.toDouble() ?: 0.0
                        }

                        val historyData = HistoryClass(address, cobblerId, dateShed, totalRequestPrice, status)
                        allActivities.add(historyData)

                        // Categorize activities based on status
                        when (status.lowercase()) {
                            "pending" -> pendingActivities.add(historyData)
                            "ongoing" -> ongoingActivities.add(historyData)
                            "complete", "canceled" -> historyActivities.add(historyData)
                        }
                    }

                    val selectedTab = when {
                        pendingTab.background.constantState == resources.getDrawable(R.drawable.tab_selected_background).constantState -> pendingActivities
                        ongoingTab.background.constantState == resources.getDrawable(R.drawable.tab_selected_background).constantState -> ongoingActivities
                        else -> historyActivities
                    }

                    showActivities(selectedTab)
                } else {
                    allActivities.clear()
                    pendingActivities.clear()
                    ongoingActivities.clear()
                    historyActivities.clear()
                    showNoDataMessage("No activities found")
                }

                loadingSpinner.visibility = View.GONE
            }

        db.collection("Request")
            .whereEqualTo("userId", userId) // Filter by matching userId
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    showNoDataMessage("Error fetching requests: ${error.message}")
                    return@addSnapshotListener
                }

                pendingActivities.clear()

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val address = document.getString("address") ?: ""
                        val cobblerId = "Waiting"
                        val dateShed = document.getString("dateShed") ?: ""
                        val status = document.getString("status") ?: ""

                        // Now fetch the SelectedServices subcollection
                        document.reference.collection("SelectedServices")
                            .get()
                            .addOnSuccessListener { servicesSnapshot ->
                                var totalRequestPrice = 0.0
                                for (serviceDoc in servicesSnapshot.documents) {
                                    val price = serviceDoc.getDouble("totalPrice") ?: 0.0
                                    totalRequestPrice += price
                                }
                                val historyData = HistoryClass(
                                    address,
                                    cobblerId,
                                    dateShed,
                                    totalRequestPrice,
                                    status
                                )

                                // Categorize activities based on status
                                when (status.lowercase()) {
                                    "pending" -> {
                                        pendingActivities.add(historyData)
                                        showActivities(pendingActivities)
                                    }
                                }
                            }

                    }

                } else {

                }
            }
    }


    private fun showActivities(activities: List<HistoryClass>) {
        if (activities.isEmpty()) {
            showNoDataMessage(when {
                activities === pendingActivities -> "No pending activities"
                activities === ongoingActivities -> "No ongoing activities"
                else -> "No history available"
            })
        } else {
            noDataTextView.visibility = View.GONE
            historyListView.visibility = View.VISIBLE
            activity?.let {
                adapter = HistoryAdapter(it, activities.toMutableList(), userId, db)
                historyListView.adapter = adapter
            }
        }
    }

    private fun showNoDataMessage(message: String) {
        historyListView.visibility = View.GONE
        noDataTextView.text = message
        noDataTextView.visibility = View.VISIBLE
    }

    companion object {
        fun newInstance(userId: String): Activity_Page_Fragment {
            val fragment = Activity_Page_Fragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}