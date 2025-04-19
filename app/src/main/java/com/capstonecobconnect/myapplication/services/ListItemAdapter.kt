package com.capstonecobconnect.myapplication.services

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R

class ListItemAdapter(
    private var items: List<SelectedServiceInfo>,
    private val viewModel: ServiceRequestViewModel
) : RecyclerView.Adapter<ListItemAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val linearList: LinearLayout = itemView.findViewById(R.id.linearList)
        val rvServices: RecyclerView = itemView.findViewById(R.id.lvTextServices)
        val quantityText: TextView = itemView.findViewById(R.id.textQuantity)
        val btnAdd: Button = itemView.findViewById(R.id.btnAdd)
        val btnMinus: Button = itemView.findViewById(R.id.btnMinus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_service_info, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= items.size) return
        val item = items[position]

        // Setup horizontal RecyclerView for services
        holder.rvServices.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = SelectedServiceChipAdapter(item.services)
        }

        holder.quantityText.text = item.quantity.toString()

        holder.btnAdd.setOnClickListener {
            val updatedQuantity = item.quantity + 1
            viewModel.updateQuantity(position, updatedQuantity)
        }

        holder.btnMinus.setOnClickListener {
            if (item.quantity > 1) {
                val updatedQuantity = item.quantity - 1
                viewModel.updateQuantity(position, updatedQuantity)
            } else {
                // Quantity would become 0 – show the popup menu for deletion
                showDeletePopup(holder, position)
            }
        }


    }

    fun updateData(newItems: List<SelectedServiceInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    private fun showDeletePopup(holder: ViewHolder, position: Int) {
        val popup = PopupMenu(holder.itemView.context, holder.linearList)
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener { menuItem ->
            if (menuItem.title == "Delete") {
                viewModel.removeServiceInfo(position)
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, items.size)
                Toast.makeText(holder.itemView.context, "Item deleted", Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        }
        popup.show()
    }
}
