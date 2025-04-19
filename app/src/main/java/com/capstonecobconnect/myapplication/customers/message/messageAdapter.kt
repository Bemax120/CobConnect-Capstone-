package com.capstonecobconnect.myapplication.customers.message

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.capstonecobconnect.myapplication.message
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class messageAdapter(private val context: Context, private val messageList: ArrayList<message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val SENT_ITEM = 1
        const val RECEIVED_ITEM = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(context)
        return if (viewType == SENT_ITEM) {
            val view = layoutInflater.inflate(R.layout.recycler_layout_sender, parent, false)
            SentViewHolder(view)
        } else {
            val view = layoutInflater.inflate(R.layout.recycler_layout_reciever, parent, false)
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

        when (holder) {
            is SentViewHolder -> {
                holder.senderMsg.text = currMessage.message
                holder.senderTime.text = formattedTime
            }
            is ReceiverViewHolder -> {
                holder.receiverMsg.text = currMessage.message
                holder.receiverTime.text = formattedTime
            }
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
        val senderMsg: TextView = itemView.findViewById(R.id.sender_msag)
        val senderTime: TextView = itemView.findViewById(R.id.sender_time)
    }

    class ReceiverViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiverMsg: TextView = itemView.findViewById(R.id.reciever_msag)
        val receiverTime: TextView = itemView.findViewById(R.id.reciever_time)
    }
}
