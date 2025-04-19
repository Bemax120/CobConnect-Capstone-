package com.capstonecobconnect.myapplication.store.client

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R

class ShopAdapter(
    private val itemList: List<ShopItem>
) : RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    class ShopViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemPrice: TextView = view.findViewById(R.id.itemPrice)
        val itemRating: TextView = view.findViewById(R.id.itemRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_layout, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val item = itemList[position]

        Glide.with(holder.itemImage.context)
            .load(item.productImage)
            .placeholder(R.drawable.shoe1)
            .error(R.drawable.img)
            .into(holder.itemImage)

        holder.itemName.text = item.name
        holder.itemPrice.text = item.price
        holder.itemRating.text = "⭐ ${item.ratings} (Avg. ratings)"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ProductDetailsActivity::class.java).apply {
                putExtra("name", item.name)
                putExtra("price", item.price)
                putExtra("productImage", item.productImage)
                putExtra("description", item.description)
                putExtra("ratings", item.ratings)
                putExtra("sizes", ArrayList(item.sizes))
                putExtra("documentId", item.documentId)
                putExtra("shopID", item.shopID)
                putExtra("shopName", item.shopName)
                putExtra("cobblerLogo", item.cobblerLogo)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = itemList.size
}
