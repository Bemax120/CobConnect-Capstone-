package com.capstonecobconnect.myapplication.cobblers.Activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Cobbler_History_Fragment : Fragment() {

    private lateinit var historyListView: ListView
    private lateinit var ongoingTab: TextView
    private lateinit var historyTab: TextView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var noDataTextView: TextView

    private var adapter: Cobblers_HistoryAdapter? = null
    private val allRequests = mutableListOf<Cobblers_HistoryClass>()
    private val ongoingRequests = mutableListOf<Cobblers_HistoryClass>()
    private val historyRequests = mutableListOf<Cobblers_HistoryClass>()

    private val db = FirebaseFirestore.getInstance()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var currentTab = "ongoing" // Default tab

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cobbler__service_request, container, false)


        loadingSpinner = view.findViewById(R.id.loading_spinner)
        historyListView = view.findViewById(R.id.historyListCob)
        ongoingTab = view.findViewById(R.id.ongoingTab)
        historyTab = view.findViewById(R.id.historyTab)
        noDataTextView = view.findViewById(R.id.emptyStateMessage)

        setupTabSelection()
        retrieveDataFromFirestore()

        return view
    }

    private fun setupTabSelection() {
        ongoingTab.setOnClickListener {
            switchToTab("ongoing")

        }

        historyTab.setOnClickListener {
            switchToTab("history")
        }

        // Set initial tab
        switchToTab(currentTab)
    }

    private fun switchToTab(tab: String) {
        currentTab = tab

        // Update tab appearance
        when (tab) {
            "ongoing" -> {
                ongoingTab.setBackgroundResource(R.drawable.tab_selected_background)
                historyTab.setBackgroundResource(R.drawable.tab_unselected_background)
                showRequests(ongoingRequests)
            }
            "history" -> {
                historyTab.setBackgroundResource(R.drawable.tab_selected_background)
                ongoingTab.setBackgroundResource(R.drawable.tab_unselected_background)
                showRequests(historyRequests)
            }
        }

        Log.d("CobblerHistory", "Switching to tab: $tab")

    }

    private fun retrieveDataFromFirestore() {
        loadingSpinner.visibility = View.VISIBLE
        val cobblerId = auth.currentUser?.uid.toString()

        db.collection("cobblers").document(cobblerId).collection("activityHistory")
            .addSnapshotListener { querySnapshot, error ->
                if (error != null) {
                    loadingSpinner.visibility = View.GONE
                    showNoDataMessage("Error loading history: ${error.message}")
                    return@addSnapshotListener
                }

                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    allRequests.clear()
                    ongoingRequests.clear()
                    historyRequests.clear()

                    for (document in querySnapshot.documents) {
                        val address = document.getString("address") ?: ""
                        val userId = document.getString("userId") ?: ""
                        val dateShed = document.getString("dateShed") ?: ""
                        val status = document.getString("status") ?: ""

                        var totalRequestPrice = 0.0
                        val selectedServices = document.get("selectedServices") as? List<Map<String, Any>>
                        selectedServices?.forEach { service ->
                            val price = service["totalPrice"] as? Number
                            totalRequestPrice += price?.toDouble() ?: 0.0
                        }

                        val historyData = Cobblers_HistoryClass(address, userId, dateShed, totalRequestPrice, status)
                        allRequests.add(historyData)

                        // Log added data for debugging
                        Log.d("CobblerHistory", "Added data: $historyData")

                        when (status.lowercase()) {
                            "ongoing" -> {
                                ongoingRequests.add(historyData)
                                Log.d("CobblerHistory", "Added to ongoingRequests: $historyData")
                            }
                            "completed", "canceled" -> {
                                historyRequests.add(historyData)
                                Log.d("CobblerHistory", "Added to historyRequests: $historyData")
                            }
                        }

                    }

                    // Debugging: Log the data to ensure it's fetched
                    Log.d("CobblerHistory", "Fetched data: ${allRequests.size} items")

                    // After loading data, update the UI with the appropriate tab content
                    loadingSpinner.visibility = View.GONE
                    switchToTab(currentTab)
                } else {
                    loadingSpinner.visibility = View.GONE
                    showNoDataMessage("No activity history found.")
                }
            }
    }

    private fun showRequests(requests: List<Cobblers_HistoryClass>) {
        // Log the number of requests to confirm data is passed correctly
        Log.d("CobblerHistory", "Showing requests: ${requests.size} items")

        if (requests.isEmpty()) {
            showNoDataMessage(when (currentTab) {
                "ongoing" -> "No ongoing activities"
                else -> "No history activities"
            })
        } else {
            // Ensure visibility is set correctly
            noDataTextView.visibility = View.GONE
            historyListView.visibility = View.VISIBLE

            // Ensure the adapter is properly set and notify changes
            activity?.let {
                adapter = Cobblers_HistoryAdapter(it, requests.toMutableList(), auth.currentUser?.uid.toString(), db)
                historyListView.adapter = adapter
                adapter?.notifyDataSetChanged() // Refresh the list
                Log.d("CobblerHistory", "Adapter set and data updated.")
            }
        }

        Log.d("CobblerHistory", "Showing requests: ${requests.size} items")

    }

    private fun showNoDataMessage(message: String) {
        historyListView.visibility = View.GONE
        noDataTextView.text = message
        noDataTextView.visibility = View.VISIBLE
    }
}