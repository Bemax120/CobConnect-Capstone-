package com.capstonecobconnect.myapplication.cobblers

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class Cobblers_Message_Fragment : Fragment() {

    private lateinit var userId: String
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var userListView: ListView
    private lateinit var adapter: Cobblers_userAdapter
    // Add this if you have notifications

    private lateinit var chatsTab: TextView
    private lateinit var notificationsTab: TextView
    private var currentTab = "chats" // Default tab

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_cobblers__message, container, false)

        userListView = view.findViewById(R.id.user_list_viewCob)
        userId = arguments?.getString("USER_ID") ?: ""
        firestoreDB = FirebaseFirestore.getInstance()
        adapter = Cobblers_userAdapter(requireActivity(), ArrayList(), userId)
        userListView.adapter = adapter

        // Initialize your notification adapter here if you have one
        // notificationAdapter = NotificationAdapter(...)

        chatsTab = view.findViewById(R.id.upcomingTab)
        notificationsTab = view.findViewById(R.id.historyTab)

        setupTabSelection()
        switchToTab(currentTab)


        if (currentTab == "chats") {
            loadChats()
        }


        return view
    }

    private fun setupTabSelection() {
        chatsTab.setOnClickListener {
            switchToTab("chats")
            loadChats()
        }

        notificationsTab.setOnClickListener {
            switchToTab("notifications")
            loadNotifications()
        }
    }

    private fun switchToTab(tab: String) {
        currentTab = tab

        when (tab) {
            "chats" -> {
                // Show chats UI
                userListView.visibility = View.VISIBLE
                // Hide notifications UI if you have a separate view
                // notificationListView.visibility = View.GONE

                chatsTab.setBackgroundResource(R.drawable.tab_selected_background)
                notificationsTab.setBackgroundResource(R.drawable.tab_unselected_background)
            }
            "notifications" -> {
                // Hide chats UI
                userListView.visibility = View.GONE
                // Show notifications UI if you have a separate view
                // notificationListView.visibility = View.VISIBLE

                notificationsTab.setBackgroundResource(R.drawable.tab_selected_background)
                chatsTab.setBackgroundResource(R.drawable.tab_unselected_background)
            }
        }
    }

    private fun loadChats() {
        // Clear previous data
        adapter.clear()

        val customersRef = firestoreDB.collection("cobblers")
        customersRef.document(userId)
            .collection("activityHistory")
            .get()
            .addOnSuccessListener { activityHistory ->
                val userIdList = mutableSetOf<String>()
                for (doc in activityHistory.documents) {
                    val customerId = doc.getString("userId")
                    customerId?.let { userIdList.add(it) }
                }
                fetchCustomerData(userIdList.toList())
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading chats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCustomerData(userIdList: List<String>) {
        val customerRef = firestoreDB.collection("customers")

        for (userId in userIdList) {
            customerRef.document(userId)
                .get()
                .addOnSuccessListener { customerDoc ->
                    if (customerDoc.exists()) {
                        val name = customerDoc.getString("fName")
                        val lName = customerDoc.getString("lName") ?: ""
                        val fName = "$name $lName"
                        val profileImg = customerDoc.getString("profileImg")

                        val cobUser = Cobblers_user(fName, userId, profileImg)
                        adapter.add(cobUser)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Error loading user: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadNotifications() {
        // Clear previous data
        adapter.clear()

        // Implement your notification loading logic here
        // For example:
        /*
        firestoreDB.collection("notifications")
            .whereEqualTo("receiverId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Process notifications
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        */

        Toast.makeText(requireContext(), "Notifications would load here", Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun newInstance(userId: String): Cobblers_Message_Fragment {
            val fragment = Cobblers_Message_Fragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}