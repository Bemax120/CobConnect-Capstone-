package com.capstonecobconnect.myapplication.customers.message

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.user
import com.capstonecobconnect.myapplication.userAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.Query
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class Message_Page_Fragment : Fragment() {

    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var userListView: ListView
    private lateinit var adapter: userAdapter
    private lateinit var chatTab: TextView
    private lateinit var notifyTab: TextView
    private val auth by lazy { FirebaseAuth.getInstance() }

    private lateinit var notificationListView: ListView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()
    private val lastNotificationIds = mutableSetOf<String>()
    private val dismissedNotificationIds = mutableSetOf<String>()

    private var activityHistoryListener: ListenerRegistration? = null

    private lateinit var notificationTextView: TextView
    private lateinit var chatTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message__page, container, false)

        userListView = view.findViewById(R.id.user_list_view)
        notificationListView = view.findViewById(R.id.notification_list_view)
        notificationTextView = view.findViewById(R.id.notification_empty_text)
        chatTextView = view.findViewById(R.id.chat_empty_text)

        val customerId = auth.currentUser?.uid.toString()
        firestoreDB = FirebaseFirestore.getInstance()
        adapter = userAdapter(requireActivity(), ArrayList(), customerId)
        userListView.adapter = adapter

        notificationAdapter = NotificationAdapter(requireContext(), notificationList)
        notificationListView.adapter = notificationAdapter
        notificationListView.visibility = View.GONE

        chatTab = view.findViewById(R.id.chatTab)
        notifyTab = view.findViewById(R.id.notifyTab)

        selectChatTab()

        chatTab.setOnClickListener { selectChatTab() }
        notifyTab.setOnClickListener { selectNotifyTab() }

        userListView.setOnItemClickListener { _, _, position, _ ->
            val selectedUser = adapter.getItem(position)
            selectedUser?.userId?.let { userId ->
                val messageFragment = MessageFragment.newInstance(userId, customerId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, messageFragment)
                    .addToBackStack(null)
                    .commit()
            } ?: Toast.makeText(context, "User ID is null", Toast.LENGTH_SHORT).show()
        }

        loadProfileImageRealTime()
        loadNotificationsRealTime()

        return view
    }

    private fun selectChatTab() {
        userListView.visibility = View.VISIBLE
        notificationListView.visibility = View.GONE
        notificationTextView.visibility = View.GONE

        chatTab.setBackgroundResource(R.drawable.tab_selected_background)
        notifyTab.setBackgroundResource(R.drawable.tab_unselected_background)

        chatTextView.visibility = if (adapter.count == 0) View.VISIBLE else View.GONE
    }

    private fun selectNotifyTab() {
        userListView.visibility = View.GONE
        notificationListView.visibility = View.VISIBLE
        chatTextView.visibility = View.GONE

        notifyTab.setBackgroundResource(R.drawable.tab_selected_background)
        chatTab.setBackgroundResource(R.drawable.tab_unselected_background)

        notificationAdapter.clear()

        val customerId = auth.currentUser?.uid.toString()
        firestoreDB.collection("notifications")
            .whereEqualTo("receiverId", customerId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    notificationTextView.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                notificationTextView.visibility = View.GONE

                for (doc in querySnapshot.documents) {
                    val notification = NotificationItem(
                        title = doc.getString("title") ?: "No Title",
                        body = doc.getString("body") ?: "No message",
                        receiverId = doc.getString("receiverId") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                    notificationAdapter.addNotification(notification)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showPopup(message: String, notificationId: String) {
        val ctx = context ?: return
        val inflater = LayoutInflater.from(ctx)
        val view = inflater.inflate(R.layout.popup_notification, null)

        val dialog = android.app.AlertDialog.Builder(ctx)
            .setView(view)
            .create()

        view.findViewById<TextView>(R.id.popup_message).text = message
        view.findViewById<TextView>(R.id.popup_dismiss).setOnClickListener {
            dialog.dismiss()

            // ✅ Mark this notification as dismissed in Firestore
            firestoreDB.collection("notifications").document(notificationId)
                .update("dismiss", true)
                .addOnSuccessListener {
                    // Optional: Toast or log success
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to dismiss notification", Toast.LENGTH_SHORT).show()
                }
        }

        showSystemNotification(ctx, "CobConnect Cobbler's Accept", message)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    @SuppressLint("MissingPermission")
    fun showSystemNotification(context: Context, title: String, message: String) {
        val channelId = "cobconnect_channel"
        val channelName = "CobConnect Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for CobConnect App Notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.cob_img)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }

    private fun loadNotificationsRealTime() {
        val customerId = auth.currentUser?.uid.toString()

        firestoreDB.collection("notifications")
            .whereEqualTo("receiverId", customerId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    context?.let {
                        Toast.makeText(it, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    for (docChange in snapshots.documentChanges) {
                        val doc = docChange.document
                        val id = doc.id

                        val isDismissed = doc.getBoolean("dismiss") ?: false
                        if (isDismissed || lastNotificationIds.contains(id)) continue

                        val notification = NotificationItem(
                            title = doc.getString("title") ?: "No Title",
                            body = doc.getString("body") ?: "No message",
                            receiverId = doc.getString("receiverId") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )

                        lastNotificationIds.add(id)
                        notificationAdapter.addNotification(notification)

                        if (docChange.type == DocumentChange.Type.ADDED) {
                            context?.let {
                                showPopup("${notification.title}: ${notification.body}", id)
                            }
                        }
                    }
                }
            }
    }


    private fun loadProfileImageRealTime() {
        val customersRef = firestoreDB.collection("customers")
        val customerId = auth.currentUser?.uid.toString()

        activityHistoryListener = customersRef.document(customerId)
            .collection("activityHistory")
            .addSnapshotListener { activityHistory, error ->
                if (error != null) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (activityHistory == null || activityHistory.isEmpty) {
                    Toast.makeText(context, "No activity message found", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val userIdList = mutableSetOf<String>()
                for (doc in activityHistory.documents) {
                    doc.getString("cobblerId")?.let { userIdList.add(it) }
                }

                if (userIdList.isEmpty()) {
                    Toast.makeText(context, "No users found in activity history", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                fetchCobblerData(userIdList.toList())
            }
    }

    private fun fetchCobblerData(userIdList: List<String>) {
        val cobblerRef = firestoreDB.collection("cobblers")
        adapter.clear()

        for (cobblerId in userIdList) {
            if (cobblerId.isEmpty()) continue

            cobblerRef.document(cobblerId)
                .get()
                .addOnSuccessListener { cobblerDoc ->
                    if (cobblerDoc != null && cobblerDoc.exists()) {
                        val fName = cobblerDoc.getString("fName")
                        val lName = cobblerDoc.getString("lName") ?: ""
                        val fullName = "$fName $lName"
                        val profileImg = cobblerDoc.getString("profileImg")

                        val user = user(fName, fullName, cobblerId, profileImg)
                        adapter.add(user)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activityHistoryListener?.remove()
    }

    companion object {
        fun newInstance(userId: String): Message_Page_Fragment {
            val fragment = Message_Page_Fragment()
            val args = Bundle()
            args.putString("USER_ID", userId)
            fragment.arguments = args
            return fragment
        }
    }
}
