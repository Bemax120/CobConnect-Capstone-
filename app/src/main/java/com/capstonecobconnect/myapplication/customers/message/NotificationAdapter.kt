package com.capstonecobconnect.myapplication.customers.message

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.capstonecobconnect.myapplication.R

class NotificationAdapter(
    private val context: Context,
    private val notifications: MutableList<NotificationItem>
) : ArrayAdapter<NotificationItem>(context, 0, notifications) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val notif = notifications[position]
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_notification, parent, false)

        val titleText = view.findViewById<TextView>(R.id.notification_title)
        val bodyText = view.findViewById<TextView>(R.id.notification_body)

        titleText.text = notif.title
        bodyText.text = notif.body

        return view
    }

    fun addNotification(notification: NotificationItem) {
        notifications.add(0, notification) // Add to top
        notifyDataSetChanged()
    }
}
