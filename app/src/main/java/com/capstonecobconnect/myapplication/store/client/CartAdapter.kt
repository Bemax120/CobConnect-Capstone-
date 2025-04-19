package com.capstonecobconnect.myapplication.store.client

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore

class CartAdapter(
    val cartEntryList: MutableList<CartEntry>,
    private val onItemDeleted: (CartItem, Int) -> Unit,
    private val onCheckboxChanged: () -> Unit,
    private val onQuantityChanged: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (cartEntryList[position]) {
            is CartEntry.Header -> VIEW_TYPE_HEADER
            is CartEntry.Item -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_store_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_shopping_cart, parent, false)
                ItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun getItemCount(): Int = cartEntryList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val entry = cartEntryList[position]) {
            is CartEntry.Header -> (holder as HeaderViewHolder).bind(entry)
            is CartEntry.Item -> (holder as ItemViewHolder).bind(entry.cartItem)
        }
    }

    fun updateCartEntries(newEntries: List<CartEntry>) {
        val mergedEntries = mergeCartEntries(newEntries)
        cartEntryList.clear()
        cartEntryList.addAll(mergedEntries)
        notifyDataSetChanged()
    }

    private fun mergeCartEntries(entries: List<CartEntry>): List<CartEntry> {
        val result = mutableListOf<CartEntry>()
        val groupedByShop = mutableMapOf<String, Pair<CartEntry.Header, MutableList<CartItem>>>()

        var currentHeader: CartEntry.Header? = null

        for (entry in entries) {
            when (entry) {
                is CartEntry.Header -> {
                    currentHeader = entry
                    groupedByShop.getOrPut(entry.shopName) {
                        Pair(entry, mutableListOf())
                    }
                }

                is CartEntry.Item -> {
                    currentHeader?.let {
                        val list = groupedByShop[it.shopName]?.second ?: mutableListOf()
                        val existing = list.find { it.name == entry.cartItem.name && it.size == entry.cartItem.size }

                        if (existing != null) {
                            existing.quantity += entry.cartItem.quantity
                        } else {
                            list.add(entry.cartItem)
                        }
                    }
                }
            }
        }

        for ((_, pair) in groupedByShop) {
            val (header, items) = pair
            result.add(header)
            result.addAll(items.map { CartEntry.Item(it) })
        }

        return result
    }



    fun removeItem(position: Int) {
        val removedEntry = cartEntryList[position]
        cartEntryList.removeAt(position)

        // Check if header above should be removed
        if (removedEntry is CartEntry.Item) {
            val previousEntry = cartEntryList.getOrNull(position - 1)
            val nextEntry = cartEntryList.getOrNull(position)

            if (previousEntry is CartEntry.Header && (nextEntry == null || nextEntry is CartEntry.Header)) {
                cartEntryList.removeAt(position - 1)
            }
        }

        notifyDataSetChanged()
    }

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val storeNameText: TextView = itemView.findViewById(R.id.storeNameText)
        private val storeLogoImage: ImageView = itemView.findViewById(R.id.storeLogoImage)
        private val selectAllCheckbox: CheckBox = itemView.findViewById(R.id.allItemCheckbox)
        private val editItemHeader: TextView = itemView.findViewById(R.id.editItemHeader)

        fun bind(header: CartEntry.Header) {
            storeNameText.text = header.shopName

            // Fetch the shop logo from Firestore
            Glide.with(itemView.context)
                .load(header.shopLogo?.trim('"')) // Prevent issues with extra quotes from Firestore
                .placeholder(R.drawable.cob_img)
                .error(R.drawable.cob_img)  // Added error handler to fallback on error
                .into(storeLogoImage)

            // Calculate the start and end indices for the item list under this header
            val startIndex = bindingAdapterPosition + 1
            val endIndexExclusive = cartEntryList.indexOfFirstIndexed(startIndex) {
                it is CartEntry.Header
            }.takeIf { it != -1 } ?: cartEntryList.size

            val items = cartEntryList.subList(startIndex, endIndexExclusive)
                .filterIsInstance<CartEntry.Item>()

            // Checkbox for selecting all items in the shop
            selectAllCheckbox.setOnCheckedChangeListener(null)
            selectAllCheckbox.isChecked = items.all { it.cartItem.isChecked }

            selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
                items.forEach { it.cartItem.isChecked = isChecked }
                notifyItemRangeChanged(startIndex, endIndexExclusive - startIndex)
                onCheckboxChanged()
            }

            // Fetch additional shop info from Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("cobblers").document(header.shopId).get()
                .addOnSuccessListener { document ->
                    val shopName = document.getString("name")
                    val shopLogo = document.getString("logo")

                    // Update shop name and logo if they exist
                    if (!shopName.isNullOrBlank()) {
                        header.shopName = shopName
                    }
                    if (!shopLogo.isNullOrBlank()) {
                        header.shopLogo = shopLogo
                    }

                    // Notify the adapter to refresh this header
                    notifyItemChanged(bindingAdapterPosition)

                    // Avoid setting placeholder data unless it's missing permanently
                    if (!header.shopName.isNullOrBlank()) {
                        storeNameText.text = header.shopName
                    }
                    if (!header.shopLogo.isNullOrBlank()) {
                        Glide.with(itemView.context)
                            .load(header.shopLogo?.trim('"'))  // Prevent issues with extra quotes
                            .placeholder(R.drawable.cob_img)
                            .error(R.drawable.cob_img)  // Fallback on error
                            .into(storeLogoImage)
                    }

                    Log.d("FirestoreDebug", "Fetching shop info for ID: ${header.shopId}")
                }
                .addOnFailureListener {
                    // Fallback to default values if data fetch fails
                    storeNameText.text = "Unknown Shop"
                    Glide.with(itemView.context)
                        .load(R.drawable.cob_img)  // Default image in case of error
                        .into(storeLogoImage)
                }

            Log.d("BindDebug", "Binding header: ${header.shopName}, logo: ${header.shopLogo}")


             editItemHeader.setOnClickListener {
                val context = itemView.context
                val itemNames = items.map { it.cartItem.name }.toTypedArray()
                val options = arrayOf("Change Size", "Delete Item")

                AlertDialog.Builder(context)
                    .setTitle("Edit items from ${header.shopName}")
                    .setItems(options) { _, selectedOption ->
                        when (selectedOption) {
                            0 -> {
                                AlertDialog.Builder(context)
                                    .setTitle("Choose item to change size")
                                    .setItems(items.map { "${it.cartItem.name} (${it.cartItem.size})" }.toTypedArray()) { _, itemIndex ->
                                        val selectedItem = items[itemIndex].cartItem
                                        val sizes = arrayOf("35","36", "37", "38", "39", "40", "41", "42", "43")

                                        AlertDialog.Builder(context)
                                            .setTitle("Change size for ${selectedItem.name} (Size: ${selectedItem.size})")
                                            .setSingleChoiceItems(
                                                sizes.mapIndexed { _, size ->
                                                    val exists = items.any {
                                                        it.cartItem.name == selectedItem.name &&
                                                                it.cartItem.size == size &&
                                                                it.cartItem != selectedItem
                                                    }
                                                    if (exists) "$size (Already in cart)" else size
                                                }.toTypedArray(),
                                                sizes.indexOf(selectedItem.size)
                                            ) { dialogInterface, sizeIdx ->
                                                val newSize = sizes[sizeIdx]

                                                val alreadyExists = items.any {
                                                    it.cartItem.name == selectedItem.name &&
                                                            it.cartItem.size == newSize &&
                                                            it.cartItem != selectedItem
                                                }

                                                if (alreadyExists) {
                                                    Toast.makeText(context, "${selectedItem.name} (Size: $newSize) already exists. Just add the quantity", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    selectedItem.size = newSize
                                                    notifyItemRangeChanged(startIndex, endIndexExclusive - startIndex)
                                                    onQuantityChanged()
                                                    dialogInterface.dismiss()
                                                }
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }


                            1 -> {
                                AlertDialog.Builder(context)
                                    .setTitle("Choose item to delete")
                                    .setItems(items.map { "${it.cartItem.name} (${it.cartItem.size})" }.toTypedArray()) { _, itemIndex ->
                                        val selectedItem = items[itemIndex].cartItem

                                        AlertDialog.Builder(context)
                                            .setTitle("Delete ${selectedItem.name} (Size: ${selectedItem.size})?")
                                            .setMessage("Are you sure you want to remove this item?")
                                            .setPositiveButton("Delete") { _, _ ->
                                                val itemToDeleteIndex = cartEntryList.indexOfFirst {
                                                    it is CartEntry.Item && it.cartItem == selectedItem
                                                }
                                                if (itemToDeleteIndex != -1) {
                                                    onItemDeleted(selectedItem, itemToDeleteIndex)
                                                } else {
                                                    Toast.makeText(context, "Item not found.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .setNegativeButton("Cancel", null)
                                            .show()
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        private inline fun <T> List<T>.indexOfFirstIndexed(start: Int, predicate: (T) -> Boolean): Int {
            for (i in start until size) {
                if (predicate(this[i])) return i
            }
            return -1
        }
    }

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.productImage)
        private val name: TextView = itemView.findViewById(R.id.productName)
        private val price: TextView = itemView.findViewById(R.id.productPrice)
        private val size: TextView = itemView.findViewById(R.id.productSize)
        private val quantityText: TextView = itemView.findViewById(R.id.quantityText)
        private val plusBtn: Button = itemView.findViewById(R.id.plusBtn)
        private val minusBtn: Button = itemView.findViewById(R.id.minusBtn)
        private val checkbox: CheckBox = itemView.findViewById(R.id.itemCheckbox)


        fun bind(item: CartItem) {
            Glide.with(itemView.context).load(item.imageUrl).into(image)
            name.text = item.name
            price.text = "₱%.2f".format(item.price)
            size.text = "Size: ${item.size}"
            quantityText.text = item.quantity.toString()

            checkbox.setOnCheckedChangeListener(null)
            checkbox.isChecked = item.isChecked
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.isChecked = isChecked
                onCheckboxChanged()
            }

            plusBtn.setOnClickListener {
                item.quantity++
                quantityText.text = item.quantity.toString()
                onQuantityChanged()
            }

            minusBtn.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    quantityText.text = item.quantity.toString()
                    onQuantityChanged()
                }
            }



        }

    }


}
