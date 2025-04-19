package com.capstonecobconnect.myapplication.cobblers

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class Cobblers_MessageFragment : Fragment() {

    private lateinit var messageAdapter: Cobblers_messageAdapter
    private lateinit var messageList: ArrayList<Cobblers_message>
    private lateinit var recyclerView: RecyclerView
    private lateinit var firestoreDB: FirebaseFirestore
    private lateinit var cobblerId: String
    private lateinit var customerId: String
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var picImg: ImageView
    private lateinit var profileName: TextView
    private var listenerRegistration: ListenerRegistration? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_message_cobblers, container, false)

        recyclerView = view.findViewById(R.id.message_recycler_viewCob)
        messageInput = view.findViewById(R.id.message_inputCob)
        sendButton = view.findViewById(R.id.send_buttonCob)

        messageList = ArrayList()
        messageAdapter = Cobblers_messageAdapter(requireContext(), messageList)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = messageAdapter

        picImg = view.findViewById(R.id.pro_imgCob)
        profileName = view.findViewById(R.id.profile_nameCob)

        firestoreDB = FirebaseFirestore.getInstance()

        cobblerId = arguments?.getString("COBBLER_ID") ?: ""
        customerId = arguments?.getString("CUSTOMER_ID") ?: ""

        getCobblerProfileImage(customerId)
        listenForMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }

        return view
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isNotEmpty()) {
            val messageId = "$customerId$cobblerId"
            val messagesRef = firestoreDB.collection("recentChats").document(messageId).collection("messages")
            val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val message = mapOf(
                "senderId" to FirebaseAuth.getInstance().currentUser?.uid,
                "message" to messageText,
                "senderTime" to currentTime,
                "receiverTime" to currentTime,
                "whoSent" to "cobblers"
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
        firestoreDB.collection("customers").document(documentId)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document != null && document.exists()) {
                        val profileImgUrl = document.getString("profileImg")
                        val fName = document.getString("fName")

                        profileName.text = fName ?: "Unknown"
                        displayProfileImage(profileImgUrl)
                    } else {
                        Toast.makeText(requireContext(), "Profile not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
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

    private fun listenForMessages() {
        val messageId = "$customerId$cobblerId"
        val messagesRef = firestoreDB.collection("recentChats").document(messageId).collection("messages")

        listenerRegistration = messagesRef.orderBy("senderTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error fetching messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newMessageList = ArrayList<Cobblers_message>()
                snapshots?.forEach { doc ->
                    val message = doc.toObject(Cobblers_message::class.java)
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
        fun newInstance(cobblerId: String, customerId: String): Cobblers_MessageFragment {
            val fragment = Cobblers_MessageFragment()
            val args = Bundle()
            args.putString("CUSTOMER_ID", customerId)
            args.putString("COBBLER_ID", cobblerId)
            fragment.arguments = args
            return fragment
        }
    }
}
