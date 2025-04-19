package com.capstonecobconnect.myapplication.store.client

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShoppingCartActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var totalPriceText: TextView
    private lateinit var checkoutButton: Button
    private  lateinit var backCart: ImageView

    private val db = FirebaseFirestore.getInstance()
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var adapter: CartAdapter

    private val shopNameCache = mutableMapOf<String, String>()
    private val shopLogoCache = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_cart)

        recyclerView = findViewById(R.id.shoppingCartRecyclerView)
        totalPriceText = findViewById(R.id.totalPrice)
        checkoutButton = findViewById(R.id.checkoutButton)
        backCart  = findViewById(R.id.cartBck)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CartAdapter(
            mutableListOf(),
            onItemDeleted = { item, position ->
                deleteItemFromFirestore(item)
                adapter.removeItem(position)
                updateTotalPrice()
            },
            onCheckboxChanged = {
                updateTotalPrice()
            },
            onQuantityChanged = {
                updateTotalPrice()
            }
        )

        recyclerView.adapter = adapter
        setupSwipeToDelete()

        val isBuyNow = intent.getBooleanExtra("buyNow", false)
        if (isBuyNow) {
            displayBuyNowItem()
        } else {
            loadCartFromFirestore()
        }

        checkoutButton.setOnClickListener {
            startActivity(Intent(this, Information::class.java))
        }


        backCart.setOnClickListener { finish() }

    }

    private fun displayBuyNowItem() {
        val name = intent.getStringExtra("name") ?: return
        val price = intent.getStringExtra("price")?.toDoubleOrNull() ?: return
        val image = intent.getStringExtra("productImage") ?: ""
        val size = intent.getStringExtra("size") ?: ""
        val quantity = intent.getIntExtra("quantity", 1)
        val shopID = intent.getStringExtra("shopID") ?: ""

        db.collection("Shop")
            .whereEqualTo("cobblerId", shopID)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val shopDoc = querySnapshot.documents.firstOrNull()
                val storeName = shopDoc?.getString("name") ?: "Unknown Store"
                val cobblerLogo = shopDoc?.getString("logo") ?: ""

                val newItem = CartItem(
                    name = name,
                    imageUrl = image,
                    price = price,
                    size = size,
                    quantity = quantity,
                    storeName = storeName,
                    cobblerLogo = cobblerLogo,
                    shopID = shopID,
                    isChecked = true
                )

                cartItems.clear()
                cartItems.add(newItem)
                val groupedEntries = buildCartEntryList(cartItems)

                adapter.updateCartEntries(groupedEntries)
                updateTotalPrice() // updates checkout button too
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load shop info", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadCartFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("Cart")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val shopIdToItemsMap = mutableMapOf<String, MutableList<CartItem>>()

                for (doc in result) {
                    val name = doc.getString("name") ?: continue
                    val image = doc.getString("productImage") ?: ""
                    val size = doc.getString("size") ?: "Not selected"
                    val quantity = doc.getLong("quantity")?.toInt() ?: 1
                    val rawShopID = doc.getString("shopID") ?: ""
                    val shopID = rawShopID.trim('"')
                    val documentId = doc.id

                    val rawPrice = doc.get("price")
                    val price: Double = when (rawPrice) {
                        is Number -> rawPrice.toDouble()
                        is String -> rawPrice.replace("₱", "").trim().toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    val cartItem = CartItem(
                        name = name,
                        imageUrl = image,
                        price = price,
                        size = size,
                        quantity = quantity,
                        documentId = documentId,
                        shopID = shopID,
                        storeName = "",
                        isChecked = false
                    )

                    shopIdToItemsMap.getOrPut(shopID) { mutableListOf() }.add(cartItem)
                }

                fetchShopNamesAndDisplay(shopIdToItemsMap)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchShopNamesAndDisplay(shopIdToItemsMap: Map<String, List<CartItem>>) {
        val shopFetchTasks = mutableListOf<com.google.android.gms.tasks.Task<*>>()

        for (shopId in shopIdToItemsMap.keys) {
            if (!shopNameCache.containsKey(shopId)) {
                val task = db.collection("Shop")
                    .whereEqualTo("cobblerId", shopId)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val shopDoc = querySnapshot.documents[0]
                            val shopName = shopDoc.getString("name") ?: "Unknown Shop"
                            val shopLogo = shopDoc.getString("logo") ?: ""

                            shopNameCache[shopId] = shopName
                            shopLogoCache[shopId] = shopLogo
                        } else {
                            shopNameCache[shopId] = "Unknown Shop"
                        }
                    }
                    .addOnFailureListener {
                        shopNameCache[shopId] = "Unknown Shop"
                    }

                shopFetchTasks.add(task)
            }
        }

        if (shopFetchTasks.isEmpty()) {
            displayCart(shopIdToItemsMap)
        } else {
            Tasks.whenAllComplete(shopFetchTasks).addOnSuccessListener {
                displayCart(shopIdToItemsMap)
            }
        }
    }

    private fun displayCart(shopIdToItemsMap: Map<String, List<CartItem>>) {
        val allItems = mutableListOf<CartItem>()

        for ((shopId, items) in shopIdToItemsMap) {
            val shopName = shopNameCache[shopId] ?: "Unknown Shop"
            val shopLogo = shopLogoCache[shopId] ?: ""

            items.forEach {
                it.storeName = shopName
                it.cobblerLogo = shopLogo
            }

            allItems.addAll(items)
        }

        allItems.sortBy { it.storeName }

        cartItems.clear()
        cartItems.addAll(allItems)

        val groupedEntries = buildCartEntryList(cartItems)
        adapter.updateCartEntries(groupedEntries)
        updateTotalPrice()
    }

    private fun buildCartEntryList(cartItems: List<CartItem>): List<CartEntry> {
        val grouped = cartItems.groupBy { it.shopID }
        val cartEntries = mutableListOf<CartEntry>()

        grouped.forEach { (shopId, items) ->
            val shopName = shopNameCache[shopId] ?: "Unknown Store"
            val shopLogo = shopLogoCache[shopId] ?: ""

            cartEntries.add(CartEntry.Header(shopId ?: "unknown_shop", shopName ?: "Unknown Shop", shopLogo ?: ""))


            items.forEach { cartEntries.add(CartEntry.Item(it)) }
        }

        return cartEntries
    }

    private fun updateTotalPrice() {
        val selectedItems = cartItems.filter { it.isChecked }
        val total = selectedItems.sumOf { it.price * it.quantity }
        totalPriceText.text = "Total ₱%.2f".format(total)

        // Update checkout button with number of selected items
        val itemCount = selectedItems.sumOf { it.quantity }
        checkoutButton.text = "Checkout ($itemCount)"
    }


    private fun deleteItemFromFirestore(item: CartItem) {
        val docId = item.documentId ?: return
        db.collection("Cart").document(docId).delete()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val entry = adapter.cartEntryList.getOrNull(position)

                if (entry is CartEntry.Item) {
                    val item = entry.cartItem

                    AlertDialog.Builder(this@ShoppingCartActivity)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to remove ${item.name} from your cart?")
                        .setPositiveButton("Delete") { _, _ ->
                            deleteItemFromFirestore(item)
                            adapter.removeItem(position)
                            updateTotalPrice()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                            adapter.notifyItemChanged(position)
                        }
                        .setCancelable(false)
                        .show()
                } else {
                    adapter.notifyItemChanged(position)
                }
            }

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.bindingAdapterPosition
                val entry = adapter.cartEntryList.getOrNull(position)
                return if (entry is CartEntry.Item) super.getSwipeDirs(recyclerView, viewHolder) else 0
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }
}
