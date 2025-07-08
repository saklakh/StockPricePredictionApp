package com.shaikhaklakh.stockprice

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.json.JSONObject


class TrendingStockFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val stockSymbols = listOf("AAPL","GOOGL")
    //, "MSFT", , "META", "NVDA", "NFLX", "BABA", "INTC", "AMZN", "TSLA"
    private val trendingStocks = mutableListOf<TrendingStock>()
    private lateinit var adapter: TrendingStockAdapter
    private val client = OkHttpClient()
    private val apiKey = "CK01Y9PXEXN8VORG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_trending_stock, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.trendingRecyclerView)
        adapter = TrendingStockAdapter(trendingStocks)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchTrendingStocks()
    }


    private fun fetchTrendingStocks() {
        for (symbol in stockSymbols) {
            fetchStockData(symbol) { prices ->
                prices?.let {
                    val change = calculatePercentageChange(it)
                    requireActivity().runOnUiThread {
                        trendingStocks.add(TrendingStock(symbol, change))
                        if (trendingStocks.size == stockSymbols.size) {
                            trendingStocks.sortByDescending { it.percentageChange }
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private fun fetchStockData(symbol: String, callback: (List<Double>?) -> Unit) {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&apikey=$apiKey"

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException){
                Log.e("StockFetch", "Error fetching data for $symbol: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("StockFetch", "Response for $symbol: $responseBody")
                responseBody?.let { body ->
                    try {
                        val jsonObject = JSONObject(body)
                        val timeSeries = jsonObject.getJSONObject("Time Series (Daily)")
                        val dates = timeSeries.keys()
                        val closes = mutableListOf<Double>()

                        while (dates.hasNext()) {
                            val date = dates.next()
                            val close = timeSeries.getJSONObject(date).getDouble("4. close")
                            closes.add(close)
                            if (closes.size >= 7) break
                        }
                        callback(closes)
                    } catch (e: Exception) {
                        Log.e("StockFetch", "JSON parsing error: ${e.message}")
                        callback(null)
                    }
                } ?: callback(null)
            }
        })
    }

    private fun calculatePercentageChange(prices: List<Double>): Double {
        return if (prices.size >= 7) {
            val recent = prices[0]
            val past = prices[6]
            ((recent - past) / past) * 100
        } else 0.0
    }


}