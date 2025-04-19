package com.capstonecobconnect.myapplication

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.customers.message.MessageActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class userAdapter(
    private val context: Activity,
    private val userList: MutableList<user>,
    private val customerId: String
) : ArrayAdapter<user>(context, 0, userList) {

    private var listenerRegistrations: MutableMap<String, ListenerRegistration> = mutableMapOf()
    private val mainHandler = Handler(Looper.getMainLooper()) // Ensure UI updates on the main thread
    private val refreshHandler = Handler(Looper.getMainLooper()) // Handler for the periodic refresh
    private val refreshInterval: Long = 1000 // Refresh every 1 second

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.recycle_layout_single_row, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val currentUser = userList[position]
        holder.profileName.text = currentUser.fName ?: "Unknown" // Safely set name or fallback
        val fName = currentUser.fName ?: "Unknown" // Assign a default value if null

        Glide.with(context)
            .load(currentUser.profileImg ?: R.drawable.default_picture) // Handle possible null profile image
            .circleCrop()
            .placeholder(R.drawable.default_picture)
            .error(R.drawable.default_picture)
            .into(holder.profileImage)

        // Fetch and listen for recent chat updates
        fetchRecentChat(currentUser.userId ?: "", customerId, holder.recentChatTextView, holder.recentChatPerson, fName)

        // Schedule periodic refresh to update recent chat every 1 second
        refreshRecentChat(currentUser.userId ?: "", customerId, holder.recentChatTextView, holder.recentChatPerson, fName)

        view?.setOnClickListener {
            // Ensure the userId is not null before starting MessageActivity
            currentUser.userId?.let { cobblerId ->
                val intent = Intent(context, MessageActivity::class.java).apply {
                    putExtra("COBBLER_ID", cobblerId)
                    putExtra("CUSTOMER_ID", customerId) // Pass customerId to MessageActivity
                }
                context.startActivity(intent)
            }
        }

        return view!!
    }

    private fun fetchRecentChat(
        cobblerId: String,
        customerId: String,
        recentChatTextView: TextView,
        recentChatPerson: TextView,
        fName: String
    ) {
        if (cobblerId.isEmpty()) {
            mainHandler.post {
                recentChatTextView.text = "No recent chat"
                recentChatPerson.text = ""
            }
            return
        }

        val messageId = customerId + cobblerId
        val messagesRef = FirebaseFirestore.getInstance().collection("recentChats").document(messageId)
            .collection("messages")
            .orderBy("senderTime", Query.Direction.DESCENDING)
            .limit(1)

        listenerRegistrations[messageId]?.remove() // Remove previous listener

        val listenerRegistration = messagesRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                mainHandler.post {
                    recentChatTextView.text = "Failed to fetch recent chat"
                    recentChatPerson.text = ""
                }
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val document = snapshots.documents[0]
                val recentChat = document.getString("message") ?: "No message"
                val senderId = document.getString("senderId")

                mainHandler.post {
                    if (senderId == customerId) {
                        recentChatPerson.text = "You: "
                    } else {
                        val firstName = fName.split(" ").firstOrNull() ?: "Unknown"
                        recentChatPerson.text = "$firstName: "
                    }
                    recentChatTextView.text = recentChat
                }
            } else {
                mainHandler.post {
                    recentChatTextView.text = "No messages"
                    recentChatPerson.text = ""
                }
            }
        }

        listenerRegistrations[messageId] = listenerRegistration
    }

    private fun refreshRecentChat(
        cobblerId: String,
        customerId: String,
        recentChatTextView: TextView,
        recentChatPerson: TextView,
        fName: String
    ) {
        // Refresh the recent chat every second
        refreshHandler.postDelayed(object : Runnable {
            override fun run() {
                fetchRecentChat(cobblerId, customerId, recentChatTextView, recentChatPerson, fName)
                refreshHandler.postDelayed(this, refreshInterval) // Schedule the next refresh
            }
        }, refreshInterval)
    }

    // Call this to stop the periodic refreshes and remove Firestore listeners
    fun clearListeners() {
        listenerRegistrations.values.forEach { it.remove() }
        listenerRegistrations.clear()
        refreshHandler.removeCallbacksAndMessages(null) // Stop periodic refreshes
    }

    private class ViewHolder(view: View) {
        val profileName: TextView = view.findViewById(R.id.profile_name)
        val profileImage: ImageView = view.findViewById(R.id.profile_pic)
        val recentChatTextView: TextView = view.findViewById(R.id.recent_chatTextView)
        val recentChatPerson: TextView = view.findViewById(R.id.recent_chatPerson)
    }
}
