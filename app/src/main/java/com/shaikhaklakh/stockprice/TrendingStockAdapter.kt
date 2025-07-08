package com.shaikhaklakh.stockprice

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TrendingStockAdapter(
    private val stocks: List<TrendingStock>
) : RecyclerView.Adapter<TrendingStockAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbolText: TextView = view.findViewById(R.id.symbolText)
        val changeText: TextView = view.findViewById(R.id.changeText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending_stock, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock = stocks[position]
        holder.symbolText.text = stock.symbol
        holder.changeText.text = "%.2f%%".format(stock.percentageChange)
        holder.changeText.setTextColor(
            if (stock.percentageChange >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
        )
    }

    override fun getItemCount(): Int = stocks.size
}
