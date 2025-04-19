package com.capstonecobconnect.myapplication.store.client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MarketPlaceActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShopAdapter
    private val itemList = mutableListOf<ShopItem>()
    private lateinit var cartBadge: TextView
    private lateinit var db: FirebaseFirestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_market_place)

        db = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.recyclerCleaning)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = ShopAdapter(itemList)
        recyclerView.adapter = adapter

        cartBadge = findViewById(R.id.cartBadge)
        val backButton: ImageView = findViewById(R.id.backButton)
        val cartIcon: ImageView = findViewById(R.id.cartIcon)
        val reviewIcon: ImageView = findViewById(R.id.reviewIcon)
        val couponsIcon: ImageView = findViewById(R.id.couponsIcon)

        backButton.setOnClickListener { finish() }

        cartIcon.setOnClickListener {
            val intent = Intent(this, ShoppingCartActivity::class.java)
            startActivity(intent)
        }

        reviewIcon.setOnClickListener {
            // TODO: Navigate to Reviews Section
        }

        couponsIcon.setOnClickListener {
            // TODO: Navigate to Coupons Section
        }

        updateCartBadgeCount()
        fetchDataFromFirestore()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data?.getBooleanExtra("cart_updated", false) == true) {
            updateCartBadgeCount()
        }
    }

    private fun fetchDataFromFirestore() {
        db.collection("ShopItem")
            .get()
            .addOnSuccessListener { result ->
                itemList.clear()
                for (document in result) {
                    val itemId = document.id
                    val shopId = document.getString("shopID") ?: ""
                    val name = document.getString("name") ?: "N/A"
                    val price = document.getString("price") ?: "N/A"
                    val productImage = document.getString("productImage") ?: ""
                    val ratings = document.getString("ratings")?.toFloat() ?: 0.0f
                    val description = document.getString("description") ?: "N/A"

                    db.collection("Shop").document(shopId).get()
                        .addOnSuccessListener { shopDoc ->
                            val shopName = shopDoc.getString("name") ?: "Unknown Shop"
                            val cobblerLogo = shopDoc.getString("logo") ?: ""

                            fetchSizes(
                                itemId,
                                productImage,
                                name,
                                price,
                                ratings,
                                description,
                                shopId,
                                shopName,
                                cobblerLogo
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreError", "Failed to fetch shop info", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to fetch items", e)
                Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchSizes(
        itemId: String,
        productImage: String,
        name: String,
        price: String,
        ratings: Float,
        description: String,
        shopId: String,
        shopName: String,
        cobblerLogo: String
    ) {
        db.collection("Size").document(itemId).collection(itemId).get()
            .addOnSuccessListener { sizeDocs ->
                val sizes = mutableListOf<Size>()
                for (sizeDoc in sizeDocs) {
                    val size = sizeDoc.id
                    val stock = sizeDoc.getLong("stock")?.toInt() ?: 0
                    sizes.add(Size(size, stock))
                }

                val item = ShopItem(
                    productImage = productImage,
                    name = name,
                    price = "₱$price",
                    ratings = ratings,
                    description = description,
                    sizes = sizes,
                    documentId = itemId,
                    shopID = shopId,
                    shopName = shopName,
                    cobblerLogo = cobblerLogo
                )

                itemList.add(item)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to fetch sizes", e)
            }
    }

    private fun updateCartBadgeCount() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("Cart")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CartBadge", "Error listening to cart updates", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val uniqueItems = mutableSetOf<String>()

                    for (doc in snapshot) {
                        val name = doc.getString("name") ?: continue
                        val size = doc.getString("size") ?: "Not selected"
                        uniqueItems.add("$name|$size")
                    }

                    val itemCount = uniqueItems.size
                    cartBadge.visibility = View.VISIBLE
                    cartBadge.text = itemCount.toString()
                    Log.d("CartBadge", "Cart count: $itemCount")
                } else {
                    cartBadge.visibility = View.GONE
                    Log.d("CartBadge", "Cart is empty")
                }
            }
    }
}
