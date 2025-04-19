package com.capstonecobconnect.myapplication.wallet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstonecobconnect.myapplication.R
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactions: MutableList<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wallet_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTransactionType: TextView = view.findViewById(R.id.txtLabel)
        private val tvAmount: TextView = view.findViewById(R.id.txtAmount)
        private val tvTimestamp: TextView = view.findViewById(R.id.txtTime)

        fun bind(transaction: Transaction) {
            tvTransactionType.text = transaction.transactionType
            tvAmount.text = String.format("PHP %.2f", transaction.amount)
            tvTimestamp.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(transaction.timestamp))
        }
    }

    // ✅ Add this to support real-time updates
    fun updateTransactions(newList: List<Transaction>) {
        transactions.clear()
        transactions.addAll(newList)
        notifyDataSetChanged()
    }
}
