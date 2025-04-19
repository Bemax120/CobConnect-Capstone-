package com.capstonecobconnect.myapplication.wallet

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class Wallet_View : AppCompatActivity() {

    private var userId: String? = null
    private lateinit var walletBalanceTextView: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var transactionList: MutableList<Transaction> = mutableListOf()
    private lateinit var transactionAdapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_view)

        recyclerView = findViewById(R.id.rv_transaction_list)
        progressBar = findViewById(R.id.progressBar)
        walletBalanceTextView = findViewById(R.id.tv_wallet_balance)

        val backbtn = findViewById<ImageView>(R.id.bck_wallet)

        backbtn.setOnClickListener {
            finish()
        }


        val btnCashOut = findViewById<LinearLayout>(R.id.cashout)
        val btnCashIn = findViewById<LinearLayout>(R.id.cashin)

        userId = intent.getStringExtra("USER_ID")

        if (userId != null) {
            checkAndCreateWallet(userId!!)
            getWalletBalance(userId!!)
            getTransactions(userId!!)
        } else {
            walletBalanceTextView.text = "Error: No user ID"
        }

        btnCashIn.setOnClickListener {
            val dialog = PaymentMethodDialog()
            val args = Bundle()
            args.putString("USER_ID", userId)
            dialog.arguments = args
            dialog.show(supportFragmentManager, "PaymentMethodDialog")
        }
    }

    private fun checkAndCreateWallet(userId: String) {
        val walletRef = FirebaseFirestore.getInstance().collection("wallet").document(userId)
        walletRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    val walletData = hashMapOf(
                        "userId" to userId,
                        "balance" to 0.0
                    )
                    walletRef.set(walletData)
                        .addOnSuccessListener {
                            Log.d("Wallet", "Wallet created successfully")
                        }
                        .addOnFailureListener {
                            Log.e("Wallet", "Error creating wallet", it)
                        }
                }
            }
            .addOnFailureListener {
                Log.e("Wallet", "Error checking wallet existence", it)
            }
    }

    private fun getWalletBalance(userId: String) {
        val walletRef = FirebaseFirestore.getInstance().collection("wallet").document(userId)
        walletRef.addSnapshotListener { document, error ->
            if (error != null) {
                Log.e("Wallet", "Error fetching balance", error)
                walletBalanceTextView.text = "Error"
                return@addSnapshotListener
            }

            val balance = document?.getDouble("balance") ?: 0.0
            walletBalanceTextView.text = String.format("$%.2f", balance)
        }
    }

    fun getTransactions(userId: String) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.VISIBLE  // Ensure RecyclerView is always visible during loading

        val db = FirebaseFirestore.getInstance()

        db.collection("wallet").document(userId).collection("transactions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->

                progressBar.visibility = View.GONE  // Hide progress bar after fetching transactions

                if (error != null) {
                    Log.e("Wallet", "Error fetching transactions", error)
                    Toast.makeText(this, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val updatedList = snapshots?.documents?.mapNotNull { doc ->
                    val type = doc.getString("type") ?: return@mapNotNull null
                    val amount = doc.getDouble("amount") ?: return@mapNotNull null
                    val timestamp = when (val ts = doc.get("timestamp")) {
                        is Timestamp -> ts.toDate().time
                        is Long -> ts
                        else -> System.currentTimeMillis()
                    }
                    Transaction(
                        userId = userId,
                        type = type,
                        amount = amount,
                        timestamp = timestamp,
                        transactionType = type
                    )
                } ?: emptyList()

                Log.d("Transactions", "Updated transactions: ${updatedList.size}")  // Log updated list size

                // Check if list is empty before updating RecyclerView
                if (updatedList.isEmpty()) {
                    Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show()
                }

                transactionList.clear()
                transactionList.addAll(updatedList)

                if (!::transactionAdapter.isInitialized) {
                    transactionAdapter = TransactionAdapter(transactionList)
                    recyclerView.layoutManager = LinearLayoutManager(this)
                    recyclerView.adapter = transactionAdapter
                } else {
                    transactionAdapter.updateTransactions(transactionList)
                }

                // Show a toast message if there are no transactions
                if (transactionList.isEmpty()) {
                    Toast.makeText(this, "No transactions found", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
