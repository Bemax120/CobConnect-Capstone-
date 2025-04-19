package com.capstonecobconnect.myapplication.cobblers

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration

class Cobblers_userAdapter(
    private val context: Activity,
    private val userList: MutableList<Cobblers_user>,
    private val userId: String
) : ArrayAdapter<Cobblers_user>(context, 0, userList) {

    private var listenerRegistration: ListenerRegistration? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val holder: ViewHolder
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.recycle_layout_single_row_cobblers, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            holder = view.tag as ViewHolder
        }

        val currentUser = userList[position]
        holder.profileName.text = currentUser.fName
        val fName = currentUser.fName
        Glide.with(context)
            .load(currentUser.profileImg)
            .circleCrop()
            .placeholder(R.drawable.default_picture)
            .error(R.drawable.default_picture)
            .into(holder.profileImage)

        // Fetch recent chat and update the recent_chatTextView
        fetchRecentChat(currentUser.customerId!!, userId, holder.recentChatTextView, holder.recentChatPerson, fName!!)

        view?.setOnClickListener {
            val intent = Intent(context, Cobblers_MessageActivity::class.java).apply {
                putExtra("CUSTOMER_ID", currentUser.customerId)
                putExtra("COBBLER_ID", userId) // Pass customerId to MessageActivity
            }
            context.startActivity(intent)
        }

        return view!!
    }

    private fun fetchRecentChat(cobblerId: String, customerId: String, recentChatTextView: TextView, recentChatPerson: TextView, fName: String) {
        val messageId = cobblerId + customerId
        val messagesRef = FirebaseFirestore.getInstance().collection("recentChats").document(messageId)
            .collection("messages")
            .orderBy("senderTime", Query.Direction.DESCENDING) // Order by timestamp to get the most recent message
            .limit(1) // Limit to 1 message to get the most recent chat

        // Remove any previous listener
        listenerRegistration?.remove()

        listenerRegistration = messagesRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                recentChatTextView.text = "Failed to fetch recent chat" // Display error message
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                for (document in snapshots) {
                    val recentChat = document.getString("message")
                    val senderId = document.getString("senderId")

                    if (senderId == customerId) {
                        recentChatPerson.text = "You: "
                    } else {
                        recentChatPerson.text = "$fName "
                    }

                    recentChatTextView.text = recentChat
                    return@addSnapshotListener // Exit the loop after setting the recent chat text
                }
            }
        }
    }

    override fun notifyDataSetChanged() {
        super.notifyDataSetChanged()
        // Remove the listener when the data set changes to prevent memory leaks
        listenerRegistration?.remove()
    }

    private class ViewHolder(view: View) {
        val profileName: TextView = view.findViewById(R.id.profile_nameCob)
        val profileImage: ImageView = view.findViewById(R.id.profile_picCob)
        val recentChatTextView: TextView = view.findViewById(R.id.recent_chatTextViewCob)
        val recentChatPerson: TextView = view.findViewById(R.id.recent_chatPersonCob)
    }
}
