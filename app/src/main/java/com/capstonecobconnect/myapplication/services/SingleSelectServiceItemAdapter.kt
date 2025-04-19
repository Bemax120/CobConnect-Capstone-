package com.capstonecobconnect.myapplication.services

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class SingleSelectServiceItemAdapter(
    private val items: List<ServiceItem>,
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<SingleSelectServiceItemAdapter.ViewHolder>() {

    private var selectedItem: ServiceItem? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serviceText: TextView = itemView.findViewById(R.id.serviceTextView)
        private val priceText: TextView = itemView.findViewById(R.id.priceTextView)

        fun bind(item: ServiceItem) {
            serviceText.text = item.service
            priceText.text = item.price.toString()
            itemView.isSelected = item == selectedItem

            itemView.setBackgroundResource(
                if (item == selectedItem) R.drawable.bg_selected
                else R.drawable.border_only2
            )

            itemView.setOnClickListener {
                selectedItem = item
                notifyDataSetChanged()
                onSelectionChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun getSelectedService(): ServiceItem? = selectedItem

    fun hasSelectedItems(): Boolean = selectedItem != null

    fun clearSelections() {
        selectedItem = null
        notifyDataSetChanged()
    }
}
