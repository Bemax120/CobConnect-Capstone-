package com.capstonecobconnect.myapplication.services

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.ArrayAdapter
import com.bumptech.glide.Glide
import com.capstonecobconnect.myapplication.R

class ImageAdapter(context: Context, private val imageList: List<ImageRow>) :
    ArrayAdapter<ImageRow>(context, 0, imageList), ListAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_image, parent, false)

        val row = imageList[position]

        val imageViews = listOf(
            view.findViewById<ImageView>(R.id.image11),
            view.findViewById<ImageView>(R.id.image2),
            view.findViewById<ImageView>(R.id.image3),
            view.findViewById<ImageView>(R.id.image4)
        )

        val images = listOf(row.image1, row.image2, row.image3, row.image4)

        for (i in images.indices) {
            images[i]?.let {
                Glide.with(context).load(it).into(imageViews[i])
            }
        }

        return view
    }
}