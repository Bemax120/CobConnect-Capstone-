package com.capstonecobconnect.myapplication.services

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class ServiceListAdapter(
    private var serviceList: List<ServiceItem> = emptyList(),
    private val totalPrice: Double // This will store the sum of prices passed from the parent
) : RecyclerView.Adapter<ServiceListAdapter.ServiceItemViewHolder>() {

    class ServiceItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.serviceTextView)
        val priceTextView: TextView = view.findViewById(R.id.priceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service2, parent, false)
        return ServiceItemViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ServiceItemViewHolder, position: Int) {
        val serviceItem = serviceList[position]

        // Bind service data to the views
        holder.nameTextView.text = serviceItem.service
        holder.priceTextView.text = "$${serviceItem.price}"
    }

    override fun getItemCount(): Int = serviceList.size

    // This method is called to show the total price
    fun getTotalPrice(): Double {
        return totalPrice
    }

    // Method to update the list with new data and notify the adapter
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newServiceList: List<ServiceItem>, newTotalPrice: Double) {
        serviceList = newServiceList
        // Update the total price
        notifyDataSetChanged()  // Notify the adapter that data has changed and it needs to refresh the view
    }
}

