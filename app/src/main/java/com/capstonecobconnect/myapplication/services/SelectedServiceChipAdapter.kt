package com.capstonecobconnect.myapplication.services

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class SelectedServiceChipAdapter(
    private val services: List<ServiceItem>
) : RecyclerView.Adapter<SelectedServiceChipAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val chipText: TextView = itemView.findViewById(R.id.serviceChip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.chipText.text = services[position].service

    }

    override fun getItemCount(): Int = services.size
}
