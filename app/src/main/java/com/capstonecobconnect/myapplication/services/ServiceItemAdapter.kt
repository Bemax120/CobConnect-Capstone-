package com.capstonecobconnect.myapplication.services

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import android.view.View
import android.widget.TextView

class ServiceItemAdapter(
    private var serviceList: List<ServiceItem> = emptyList(),
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<ServiceItemAdapter.ServiceItemViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return ServiceItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceItemViewHolder, position: Int) {
        val serviceItem = serviceList[position]
        val isSelected = selectedItems.contains(position)
        holder.bind(serviceItem, isSelected)

        holder.itemView.setOnClickListener {
            if (isSelected) selectedItems.remove(position) else selectedItems.add(position)
            notifyItemChanged(position)
            onSelectionChanged()
        }
    }

    override fun getItemCount(): Int = serviceList.size

    fun hasSelectedItems(): Boolean = selectedItems.isNotEmpty()

    fun getSelectedServices(): List<ServiceItem> = selectedItems.map { serviceList[it] }

    class ServiceItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceText: TextView = itemView.findViewById(R.id.serviceTextView)
        private val priceText: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(serviceItem: ServiceItem, isSelected: Boolean) {
            serviceText.text = serviceItem.service
            priceText.text = "Starting Price: ₱${serviceItem.price}"
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.bg_selected else R.drawable.border_only2
            )
        }
    }

    fun clearSelections() {
        selectedItems.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newServiceList: List<ServiceItem>, newTotalPrice: Double) {
        serviceList = newServiceList
        // Update the total price
        notifyDataSetChanged()  // Notify the adapter that data has changed and it needs to refresh the view
    }
}

