package com.shaikhaklakh.stockprice

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.shaikhaklakh.stockprice.data.OHLC
import com.shaikhaklakh.stockprice.databinding.FragmentPricePredictBinding
import com.shaikhaklakh.stockprice.util.Utils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.collections.map


class PricePredictFragment : Fragment() {


    private lateinit var binding: FragmentPricePredictBinding
    private val min = 13.12
    private val max = 702.1
    private lateinit var tflite: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentPricePredictBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tflite = Interpreter(loadModelFile("stock_model.tflite"))

        val markerView = CustomMarkerView(requireContext(), R.layout.marker_view_layout)
        binding.lineChart.marker = markerView

        //changes made
        binding.symbolEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.lineChart.clear()
                binding.lineChart.invalidate()
                binding.candleChart.clear()
                binding.candleChart.invalidate()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.predictButton.setOnClickListener {
            val symbol = binding.symbolEditText.text.toString().uppercase()
            fetchStockData(symbol) { data ->
                requireActivity().runOnUiThread {
                    if (data != null && data.size == 60) {
                        val normalizedInput = data.map { (it - min) / (max - min) }
                        val futurePredictions = mutableListOf<Double>()
                        val tempData = normalizedInput.toMutableList()

                        for (i in 0 until 5) {
                            val input = Array(1) { Array(60) { FloatArray(1) } }
                            for (j in 0 until 60) {
                                input[0][j][0] = tempData[j].toFloat()
                            }

                            val output = Array(1) { FloatArray(1) }
                            tflite.run(input, output)

                            val predictedNorm = output[0][0]
                            val predictedPrice = predictedNorm * (max - min) + min
                            futurePredictions.add(predictedPrice)

                            tempData.add(predictedNorm.toDouble())  // add normalized predicted value
                            tempData.removeAt(0)         // maintain size 60
                        }

                        showChart(data, futurePredictions)

                    }
                    else{
                        Utils.showCustomToast(requireContext(),"Error fetching stock data")
                    }
                }
            }

            fetchStockOHLC(symbol) { ohlcList ->
                requireActivity().runOnUiThread {
                    if (ohlcList != null && ohlcList.isNotEmpty()) {
                        showCandlestickChart(ohlcList)
                    } else {
                        Utils.showCustomToast(requireContext(), "Candlestick data not available")
                    }
                }
            }


        }

    }


    private fun showChart(pastData: List<Double>, predicted: List<Double>) {
        binding.lineChart.clear()//changes made

        val actualEntries = mutableListOf<Entry>()
        val predictedEntries = mutableListOf<Entry>()

        // Plot past data (Actual)
        for (i in pastData.indices) {
            actualEntries.add(Entry(i.toFloat(), pastData[i].toFloat()))
        }

        for (i in predicted.indices) {
            predictedEntries.add(Entry((pastData.size + i).toFloat(), predicted[i].toFloat()))
        }

        // Calculate SMA with 10-day window
        val smaEntries = mutableListOf<Entry>()
        if (pastData.size >= 10) {
            for (i in 9 until pastData.size) {
                val window = pastData.subList(i - 9, i + 1)
                val average = window.average()
                smaEntries.add(Entry(i.toFloat(), average.toFloat()))
            }
        }




        // Create datasets
        val actualDataSet = LineDataSet(actualEntries, "Actual Price")
        actualDataSet.color = Color.BLUE
        actualDataSet.setCircleColor(Color.BLUE)
        actualDataSet.lineWidth = 2f

        val predictedDataSet = LineDataSet(predictedEntries, "Predicted Prices")
        predictedDataSet.color = Color.RED
        predictedDataSet.setCircleColor(Color.RED)
        predictedDataSet.lineWidth = 2f
        predictedDataSet.setDrawValues(true)

        val smaDataSet = LineDataSet(smaEntries, "10-day SMA")
        smaDataSet.color = Color.MAGENTA
        smaDataSet.setCircleColor(Color.MAGENTA)
        smaDataSet.lineWidth = 2f
        smaDataSet.setDrawValues(false)

        val lineData = LineData(actualDataSet, predictedDataSet, smaDataSet)
        binding.lineChart.data = lineData

        // Axis customization
        val xAxis = binding.lineChart.xAxis
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        xAxis.setDrawGridLines(false)

        val leftAxis = binding.lineChart.axisLeft
        leftAxis.setDrawGridLines(true)

        binding.lineChart.axisRight.isEnabled = false

        // Description and legend
        binding.lineChart.description.text = "Actual vs Predicted Stock Price"
        binding.lineChart.legend.isEnabled = true

        // Interaction
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.setPinchZoom(true)
        binding.lineChart.isDragEnabled = true
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.animateX(1000) //changes made Animate

        // Refresh
        binding.lineChart.invalidate()
    }


    private fun showCandlestickChart(ohlcList: List<OHLC>) {
        binding.candleChart.clear()
        val entries = ohlcList.mapIndexed { index, ohlc ->
            CandleEntry(
                index.toFloat(),
                ohlc.high.toFloat(),
                ohlc.low.toFloat(),
                ohlc.open.toFloat(),
                ohlc.close.toFloat()
            )
        }

        val dataSet = CandleDataSet(entries, "Candlestick")
        dataSet.color = Color.rgb(80, 80, 80)
        dataSet.shadowColor = Color.DKGRAY
        dataSet.decreasingColor = Color.RED
        dataSet.increasingColor = Color.GREEN
        dataSet.decreasingPaintStyle = Paint.Style.FILL
        dataSet.increasingPaintStyle = Paint.Style.FILL

        val candleData = CandleData(dataSet)

        binding.candleChart.data = candleData
        binding.candleChart.description.text = "Candlestick Chart"
        binding.candleChart.invalidate()
    }






    private fun loadModelFile(modelFileName: String): MappedByteBuffer {
        val fileDescriptor = requireContext().assets.openFd(modelFileName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }




    fun fetchStockData(symbol: String, callback: (List<Double>?) -> Unit) {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&apikey=CK01Y9PXEXN8VORG&datatype=csv"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val lines = body.lines().drop(1).take(60) // Skip header, take latest 60
                    val closes = lines.mapNotNull {
                        val cols = it.split(",")
                        if (cols.size > 4) cols[4].toDoubleOrNull() else null
                    }.reversed()
                    callback(closes)
                } ?: callback(null)
            }
        })
    }



    fun fetchStockOHLC(symbol: String, callback: (List<OHLC>?) -> Unit) {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&apikey=CK01Y9PXEXN8VORG&datatype=csv"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { body ->
                    val lines = body.lines().drop(1).take(60)
                    val ohlcData = lines.mapNotNull {
                        val cols = it.split(",")
                        if (cols.size > 5) {
                            val open = cols[1].toDoubleOrNull()
                            val high = cols[2].toDoubleOrNull()
                            val low = cols[3].toDoubleOrNull()
                            val close = cols[4].toDoubleOrNull()
                            if (open != null && high != null && low != null && close != null) {
                                OHLC(open, high, low, close)
                            } else null
                        } else null
                    }.reversed()
                    callback(ohlcData)
                } ?: callback(null)
            }
        })
    }





}