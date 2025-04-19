package com.capstonecobconnect.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListAdapter
import android.widget.TextView

class ShoeAdapter(private val context: Context, private val shoeList: List<ShoeItem>) :
    ArrayAdapter<ShoeItem>(context, 0, shoeList), ListAdapter {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_shoe, parent, false)

        val tvShoeType = view.findViewById<TextView>(R.id.tv_shoeTpye)
        val tvNumber = view.findViewById<TextView>(R.id.tv_number)
        val btnPlus = view.findViewById<Button>(R.id.btn_plus)
        val btnMinus = view.findViewById<Button>(R.id.btn_minus)

        val shoeItem = shoeList[position]

        tvShoeType.text = shoeItem.name
        tvNumber.text = shoeItem.quantity.toString()

        btnPlus.setOnClickListener {
            shoeList[position].quantity++
            notifyDataSetChanged()
        }

        btnMinus.setOnClickListener {
            if (shoeList[position].quantity > 0) {
                shoeList[position].quantity--
                notifyDataSetChanged()
            }
        }

        return view
    }
}
