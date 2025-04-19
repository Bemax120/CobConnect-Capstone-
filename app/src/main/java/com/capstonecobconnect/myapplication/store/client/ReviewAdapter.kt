package com.capstonecobconnect.myapplication.store.client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private val reviews: List<Review>
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.profileImageView)
        val name: TextView = itemView.findViewById(R.id.nameTextView)
        val date: TextView = itemView.findViewById(R.id.dateTextView)
        val comments: TextView = itemView.findViewById(R.id.commentTextView)
        val ratings: TextView = itemView.findViewById(R.id.ratingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]

        Glide.with(holder.profileImage.context)
            .load(review.profileImage)
            .placeholder(R.drawable.cob_img) // Add your default profile image here
            .into(holder.profileImage)

        holder.name.text = "${review.fname} ${review.lname}"
        holder.date.text = formatDate(review.date)
        holder.comments.text = review.comments
        holder.ratings.text = "⭐ ${review.ratings}"
    }

    override fun getItemCount(): Int = reviews.size

    private fun formatDate(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }
}
