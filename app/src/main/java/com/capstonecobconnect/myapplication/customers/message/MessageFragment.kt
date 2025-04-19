package com.capstonecobconnect.myapplication.customers.message

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class MessageFragment : Fragment() {

    private lateinit var messageAdapter: messageAdapter
    private lateinit var messageList: ArrayList<message>
    private lateinit var recyclerView: RecyclerView
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var cobblerId: String
    private lateinit var customerId: String
    private lateinit var userId: String
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var picImg: ImageView
    private lateinit var backimg: ImageButton
    private lateinit var profileName: TextView
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message, container, false)

        // Initialize Firebase Firestore
        firestoreDB = FirebaseFirestore.getInstance()

        // Initialize views
        backimg = view.findViewById(R.id.back_arrow)
        sendButton = view.findViewById(R.id.send_button)
        messageInput = view.findViewById(R.id.message_input)
        picImg = view.findViewById(R.id.pro_img)
        profileName = view.findViewById(R.id.profile_name)
        recyclerView = view.findViewById(R.id.message_recycler_view)

        // Initialize messageList and adapter
        messageList = ArrayList()
        messageAdapter = messageAdapter(requireContext(), messageList) // Pass context to adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = messageAdapter

        // Retrieve arguments
        cobblerId = arguments?.getString("COBBLER_ID") ?: ""
        customerId = arguments?.getString("CUSTOMER_ID") ?: ""
        userId = customerId

        // Load cobbler profile and set up listeners
        getCobblerProfileImage(cobblerId)
        listenForMessages()

        // Set click listener for sending messages
        sendButton.setOnClickListener { sendMessage() }

        // Back navigation handling
        backimg.setOnClickListener {
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack() // Pops the current fragment from the back stack
            } else {
                requireActivity().onBackPressed() // Close activity if no fragments left
            }
        }

        return view
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val messageId = customerId + cobblerId
            val messagesRef = firestoreDB.collection("recentChats").document(messageId).collection("messages")
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val message = mapOf(
                "senderId" to FirebaseAuth.getInstance().currentUser?.uid,
                "message" to messageText,
                "senderTime" to currentTime,
                "receiverTime" to currentTime,
                "whoSent" to "customer"
            )

            messagesRef.add(message).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    messageInput.text.clear()
                } else {
                    Toast.makeText(requireContext(), "Failed to send message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCobblerProfileImage(documentId: String) {
        firestoreDB.collection("cobblers").document(documentId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val profileImgUrl = document.getString("profileImg")
                        val fName = document.getString("fName")

                        profileName.text = fName
                        displayProfileImage(profileImgUrl)
                    }
                }
            }
    }

    private fun displayProfileImage(profileImgUrl: String?) {
        if (!profileImgUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(profileImgUrl)
                .circleCrop()
                .into(picImg)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun listenForMessages() {
        val messageId = customerId + cobblerId
        val messagesRef = firestoreDB.collection("recentChats").document(messageId).collection("messages")

        listenerRegistration = messagesRef.orderBy("senderTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error fetching messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Toast.makeText(requireContext(), "No messages found", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newMessageList = ArrayList<message>()
                for (doc in snapshots) {
                    val message = doc.toObject(message::class.java)
                    newMessageList.add(message)
                }

                messageList.clear()
                messageList.addAll(newMessageList)
                messageAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messageList.size - 1)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
    }

    companion object {
        fun newInstance(cobblerId: String, customerId: String): MessageFragment {
            val fragment = MessageFragment()
            val args = Bundle()
            args.putString("COBBLER_ID", cobblerId)
            args.putString("CUSTOMER_ID", customerId)
            fragment.arguments = args
            return fragment
        }
    }
}
