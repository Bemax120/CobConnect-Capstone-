package com.capstonecobconnect.myapplication.cobblers

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class Cobblers_messageAdapter(val context: Context, val messageList: ArrayList<Cobblers_message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val SENT_ITEM = 1
    private val RECEIVED_ITEM = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == SENT_ITEM) {
            val view = LayoutInflater.from(context).inflate(R.layout.recycler_layout_sender_cobblers, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.recycler_layout_reciever_cobblers, parent, false)
            ReceiverViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currMessage = messageList[position]

        val originalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val targetFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val formattedTime: String = try {
            val date: Date? = originalFormat.parse(currMessage.senderTime ?: "")
            date?.let { targetFormat.format(it) } ?: "Invalid time"
        } catch (e: Exception) {
            "Invalid time"
        }

        if (holder is SentViewHolder) {
            holder.senderMsg.text = currMessage.message
            holder.senderTime.text = formattedTime
        } else if (holder is ReceiverViewHolder) {
            holder.receiverMsg.text = currMessage.message
            holder.receiverTime.text = formattedTime
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun getItemViewType(position: Int): Int {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        return if (messageList[position].senderId == currentUser) {
            SENT_ITEM
        } else {
            RECEIVED_ITEM
        }
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderMsg: TextView = itemView.findViewById(R.id.sender_msagCob)
        val senderTime: TextView = itemView.findViewById(R.id.sender_timeCob)
    }

    class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverMsg: TextView = itemView.findViewById(R.id.reciever_msagCob)
        val receiverTime: TextView = itemView.findViewById(R.id.reciever_timeCob)
    }
}
