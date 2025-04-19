package com.capstonecobconnect.myapplication.cobblers.HomeService

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class ServiceItemAdapter(private val items: List<ServiceItem>) :
    RecyclerView.Adapter<ServiceItemAdapter.ServiceItemViewHolder>() {

    inner class ServiceItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val quantity: TextView = itemView.findViewById(R.id.quantity)
        val totalPrice: TextView = itemView.findViewById(R.id.totalPrice)
        val servicesList: RecyclerView = itemView.findViewById(R.id.servicesList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_horizontal, parent, false)
        return ServiceItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceItemViewHolder, position: Int) {
        val item = items[position]
        holder.quantity.text = "Qty: ${item.quantity}"
        holder.totalPrice.text = "₱${item.totalPrice}"

        holder.servicesList.layoutManager =
            LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
        holder.servicesList.adapter = ServiceListAdapter(item.services)
    }

    override fun getItemCount(): Int = items.size
}
