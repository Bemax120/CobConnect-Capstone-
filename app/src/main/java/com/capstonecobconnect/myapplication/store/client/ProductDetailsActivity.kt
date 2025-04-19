package com.capstonecobconnect.myapplication.store.client

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var sizeGridLayout: GridLayout
    private lateinit var priceView: TextView
    private lateinit var stockTextView: TextView
    private lateinit var reviewRecyclerView: RecyclerView
    private lateinit var reviewAdapter: ReviewAdapter
    private lateinit var addToCartButton: Button
    private lateinit var buyNowButton: Button

    private val reviewList = mutableListOf<Review>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedButton: Button? = null

    private var shopName = ""
    private var cobblerLogo = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_details)

        val productName = intent.getStringExtra("name") ?: ""
        val productPrice = intent.getStringExtra("price") ?: ""
        val productImage = intent.getStringExtra("productImage") ?: ""
        val productDescription = intent.getStringExtra("description") ?: ""
        val productRating = intent.getFloatExtra("ratings", 0.0f)
        val documentId = intent.getStringExtra("documentId") ?: ""
        var shopID = intent.getStringExtra("shopID") ?: ""

        val imageView: ImageView = findViewById(R.id.productImage)
        val nameView: TextView = findViewById(R.id.productName)
        val ratingView: TextView = findViewById(R.id.productRating)
        val descriptionView: TextView = findViewById(R.id.productDescription)
        priceView = findViewById(R.id.productPrice)
        stockTextView = findViewById(R.id.stockText)
        sizeGridLayout = findViewById(R.id.sizeGridLayout)
        reviewRecyclerView = findViewById(R.id.reviewRecyclerView)
        addToCartButton = findViewById(R.id.addToCartButton)
        buyNowButton = findViewById(R.id.buyButton)
        val backButton: ImageView = findViewById(R.id.backButtonPDA)

        Glide.with(this).load(productImage).placeholder(R.drawable.img).into(imageView)

        nameView.text = productName
        priceView.text = getString(R.string.price_format, productPrice)
        ratingView.text = getString(R.string.rating_format, productRating)
        descriptionView.text = productDescription
        stockTextView.text = getString(R.string.stock_format, 0)

        reviewRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewAdapter = ReviewAdapter(reviewList)
        reviewRecyclerView.adapter = reviewAdapter

        if (documentId.isNotEmpty()) {
            fetchSizes(documentId)
            fetchReviews(documentId)
        }

        backButton.setOnClickListener { finish() }

        // 🔥 FIX: Ensure shopID is fetched from product if not passed
        if (shopID.isBlank() && documentId.isNotEmpty()) {
            db.collection("ShopItem").document(documentId).get()
                .addOnSuccessListener { productDoc ->
                    val fetchedShopID = productDoc.getString("shopID") ?: ""
                    if (fetchedShopID.isNotBlank()) {
                        shopID = fetchedShopID
                    }
                    fetchShopInfoAndSetupButtons(shopID, productName, productPrice, productImage, productRating, documentId)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get product details", Toast.LENGTH_SHORT).show()
                }
        } else {
            fetchShopInfoAndSetupButtons(shopID, productName, productPrice, productImage, productRating, documentId)
        }
    }

    private fun fetchShopInfoAndSetupButtons(
        shopID: String,
        productName: String,
        productPrice: String,
        productImage: String,
        productRating: Float,
        documentId: String
    ) {
        fetchShopInfo(shopID) { fetchedShopName, fetchedCobblerLogo ->
            shopName = fetchedShopName
            cobblerLogo = fetchedCobblerLogo

            addToCartButton.setOnClickListener {
                val selectedSize = selectedButton?.text?.toString()
                if (selectedSize == null) {
                    Toast.makeText(this, "Please select a size first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val cartItem = createCartItemMap(selectedSize, shopID, shopName, cobblerLogo)
                db.collection("Cart").add(cartItem)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK, Intent().putExtra("cart_updated", true))
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show()
                    }
            }

            buyNowButton.setOnClickListener {
                val selectedSize = selectedButton?.text?.toString()
                if (selectedSize == null) {
                    Toast.makeText(this, "Please select a size first", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val intent = Intent(this, ShoppingCartActivity::class.java).apply {
                    putExtra("name", productName)
                    putExtra("price", productPrice)
                    putExtra("productImage", productImage)
                    putExtra("size", selectedSize)
                    putExtra("quantity", 1)
                    putExtra("documentId", documentId)
                    putExtra("shopName", shopName)
                    putExtra("cobblerLogo", cobblerLogo)
                    putExtra("shopID", shopID)
                    putExtra("buyNow", true)
                }
                startActivity(intent)
            }
        }
    }

    private fun fetchShopInfo(shopID: String, onComplete: (String, String) -> Unit) {
        if (shopID.isBlank()) {
            onComplete("Unknown Shop", "")
            return
        }

        db.collection("Cobbler").document(shopID).get()
            .addOnSuccessListener { document ->
                val shopName = document.getString("shopName") ?: "Unknown Shop"
                val cobblerLogo = document.getString("cobblerLogo") ?: ""
                onComplete(shopName, cobblerLogo)
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch shop info", it)
                onComplete("Unknown Shop", "")
            }
    }

    private fun createCartItemMap(
        selectedSize: String,
        shopID: String,
        shopName: String,
        cobblerLogo: String
    ): HashMap<String, Any> {
        val userId = auth.currentUser?.uid ?: ""
        return hashMapOf(
            "productImage" to intent.getStringExtra("productImage").orEmpty(),
            "name" to intent.getStringExtra("name").orEmpty(),
            "price" to intent.getStringExtra("price").orEmpty(),
            "ratings" to intent.getFloatExtra("ratings", 0.0f),
            "size" to selectedSize,
            "shopID" to shopID,
            "userId" to userId,
            "shopName" to shopName,
            "cobblerLogo" to cobblerLogo,
            "quantity" to 1
        )
    }

    private fun fetchSizes(documentId: String) {
        db.collection("ShopItem").document(documentId).collection("size").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    stockTextView.text = getString(R.string.stock_format, 0)
                    return@addOnSuccessListener
                }
                for (doc in result) {
                    val size = doc.id
                    val stock = doc.getLong("stock")?.toInt() ?: 0
                    updateSizeButton(size, stock)
                }
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch sizes", it)
                Toast.makeText(this, getString(R.string.error_loading_sizes), Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateSizeButton(size: String, stock: Int) {
        val buttonId = resources.getIdentifier("size$size", "id", packageName)
        val button = findViewById<Button?>(buttonId)

        if (button != null) {
            button.isEnabled = stock > 0
            button.background = createRoundedBackground(stock)
            button.setOnClickListener {
                updateButtonSelection(button, stock)
            }
        } else {
            addDynamicSizeButton(size, stock)
        }
    }

    private fun addDynamicSizeButton(size: String, stock: Int) {
        val button = Button(this).apply {
            text = size
            isEnabled = stock > 0
            background = createRoundedBackground(stock)
            setOnClickListener {
                updateButtonSelection(this, stock)
            }
        }

        val params = GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.WRAP_CONTENT
            height = GridLayout.LayoutParams.WRAP_CONTENT
            setMargins(8, 8, 8, 8)
        }
        sizeGridLayout.addView(button, params)
    }

    private fun updateButtonSelection(button: Button, stock: Int) {
        selectedButton?.background = createRoundedBackground(stock)
        selectedButton = button
        button.background = createClickedBackground()
        stockTextView.text = getString(R.string.stock_format, stock)
    }

    private fun createRoundedBackground(stock: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20 * resources.displayMetrics.density
            setColor(
                ContextCompat.getColor(
                    this@ProductDetailsActivity,
                    if (stock > 0) android.R.color.holo_blue_light else android.R.color.darker_gray
                )
            )
        }
    }

    private fun createClickedBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20 * resources.displayMetrics.density
            setColor(ContextCompat.getColor(this@ProductDetailsActivity, android.R.color.holo_blue_dark))
        }
    }

    private fun fetchReviews(documentId: String) {
        db.collection("ShopItem").document(documentId).collection("reviews").get()
            .addOnSuccessListener { result ->
                reviewList.clear()
                for (doc in result) {
                    val comments = doc.getString("comments") ?: "No comment provided"
                    val date = doc.getTimestamp("date") ?: Timestamp.now()
                    val fname = doc.getString("fname") ?: "Anonymous"
                    val lname = doc.getString("lname") ?: ""
                    val profileImage = doc.getString("profileImage") ?: ""
                    val ratings = doc.getLong("ratings")?.toInt() ?: 0

                    reviewList.add(Review(comments, date, fname, lname, profileImage, ratings))
                }
                reviewAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Log.e("Firestore", "Failed to fetch reviews", it)
                Toast.makeText(this, getString(R.string.error_loading_reviews), Toast.LENGTH_SHORT).show()
            }
    }
}
